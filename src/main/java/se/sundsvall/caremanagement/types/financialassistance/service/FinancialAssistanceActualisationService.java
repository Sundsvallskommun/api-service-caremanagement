package se.sundsvall.caremanagement.types.financialassistance.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationResult;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
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
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.APPLICATION_TYPE_NEW;

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
	/** Lifecare archive defaults — used when the archive request omits the matching field; all overridable per request. */
	/**
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

	FinancialAssistanceActualisationService(final ActualisationService actualisationService, final AttachmentService attachmentService,
		final DecisionService decisionService,
		final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository) {
		this.actualisationService = actualisationService;
		this.attachmentService = attachmentService;
		this.decisionService = decisionService;
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
	}

	/**
	 * Create the Lifecare actualisation (case intake) for the application month and return the created actualisation id.
	 * The intake date follows {@link #intakeDate(ActualisationRequest)}. When the request carries an {@code errandId},
	 * the creation is recorded on that errand as a {@code Decision(ACTUALISATION)} so the caseworker sees it in the
	 * case's audit trail.
	 */
	public ActualisationResponse createActualisation(final String municipalityId, final String namespace, final ActualisationRequest request) {
		final var applicant = request.getApplicant();
		final var intakeDate = intakeDate(request);
		final var result = actualisationService.createActualisation(municipalityId, applicant, intakeDate, isNewApplication(request));

		ofNullable(request.getErrandId()).filter(StringUtils::hasText)
			.ifPresent(errandId -> recordActualisation(municipalityId, namespace, errandId, result));

		return ActualisationResponse.create().withActualisationId(result.actualisationId());
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
	private LocalDate intakeDate(final ActualisationRequest request) {
		final var applicationMonthStart = YearMonth.parse(request.getApplicationMonth()).atDay(1);

		return ofNullable(request.getErrandId())
			.filter(StringUtils::hasText)
			.flatMap(financialAssistanceRepository::findByErrandId)
			.map(FinancialAssistanceEntity::getCreated)
			.map(OffsetDateTime::toLocalDate)
			.orElse(applicationMonthStart);
	}

	/**
	 * Whether the errand is a nyansökan, which Lifecare actualises with its own type. A standalone call without an
	 * {@code errandId} keeps the återansökan type it has always had.
	 */
	private boolean isNewApplication(final ActualisationRequest request) {
		return ofNullable(request.getErrandId())
			.filter(StringUtils::hasText)
			.flatMap(financialAssistanceRepository::findByErrandId)
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
	 * rather than needing a row of its own.
	 */
	private void recordActualisation(final String municipalityId, final String namespace, final String errandId, final ActualisationResult result) {
		final var archiveOutcome = archiveApplication(municipalityId, namespace, errandId, result.actualisationId());

		addActualisationDecision(municipalityId, namespace, errandId, result.actualisationId(),
			"Actualisation created in Lifecare (id %d). %s".formatted(result.actualisationId(), archiveOutcome));

		// The insats the actualisation was linked to is the key Lifecare's own case reads take; keeping it on the errand
		// saves every later errand open a Lifecare lookup. None for a nyansökan — that one is filled in on read.
		ofNullable(result.serviceId())
			.ifPresent(serviceId -> financialAssistanceRepository.findByErrandId(errandId)
				.ifPresent(entity -> financialAssistanceRepository.save(entity.withLifecareServiceId(serviceId))));

		ofNullable(result.assignedUserId()).filter(StringUtils::hasText)
			.ifPresent(assignedUserId -> errandService.updateErrand(municipalityId, namespace, errandId,
				PatchErrand.create().withAssignedUserId(assignedUserId)));
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
				LOG.info("No application documents to archive for errand {}", errandId);
				return NOTHING_TO_ARCHIVE_MESSAGE;
			}

			final var errandNumber = errandService.readErrand(municipalityId, namespace, errandId).getErrandNumber();
			final var archived = documents.stream()
				.map(document -> upload(municipalityId, actualisationId, errandNumber, document))
				.toList();

			LOG.info("Archived {} application document(s) of errand {} to Lifecare actualisation {}", archived.size(), errandNumber, actualisationId);
			return ARCHIVED_MESSAGE.formatted(String.join(", ", archived));
		} catch (final Exception e) {
			LOG.error("Failed to archive the application of errand {} to Lifecare actualisation {}: {}", errandId, actualisationId, e.getMessage(), e);
			return ARCHIVE_FAILED_MESSAGE.formatted(e.getMessage());
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
		final var fileName = (merged ? ATTACHMENTS_ARCHIVE_FILE_NAME : APPLICATION_ARCHIVE_FILE_NAME).formatted(errandNumber);
		final var title = (merged ? ATTACHMENTS_ARCHIVE_TITLE : APPLICATION_ARCHIVE_TITLE).formatted(errandNumber);

		actualisationService.uploadAttachment(municipalityId, actualisationId, fileName, document.content(),
			DEFAULT_ARCHIVE_DOCUMENT_TYPE, DEFAULT_ARCHIVE_DOCUMENT_SENDER_TYPE, title, DEFAULT_ARCHIVE_SENDER_NAME);

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

		actualisationService.uploadAttachment(municipalityId, actualisationId, fileName, readBytes(file), documentType, documentSenderType, title, senderName);

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
