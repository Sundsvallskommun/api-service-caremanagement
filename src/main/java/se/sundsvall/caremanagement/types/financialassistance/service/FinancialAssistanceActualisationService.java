package se.sundsvall.caremanagement.types.financialassistance.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationResult;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
import se.sundsvall.caremanagement.lifecare.service.AttachmentUpload;
import se.sundsvall.caremanagement.lifecare.service.model.ActualisationSummary;
import se.sundsvall.caremanagement.shared.SourceFile;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Actualisation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.APPLICATION_TYPE_NEW;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * The Lifecare actualisation (case intake) surface — create a new actualisation for the application month, list the
 * applicant's actualisations, and archive an uploaded document to a specific actualisation. Creating or archiving is
 * recorded on the errand (when one is supplied) as a {@code Decision(ACTUALISATION)} so the caseworker sees it in the
 * case's audit trail.
 */
@Service
@Transactional
public class FinancialAssistanceActualisationService {

	private static final String ACTUALISATION_TYPE = "ACTUALISATION";
	private static final String CREATED_BY = "drakel";

	/** How far back the actualisation listing reaches when the caller gives no explicit {@code from} date. */
	private static final int ACTUALISATION_LOOKBACK_MONTHS = 24;
	/**
	 * Lifecare archive defaults — used when the archive request omits the matching field; all overridable per request.
	 *
	 * <p>
	 * Lifecare's {@code InsertDocumentType} / {@code InsertDocumentSenderType} are <strong>catalogue ids</strong>, not
	 * code words: the actualisation proposal's {@code attachmentTypes} carries {@code {id: 1, name: "Inkommen
	 * handling"}} with {@code senderTypes} {@code {id: 1, name: "Den enskilde"}}. The previous values, "ANSOKAN" and
	 * "ENSKILD", were invented here and had never reached the live API — it answers
	 * {@code InsertDocumentType parameter not a number}. Verified against FamilyCare 2026-09-23: 1/1 uploads.
	 *
	 * <p>
	 * Left as constants rather than resolved from the proposal by name because the catalogue holds a single entry and
	 * a lookup would add a proposal fetch to every archive. If it grows, resolve by name the way the actualisation
	 * type, reason and fromWho already are.
	 */
	private static final String DEFAULT_ARCHIVE_DOCUMENT_TYPE = "1";
	private static final String DEFAULT_ARCHIVE_DOCUMENT_SENDER_TYPE = "1";
	private static final String DEFAULT_ARCHIVE_SENDER_NAME = "Draken";
	private static final String ACTUALISATION_NOT_FOUND_MESSAGE = "No Lifecare actualisation '%s' found for the given applicant";
	/** What the errand's Decision row says about the actualisation itself; the archive outcome is appended. */
	private static final String ACTUALISATION_CREATED_MESSAGE = "Actualisation created in Lifecare (id %d). %s";
	private static final String ACTUALISATION_ADOPTED_MESSAGE = "Actualisation found in Lifecare (id %d) - an earlier attempt had created it, so no second one was made. %s";
	/** Archive outcomes, written onto the errand's Decision row so the caseworker sees what happened. */
	private static final String ARCHIVED_MESSAGE = "Application archived to Lifecare as %s.";
	/** careM's own name for the merge of the citizen's uploads — the marker that tells the two documents apart. */
	private static final String COMBINED_PDF_FILE_NAME = "sammanstallning.pdf";
	private static final String ATTACHMENTS_ARCHIVE_FILE_NAME = "%s_bilagor.pdf";
	private static final String ATTACHMENTS_ARCHIVE_TITLE = "Bilagor till ansökan %s";
	private static final String NOTHING_TO_ARCHIVE_MESSAGE = "No application documents to archive.";
	private static final String ARCHIVE_FAILED_MESSAGE = "Archiving the application to Lifecare FAILED: %s";
	private static final String APPLICATION_ARCHIVE_FILE_NAME = "%s_ansokan.pdf";
	private static final String APPLICATION_ARCHIVE_TITLE = "Ansökan ekonomiskt bistånd %s";

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceActualisationService.class);

	private final ActualisationService actualisationService;
	private final AttachmentService attachmentService;
	private final DecisionService decisionService;
	private final ErrandService errandService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final TransactionTemplate transactionTemplate;

	FinancialAssistanceActualisationService(final ActualisationService actualisationService, final AttachmentService attachmentService,
		final DecisionService decisionService,
		final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository,
		final PlatformTransactionManager transactionManager) {
		this.actualisationService = actualisationService;
		this.attachmentService = attachmentService;
		this.decisionService = decisionService;
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	/**
	 * Create the Lifecare actualisation (case intake) for the application month and return the created actualisation id.
	 * The intake date follows {@link #intakeDate}. When the request carries an {@code errandId},
	 * the creation is recorded on that errand as a {@code Decision(ACTUALISATION)} so the caseworker sees it in the
	 * case's audit trail.
	 *
	 * <p>
	 * <strong>Repeatable for one errand.</strong> FamilyCare has no delete, so a retry must never create a second
	 * actualisation for the same application, and the external task does retry — on any failure, including a response
	 * that never arrived. With an {@code errandId}:
	 * <ol>
	 * <li>an actualisation already recorded on the errand is returned as it is — nothing is created;</li>
	 * <li>otherwise the intent is committed to the errand <em>before</em> Lifecare is called (see
	 * {@link FinancialAssistanceRepository#markActualisationRequestedIfAbsent}), so an attempt that dies between
	 * Lifecare's answer and our record leaves a trace;</li>
	 * <li>when that marker was already set, an earlier attempt may have created the actualisation without recording it, so
	 * it is looked for in Lifecare first and adopted if found — see
	 * {@link ActualisationService#createOrAdoptActualisation}.</li>
	 * </ol>
	 * A call without an {@code errandId}, or for an errand with no stored application, has nothing to hold the marker and
	 * creates every time.
	 *
	 * <p>
	 * <strong>No transaction around the whole step.</strong> The marker is committed on the errand row in a transaction
	 * of its own, and recording the actualisation writes the insats to the same row. A transaction that had already read
	 * anything before the marker was committed has a snapshot older than the row, and MariaDB with snapshot isolation
	 * refuses its write ({@code 1020 Record has changed since last read}) — after Lifecare has created the actualisation
	 * and taken the application, so the retry adopted it and uploaded the application a second time. The reads run in
	 * their own short transactions, Lifecare is called outside any, and the record is written in one transaction started
	 * after the marker (see {@link #recordActualisation}). It also keeps a database connection from being held for the
	 * seconds Lifecare takes.
	 */
	@Transactional(propagation = NOT_SUPPORTED)
	public ActualisationResponse createActualisation(final String municipalityId, final String namespace, final ActualisationRequest request) {
		return ofNullable(request.getErrandId()).filter(StringUtils::hasText)
			.map(errandId -> createForErrand(municipalityId, namespace, errandId, request))
			.orElseGet(() -> createStandalone(municipalityId, request));
	}

	/**
	 * Nothing to key a retry on and nothing to record it on: a manual call creates the actualisation, with the återansökan
	 * type.
	 */
	private ActualisationResponse createStandalone(final String municipalityId, final ActualisationRequest request) {
		final var result = actualisationService.createActualisation(municipalityId, request.getApplicant(), applicationMonthStart(request), false);

		return ActualisationResponse.create().withActualisationId(result.actualisationId());
	}

	private ActualisationResponse createForErrand(final String municipalityId, final String namespace, final String errandId, final ActualisationRequest request) {
		final var recorded = recordedActualisationId(municipalityId, namespace, errandId);
		if (recorded.isPresent()) {
			LOG.info("Errand {} already has Lifecare actualisation {} recorded - not creating another", sanitizeForLogging(errandId), recorded.get());
			return ActualisationResponse.create().withActualisationId(recorded.get());
		}

		// Null for an errand with no stored application: it has neither a submission date to read nor a row to hold the marker.
		final var application = financialAssistanceRepository.findByErrandId(errandId).orElse(null);
		final var applicant = request.getApplicant();
		final var intakeDate = intakeDate(request, application);
		final var newApplication = isNewApplication(application);

		final ActualisationResult result;
		if (ofNullable(application).map(entity -> requestedBefore(entity, errandId)).orElse(false)) {
			result = actualisationService.createOrAdoptActualisation(municipalityId, applicant, intakeDate, newApplication,
				actualisationId -> decisionService.existsOnAnotherErrand(ACTUALISATION_TYPE, String.valueOf(actualisationId), errandId));
		} else {
			result = actualisationService.createActualisation(municipalityId, applicant, intakeDate, newApplication);
		}

		recordActualisation(municipalityId, namespace, errandId, result);

		return ActualisationResponse.create().withActualisationId(result.actualisationId());
	}

	/**
	 * The Lifecare actualisation id recorded on the errand — the most recent {@code Decision(ACTUALISATION)} whose value
	 * is an id. Also finds one a caseworker set from the archive route.
	 */
	private Optional<Integer> recordedActualisationId(final String municipalityId, final String namespace, final String errandId) {
		return decisionService.readAll(municipalityId, namespace, errandId).stream()
			.filter(decision -> ACTUALISATION_TYPE.equals(decision.getDecisionType()))
			.map(Decision::getValue)
			.filter(StringUtils::hasText)
			.map(FinancialAssistanceActualisationService::parseId)
			.flatMap(Optional::stream)
			.findFirst();
	}

	private static Optional<Integer> parseId(final String value) {
		try {
			return Optional.of(Integer.valueOf(value.trim()));
		} catch (final NumberFormatException e) {
			return Optional.empty();
		}
	}

	/**
	 * Whether an earlier attempt already went to Lifecare for the errand — and, when none did, commit that this one is
	 * about to. The write is the repository's own transaction, so it is committed here and now, not with the record: a
	 * failure after Lifecare has created the actualisation rolls the record back and must not take the marker with it.
	 *
	 * <p>
	 * A marker that was set between the read and the write counts as an earlier attempt: the conditional update reports
	 * it, so two attempts cannot both take the first-attempt path.
	 */
	private boolean requestedBefore(final FinancialAssistanceEntity application, final String errandId) {
		if (application.getActualisationRequestedAt() != null) {
			return true;
		}
		return financialAssistanceRepository.markActualisationRequestedIfAbsent(errandId, OffsetDateTime.now(ZoneId.systemDefault())) == 0;
	}

	/**
	 * The actualisation's {@code Ansökningsdatum}: <strong>the day the application was submitted</strong>, read from
	 * the errand's {@code created} stamp.
	 * <p>
	 * Verksamhetens regelverk (revision 2026-09-22) says “Ansökningsdatum = datum för inskickandet”; the previous
	 * revision said “datum för senaste signering”. This code did neither — it used the first day of the application
	 * month, which is not a submission date at all and is up to a month early.
	 * <p>
	 * The errand's {@code created} is the only server-owned arrival stamp: it is set by {@code AuditableListener} when
	 * the Mina-sidor application is persisted, so it is the moment careM received it. {@code attestedAt} would read
	 * better but is supplied by the client and never written by careM, so it cannot be relied on.
	 * <p>
	 * Falls back to the first day of the application month when there is no errand to read — the actualisation may be
	 * created standalone, without an {@code errandId}. The EB process always passes one (businessKey = errandId), so
	 * the fallback only covers manual calls.
	 * <p>
	 * Note this date has a second effect: {@code ActualisationService} passes it to {@code CaseworkerResolver} as the
	 * reference date bounding the 36-month Lifecare Service lookback. Moving it from the 1st to the submission day
	 * shifts that window by at most a month, which does not change which caseworker is found in practice, but it is a
	 * behaviour change rather than a pure relabelling.
	 */
	private static LocalDate intakeDate(final ActualisationRequest request, final FinancialAssistanceEntity application) {
		return ofNullable(application)
			.map(FinancialAssistanceEntity::getCreated)
			.map(OffsetDateTime::toLocalDate)
			.orElseGet(() -> applicationMonthStart(request));
	}

	private static LocalDate applicationMonthStart(final ActualisationRequest request) {
		return YearMonth.parse(request.getApplicationMonth()).atDay(1);
	}

	/**
	 * Whether the errand is a nyansökan, which Lifecare actualises with its own type. A standalone call without an
	 * {@code errandId} keeps the återansökan type it has always had.
	 */
	private static boolean isNewApplication(final FinancialAssistanceEntity application) {
		return ofNullable(application)
			.map(FinancialAssistanceEntity::getApplicationType)
			.filter(APPLICATION_TYPE_NEW::equals)
			.isPresent();
	}

	/**
	 * Record the created actualisation on the errand as a {@code Decision(ACTUALISATION)} — the canonical audit-trail
	 * vehicle on the case — carrying the Lifecare actualisation id as the value, and assign the errand to the resolved
	 * caseworker when one was found (the same caseworker set on the Lifecare actualisation).
	 *
	 * <p>
	 * The application is archived before the row is written, so its outcome can be folded into the same description
	 * rather than needing a row of its own. The archive is a Lifecare call and runs outside any transaction; the three
	 * writes after it share one, begun only now — after the marker was committed — so its snapshot is not older than
	 * the errand row it updates.
	 */
	private void recordActualisation(final String municipalityId, final String namespace, final String errandId, final ActualisationResult result) {
		final var archiveOutcome = archiveApplication(municipalityId, namespace, errandId, result.actualisationId());

		transactionTemplate.executeWithoutResult(status -> {
			addActualisationDecision(municipalityId, namespace, errandId, result.actualisationId(),
				actualisationRecordedMessage(result).formatted(result.actualisationId(), archiveOutcome));

			// The insats the actualisation was linked to is the key Lifecare's own case reads take; keeping it on the errand
			// saves every later errand open a Lifecare lookup. None for a nyansökan — that one is filled in on read.
			ofNullable(result.serviceId())
				.ifPresent(serviceId -> financialAssistanceRepository.updateLifecareServiceId(errandId, serviceId));

			ofNullable(result.assignedUserId()).filter(StringUtils::hasText)
				.ifPresent(assignedUserId -> errandService.updateErrand(municipalityId, namespace, errandId,
					PatchErrand.create().withAssignedUserId(assignedUserId)));
		});
	}

	/**
	 * What the errand's audit trail says about where the actualisation came from — created now, or found from an earlier
	 * attempt.
	 */
	private static String actualisationRecordedMessage(final ActualisationResult result) {
		if (result.adopted()) {
			return ACTUALISATION_ADOPTED_MESSAGE;
		}
		return ACTUALISATION_CREATED_MESSAGE;
	}

	/**
	 * Upload the citizen's application documents onto the actualisation just created — the "arkivera ansökan" half of
	 * the process step, which carried the name without doing the work.
	 *
	 * <p>
	 * Best-effort, and the retry is the reason. This runs after the actualisation exists in Lifecare, so letting a
	 * failed upload propagate would make the external task retry the whole step and create a <strong>second</strong>
	 * actualisation for the same application. A failed archive must never cost the intake.
	 *
	 * <p>
	 * What it must not do either is fail quietly. The outcome — archived, nothing to archive, or failed — goes into
	 * the {@code Decision} row the step already writes, so a caseworker sees it on the errand instead of it living
	 * only in a log line nobody reads.
	 *
	 * @return a sentence describing the outcome, for the audit-trail decision
	 */
	private String archiveApplication(final String municipalityId, final String namespace, final String errandId, final Integer actualisationId) {
		try {
			final var documents = attachmentService.readApplicationArchiveDocuments(errandId);
			if (documents.isEmpty()) {
				LOG.info("No application documents to archive for errand {}", sanitizeForLogging(errandId));
				return NOTHING_TO_ARCHIVE_MESSAGE;
			}

			final var errandNumber = errandService.readErrand(municipalityId, namespace, errandId).getErrandNumber();
			final var archived = documents.stream()
				.map(document -> upload(municipalityId, actualisationId, errandNumber, document))
				.toList();

			LOG.info("Archived {} application document(s) of errand {} to Lifecare actualisation {}", archived.size(), errandNumber, actualisationId);
			return ARCHIVED_MESSAGE.formatted(String.join(", ", archived));
		} catch (final Exception e) {
			// The exception type only, in the log and in the decision text the API serves: Lifecare's and the blob store's
			// messages may carry the file name or the applicant's details.
			LOG.error("Failed to archive the application of errand {} to Lifecare actualisation {} ({})", sanitizeForLogging(errandId), actualisationId,
				e.getClass().getSimpleName());
			return ARCHIVE_FAILED_MESSAGE.formatted(e.getClass().getSimpleName());
		}
	}

	/**
	 * Upload one application document, under a name a caseworker can tell apart in Lifecare. careM's own names do not
	 * travel: the case-data snapshot is {@code {errandNumber}.pdf} and the merge is {@code sammanstallning.pdf}, which
	 * says nothing about which errand it belongs to once it sits among a person's other documents.
	 *
	 * @return the name the document was archived under
	 */
	private String upload(final String municipalityId, final Integer actualisationId, final String errandNumber, final SourceFile document) {
		final var merged = COMBINED_PDF_FILE_NAME.equals(document.fileName());
		final String fileNameTemplate;
		final String titleTemplate;
		if (merged) {
			fileNameTemplate = ATTACHMENTS_ARCHIVE_FILE_NAME;
			titleTemplate = ATTACHMENTS_ARCHIVE_TITLE;
		} else {
			fileNameTemplate = APPLICATION_ARCHIVE_FILE_NAME;
			titleTemplate = APPLICATION_ARCHIVE_TITLE;
		}
		final var fileName = fileNameTemplate.formatted(errandNumber);
		final var title = titleTemplate.formatted(errandNumber);

		actualisationService.uploadAttachment(municipalityId, actualisationId,
			new AttachmentUpload(DEFAULT_ARCHIVE_DOCUMENT_TYPE, DEFAULT_ARCHIVE_DOCUMENT_SENDER_TYPE, title, DEFAULT_ARCHIVE_SENDER_NAME, fileName, document.content()));

		return fileName;
	}

	/**
	 * Set the errand's Lifecare actualisation to the given id by recording the canonical {@code Decision(ACTUALISATION)}.
	 */
	private void addActualisationDecision(final String municipalityId, final String namespace, final String errandId, final Integer actualisationId, final String description) {
		decisionService.create(municipalityId, namespace, errandId, Decision.create()
			.withDecisionType(ACTUALISATION_TYPE)
			.withValue(String.valueOf(actualisationId))
			.withDescription(description)
			.withCreatedBy(CREATED_BY));
	}

	/**
	 * List the Lifecare actualisations (case intakes) registered on the applicant, so a caseworker can pick which one a
	 * supplementary application is archived to. The applicant is identified by partyId. The period defaults to the last
	 * {@value #ACTUALISATION_LOOKBACK_MONTHS} months up to today when {@code from}/{@code to} are omitted.
	 */
	@Transactional(readOnly = true)
	public List<Actualisation> listActualisations(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
		return actualisationsFor(municipalityId, partyId, from, to);
	}

	/**
	 * The applicant's Lifecare actualisations — a non-proxied worker so an in-class scope check
	 * ({@link #archiveToActualisation}) can reuse it without a self-call to the {@code @Transactional} public method
	 * (which would bypass the Spring proxy — Sonar S6809).
	 */
	private List<Actualisation> actualisationsFor(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
		final var toDate = ofNullable(to).orElseGet(LocalDate::now);
		final var fromDate = ofNullable(from).orElseGet(() -> toDate.minusMonths(ACTUALISATION_LOOKBACK_MONTHS));

		return actualisationService.listActualisations(municipalityId, partyId, fromDate, toDate).stream()
			.map(FinancialAssistanceActualisationService::toActualisation)
			.toList();
	}

	/**
	 * Archive an uploaded document (e.g. a supplementary application) to a specific Lifecare
	 * actualisation by binding it as an attachment. caremanagement only forwards the bytes — the file is supplied by the
	 * frontend. Document type / sender type / sender name fall back to server defaults when the request omits them; the
	 * title defaults to the uploaded file name. When the request carries an {@code errandId}, the target actualisation id
	 * is recorded on that errand as a {@code Decision(ACTUALISATION)} — setting the errand's Lifecare actualisation to the
	 * one archived to.
	 *
	 * <p>
	 * Gated on ownership: the actualisation id must belong to the given applicant (the same partyId scope as
	 * {@link #listActualisations}), so a caller cannot bind a file to another applicant's actualisation by guessing its
	 * (sequential) Lifecare-global id — a foreign id yields 404 before anything is uploaded.
	 */
	public void archiveToActualisation(final String municipalityId, final String namespace, final String partyId, final Integer actualisationId, final MultipartFile file, final ArchiveActualisationRequest request) {
		final var owned = actualisationsFor(municipalityId, partyId, null, null).stream()
			.anyMatch(actualisation -> actualisationId.equals(actualisation.getId()));
		if (!owned) {
			throw Problem.valueOf(NOT_FOUND, ACTUALISATION_NOT_FOUND_MESSAGE.formatted(actualisationId));
		}

		final var meta = ofNullable(request).orElseGet(ArchiveActualisationRequest::create);
		final var fileName = ofNullable(file.getOriginalFilename()).filter(StringUtils::hasText).orElse("dokument.pdf");
		final var title = ofNullable(meta.getTitle()).filter(StringUtils::hasText).orElse(fileName);
		final var documentType = ofNullable(meta.getDocumentType()).filter(StringUtils::hasText).orElse(DEFAULT_ARCHIVE_DOCUMENT_TYPE);
		final var documentSenderType = ofNullable(meta.getDocumentSenderType()).filter(StringUtils::hasText).orElse(DEFAULT_ARCHIVE_DOCUMENT_SENDER_TYPE);
		final var senderName = ofNullable(meta.getSenderName()).filter(StringUtils::hasText).orElse(DEFAULT_ARCHIVE_SENDER_NAME);

		actualisationService.uploadAttachment(municipalityId, actualisationId,
			new AttachmentUpload(documentType, documentSenderType, title, senderName, fileName, readBytes(file)));

		ofNullable(meta.getErrandId()).filter(StringUtils::hasText)
			.ifPresent(errandId -> addActualisationDecision(municipalityId, namespace, errandId, actualisationId,
				"Actualisation set on errand from archive (id %d).".formatted(actualisationId)));
	}

	/** Read the uploaded file's bytes, surfacing an unreadable upload as a 400 rather than an opaque 500. */
	private static byte[] readBytes(final MultipartFile file) {
		try {
			return file.getBytes();
		} catch (final IOException e) {
			throw Problem.valueOf(BAD_REQUEST, "Could not read the uploaded file: " + e.getMessage());
		}
	}

	/** Project the lifecare-module summary onto the API model. */
	private static Actualisation toActualisation(final ActualisationSummary summary) {
		return Actualisation.create()
			.withId(summary.id())
			.withType(summary.type())
			.withName(summary.name())
			.withDate(summary.date())
			.withReason(summary.reason())
			.withRegards(summary.regards())
			.withFromWho(summary.fromWho())
			.withCaseworker(summary.caseworker())
			.withOrganization(summary.organization())
			.withStatus(summary.status())
			.withInvestigationId(summary.investigationId())
			.withServiceId(summary.serviceId())
			.withDecisionId(summary.decisionId());
	}
}
