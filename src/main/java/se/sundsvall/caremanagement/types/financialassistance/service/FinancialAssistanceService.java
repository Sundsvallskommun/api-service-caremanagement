package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.YearMonth;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.NormberakningService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningResponse;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_INKOMMEN;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.applicationTypeForSlug;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinancialAssistanceMapper.toEntity;
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

	/** Recommendation Decision recorded by the automated normberäkning pipeline. */
	private static final String RECOMMENDATION_TYPE = "RECOMMENDATION";
	private static final String RECOMMENDATION_CREATED_BY = "drakel";
	private static final String VALUE_REVIEW_REQUIRED = "REVIEW_REQUIRED";
	private static final String VALUE_OK = "OK";

	private final ErrandService errandService;
	private final FinancialAssistanceRepository repository;
	private final NormberakningService normberakningService;
	private final CitizenService citizenService;
	private final DecisionService decisionService;

	FinancialAssistanceService(final ErrandService errandService, final FinancialAssistanceRepository repository, final NormberakningService normberakningService,
		final CitizenService citizenService, final DecisionService decisionService) {
		this.errandService = errandService;
		this.repository = repository;
		this.normberakningService = normberakningService;
		this.citizenService = citizenService;
		this.decisionService = decisionService;
	}

	public String create(final String municipalityId, final String namespace, final String typeSlug, final CreateFinancialAssistanceRequest request) {
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
		final var coApplicant = ofNullable(request.getCoApplicant()).filter(StringUtils::hasText)
			.map(partyId -> personalNumber(municipalityId, partyId))
			.orElse(null);
		final var result = normberakningService.buildAndPost(municipalityId, applicant, coApplicant, YearMonth.parse(request.getApplicationMonth()));
		final var response = toResponse(result);

		ofNullable(request.getErrandId()).filter(StringUtils::hasText)
			.ifPresent(errandId -> recordRecommendation(municipalityId, namespace, errandId, response));

		return response;
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
			.withCreatedBy(RECOMMENDATION_CREATED_BY));
	}

	/** Resolve a partyId to the personnummer the Lifecare/SSBTEK pipeline needs, or 404 when the citizen is unknown. */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}
}
