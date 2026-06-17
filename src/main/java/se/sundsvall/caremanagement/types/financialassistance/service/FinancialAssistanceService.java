package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
import se.sundsvall.caremanagement.lifecare.service.NormberakningService;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatus;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatusService;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_INKOMMEN;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.applicationTypeForSlug;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toEntity;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toStakeholders;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toView;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.NormberakningMapper.toResponse;

/**
 * Creates and reads financial-assistance (EB) errands. The envelope is owned by the exposed core {@link ErrandService};
 * the {@code typeSlug} is one of the three EB slugs (new / renewal / supplementary) and the stored
 * {@code applicationType}
 * is derived from it server-side, so the slug stays authoritative. Initial status is {@code INKOMMEN}. The
 * strongly-typed
 * application data lives on this module's own table, keyed by the envelope id. The title falls back to the EB display
 * name when the client omits one.
 */
@Service
@Transactional
public class FinancialAssistanceService {

	private static final String DEFAULT_TITLE = "Ekonomiskt bistånd";

	/** Decisions recorded by the automated pipelines, written as the drakel system actor. */
	private static final String RECOMMENDATION_TYPE = "RECOMMENDATION";
	private static final String ACTUALISATION_TYPE = "ACTUALISATION";
	private static final String CREATED_BY = "drakel";
	private static final String VALUE_REVIEW_REQUIRED = "REVIEW_REQUIRED";
	private static final String VALUE_OK = "OK";

	private final ErrandService errandService;
	private final FinancialAssistanceRepository repository;
	private final NormberakningService normberakningService;
	private final ActualisationService actualisationService;
	private final PaymentStatusService paymentStatusService;
	private final CitizenService citizenService;
	private final DecisionService decisionService;
	private final AttachmentService attachmentService;
	private final StakeholderService stakeholderService;

	FinancialAssistanceService(final ErrandService errandService, final FinancialAssistanceRepository repository, final NormberakningService normberakningService,
		final ActualisationService actualisationService, final PaymentStatusService paymentStatusService, final CitizenService citizenService, final DecisionService decisionService,
		final AttachmentService attachmentService, final StakeholderService stakeholderService) {
		this.errandService = errandService;
		this.repository = repository;
		this.normberakningService = normberakningService;
		this.actualisationService = actualisationService;
		this.paymentStatusService = paymentStatusService;
		this.citizenService = citizenService;
		this.decisionService = decisionService;
		this.attachmentService = attachmentService;
		this.stakeholderService = stakeholderService;
	}

	/**
	 * Create the EB errand, persist its strongly-typed data, promote the application's persons (sökande/medsökande) to
	 * core stakeholder rows on the errand, and — when the citizen supplied supporting files — store each attachment plus a
	 * single combined PDF merging them all. Attachments are type-agnostic: the list is handed to the attachments module
	 * as-is.
	 */
	public String create(final String municipalityId, final String namespace, final String typeSlug, final CreateFinancialAssistanceRequest request,
		final List<MultipartFile> attachments) {
		final var envelope = Errand.create()
			.withTypeSlug(typeSlug)
			.withTitle(ofNullable(request.getTitle()).orElse(DEFAULT_TITLE))
			.withStatus(STATUS_INKOMMEN)
			.withDescription(request.getDescription())
			.withPriority(request.getPriority())
			.withReporterUserId(request.getReporterUserId())
			.withAssignedUserId(request.getAssignedUserId());

		final var errandId = errandService.createErrand(municipalityId, namespace, envelope);

		final var entity = ofNullable(toEntity(request.getData(), errandId))
			.orElseGet(() -> FinancialAssistanceEntity.create().withErrandId(errandId));
		entity.setApplicationType(applicationTypeForSlug(typeSlug)); // slug is authoritative — overrides any client-sent value
		repository.save(entity);

		// Promote the application's persons to stakeholders so the errand carries its sökande/medsökande in the shared
		// collection.
		toStakeholders(ofNullable(request.getData()).map(FinancialAssistanceData::getPersons).orElse(null))
			.forEach(stakeholder -> stakeholderService.create(municipalityId, namespace, errandId, stakeholder));

		ofNullable(attachments)
			.filter(files -> !files.isEmpty())
			.ifPresent(files -> attachmentService.storeAndCombine(municipalityId, namespace, errandId, files));

		return errandId;
	}

	@Transactional(readOnly = true)
	public FinancialAssistanceView read(final String municipalityId, final String namespace, final String errandId) {
		final var envelope = errandService.readErrand(municipalityId, namespace, errandId);
		final var entity = repository.findByErrandId(errandId).orElse(null);
		return toView(envelope, entity);
	}

	public void updateData(final String municipalityId, final String namespace, final String errandId, final FinancialAssistanceData data) {
		// Scope check — throws 404 when the errand is missing in this namespace/municipality.
		errandService.readErrand(municipalityId, namespace, errandId);
		repository.save(toEntity(data, errandId));
	}

	/**
	 * Build the SSBTEK-driven normberäkning for the application month and post it to Lifecare FC, returning the created
	 * calculation id plus the income warnings the handläggare must review. When the request carries an {@code errandId},
	 * the warnings are also recorded on that errand as a {@code Decision(RECOMMENDATION)} so the handläggare sees them on
	 * the case.
	 */
	public NormberakningResponse createNormberakning(final String municipalityId, final String namespace, final NormberakningRequest request) {
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var applicationMonth = YearMonth.parse(request.getApplicationMonth());

		final var response = ofNullable(request.getClassifiedIncomes()).filter(StringUtils::hasText)
			.map(classifiedIncomes -> fromClassified(applicant, applicationMonth, classifiedIncomes, request))
			.orElseGet(() -> fromSsbtek(municipalityId, applicant, applicationMonth, request));

		ofNullable(request.getErrandId()).filter(StringUtils::hasText)
			.ifPresent(errandId -> recordRecommendation(municipalityId, namespace, errandId, response));

		return response;
	}

	/** Slim path: incomes already classified by the operaton regelverk — caremanagement only assembles + posts. */
	private NormberakningResponse fromClassified(final String applicant, final YearMonth applicationMonth, final String classifiedIncomes, final NormberakningRequest request) {
		final var calculationId = normberakningService.buildAndPostFromClassified(applicant, applicationMonth, classifiedIncomes);
		return NormberakningResponse.create()
			.withCalculationId(calculationId)
			.withUnhandledIncomes(ofNullable(request.getUnhandledIncomes()).orElseGet(List::of))
			.withChangeWarnings(ofNullable(request.getChangeWarnings()).orElseGet(List::of));
	}

	/** Legacy path (to be removed): caremanagement fetches SSBTEK and evaluates the rålista itself. */
	private NormberakningResponse fromSsbtek(final String municipalityId, final String applicant, final YearMonth applicationMonth, final NormberakningRequest request) {
		final var coApplicant = ofNullable(request.getCoApplicant()).filter(StringUtils::hasText)
			.map(partyId -> personalNumber(municipalityId, partyId))
			.orElse(null);
		return toResponse(normberakningService.buildAndPost(municipalityId, applicant, coApplicant, applicationMonth));
	}

	/**
	 * Surface the normberäkning's income warnings on the errand as a {@code Decision(RECOMMENDATION)} — the canonical flag
	 * vehicle handläggaren reviews in the case. The value is {@code REVIEW_REQUIRED} when there is anything to review
	 * (unhandled incomes or significant period-over-period changes) and {@code OK} otherwise; the description lists the
	 * warnings in plain language.
	 */
	private void recordRecommendation(final String municipalityId, final String namespace, final String errandId, final NormberakningResponse response) {
		final var warnings = Stream.concat(
			response.getUnhandledIncomes().stream().map("Ej överförd inkomst: "::concat),
			response.getChangeWarnings().stream().map("Förändrad inkomst: "::concat))
			.toList();
		final var header = "Normberäkning skapad i Lifecare (id %s). ".formatted(response.getCalculationId());
		final var description = warnings.isEmpty()
			? header + "Inga varningar – inkomsterna överfördes utan anmärkning."
			: header + warnings.size() + " varning(ar) att granska:\n" + String.join("\n", warnings);

		decisionService.create(municipalityId, namespace, errandId, Decision.create()
			.withDecisionType(RECOMMENDATION_TYPE)
			.withValue(warnings.isEmpty() ? VALUE_OK : VALUE_REVIEW_REQUIRED)
			.withDescription(description)
			.withCreatedBy(CREATED_BY));
	}

	/**
	 * Create the Lifecare aktualisering (case intake) for the application month and return the created aktualisering id.
	 * The intake date is the first day of the application month. When the request carries an {@code errandId}, the
	 * creation is recorded on that errand as a {@code Decision(ACTUALISATION)} so the handläggare sees it in the case's
	 * audit trail.
	 */
	public ActualisationResponse createActualisation(final String municipalityId, final String namespace, final ActualisationRequest request) {
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var intakeDate = YearMonth.parse(request.getApplicationMonth()).atDay(1);
		final var actualisationId = actualisationService.create(applicant, intakeDate);

		ofNullable(request.getErrandId()).filter(StringUtils::hasText)
			.ifPresent(errandId -> recordActualisation(municipalityId, namespace, errandId, actualisationId));

		return ActualisationResponse.create().withActualisationId(actualisationId);
	}

	/**
	 * Read whether the manual Lifecare utbetalning for the application month has been effectuated for the applicant.
	 * caremanagement makes no payment — the handläggare does it in Lifecare; the process polls this to detect when the
	 * payment is registered. Returns the effectuated flag and, when effectuated, the Lifecare PayDate.
	 */
	public PaymentStatusResponse checkPaymentStatus(final String municipalityId, final PaymentStatusRequest request) {
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final PaymentStatus status = paymentStatusService.read(applicant, YearMonth.parse(request.getApplicationMonth()));
		return PaymentStatusResponse.create()
			.withEffectuated(status.effectuated())
			.withPaymentDate(status.paymentDate());
	}

	/**
	 * Record the created aktualisering on the errand as a {@code Decision(ACTUALISATION)} — the canonical audit-trail
	 * vehicle on the case — carrying the Lifecare aktualisering id as the value.
	 */
	private void recordActualisation(final String municipalityId, final String namespace, final String errandId, final Integer actualisationId) {
		decisionService.create(municipalityId, namespace, errandId, Decision.create()
			.withDecisionType(ACTUALISATION_TYPE)
			.withValue(String.valueOf(actualisationId))
			.withDescription("Aktualisering skapad i Lifecare (id %d).".formatted(actualisationId))
			.withCreatedBy(CREATED_BY));
	}

	/** Resolve a partyId to the personnummer the Lifecare/SSBTEK pipeline needs, or 404 when the citizen is unknown. */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}
}
