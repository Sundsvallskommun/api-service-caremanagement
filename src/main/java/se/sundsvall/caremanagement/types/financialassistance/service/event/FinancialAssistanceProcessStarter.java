package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Starts a financial assistance process on an errand and seeds the household start variables the BPMN/DMN flow reads.
 * The errand's type slug picks the process: a renewal starts the full decision-support process
 * ({@code rakel-ekonomiskt-bistand}), a supplementary application the lighter supplementary-application process
 * ({@code rakel-ekonomiskt-bistand-tillaggsansokan}) and a new application the new-application process
 * ({@code rakel-ekonomiskt-bistand-nyansokan}) — the latter two reuse the same first steps (status + actualisation)
 * without SSBTEK/calculation, the new application additionally building a calculation straight from what the citizen
 * declared. In
 * each case the actualisation step resolves the caseworker off the applicant's most recent Lifecare intervention (for a
 * supplementary application: the ongoing renewal application's caseworker), so the errand ends up on that caseworker
 * rather than the
 * default assignee (a new application has no prior intervention and keeps the default assignee).
 *
 * <p>
 * Both callers — the intake ({@link FinancialAssistanceErrandCreatedListener}) and the manual-review release
 * ({@link FinancialAssistanceReleaseListener}) — go through the single {@link #startFor} dispatch, so a frozen errand
 * resumes in the flow of the type the citizen actually applied for. Choosing that type is the eligibility check's job
 * (a changed household constellation is a new application, never a renewal); the freeze only defers the start, it must
 * not rewrite the type.
 *
 * <p>
 * The process is started with {@code businessKey = errandId} and seeded with the municipalityId and namespace, the
 * applicant and optional co-applicant partyIds (from the application's persons), their personnummer (resolved via
 * {@link CitizenService}, keyed on by the SSBTEK fetch), the application month derived from the period, and the SSBTEK
 * fetch window (fromDate/toDate) derived from that month. Best-effort: a missing process definition or an unavailable
 * engine is logged and swallowed so it never disturbs the caller — the flow can be (re)started later.
 */
@Component
class FinancialAssistanceProcessStarter {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceProcessStarter.class);

	/**
	 * The financial assistance decision-support BPMN process (renewal), deployed to Operaton via the modeler. businessKey =
	 * errandId.
	 */
	static final String PROCESS_DEFINITION_NAME = "rakel-ekonomiskt-bistand";

	/**
	 * The financial assistance supplementary-application BPMN process — status + actualisation only, no SSBTEK/calculation.
	 */
	static final String PROCESS_DEFINITION_NAME_SUPPLEMENTARY = "rakel-ekonomiskt-bistand-tillaggsansokan";

	/**
	 * The financial assistance new-application BPMN process — status + actualisation, then a calculation built from the
	 * application (no
	 * SSBTEK/daily loop).
	 */
	static final String PROCESS_DEFINITION_NAME_NEW = "rakel-ekonomiskt-bistand-nyansokan";

	/** The process each financial assistance type starts — the single place the intake and the release agree on. */
	private static final Map<String, String> PROCESS_DEFINITION_BY_SLUG = Map.of(
		SLUG_RENEWAL, PROCESS_DEFINITION_NAME,
		SLUG_SUPPLEMENTARY, PROCESS_DEFINITION_NAME_SUPPLEMENTARY,
		SLUG_NEW, PROCESS_DEFINITION_NAME_NEW);

	// Start-variable keys the BPMN/DMN flow reads (scalars — Operaton receives a plain map, so no list indexing).
	static final String VAR_MUNICIPALITY_ID = "municipalityId";
	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_CO_APPLICANT = "coApplicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";
	// Personnummer + SSBTEK window the fetch worker keys on (financial-aid looks up by personnummer, not partyId).
	static final String VAR_PERSONAL_NUMBER = "personalNumber";
	static final String VAR_CO_APPLICANT_PERSONAL_NUMBER = "coApplicantPersonalNumber";
	static final String VAR_FROM_DATE = "fromDate";
	static final String VAR_TO_DATE = "toDate";

	private final ProcessService processService;
	private final ErrandService errandService;
	private final CitizenService citizenService;

	FinancialAssistanceProcessStarter(final ProcessService processService, final ErrandService errandService,
		final CitizenService citizenService) {
		this.processService = processService;
		this.errandService = errandService;
		this.citizenService = citizenService;
	}

	/**
	 * Start the process belonging to {@code typeSlug} on {@code errandId} and link the instance back. A slug outside the
	 * three financial assistance types has no process and is a no-op. Best-effort.
	 */
	void startFor(final String typeSlug, final String municipalityId, final String namespace, final String errandId,
		final FinancialAssistanceEntity entity) {
		processDefinitionName(typeSlug)
			.ifPresent(processDefinitionName -> start(processDefinitionName, municipalityId, namespace, errandId, entity));
	}

	/**
	 * The BPMN process a financial assistance type starts, when the slug is one of the three financial assistance types.
	 */
	private static Optional<String> processDefinitionName(final String typeSlug) {
		return Optional.ofNullable(typeSlug).map(PROCESS_DEFINITION_BY_SLUG::get);
	}

	private void start(final String processDefinitionName, final String municipalityId, final String namespace, final String errandId,
		final FinancialAssistanceEntity entity) {
		try {
			processService.startProcess(municipalityId, processDefinitionName, errandId, startVariables(municipalityId, namespace, entity))
				.ifPresent(processInstanceId -> errandService.linkProcessInstance(municipalityId, namespace, errandId, processInstanceId));
		} catch (final RuntimeException e) {
			LOG.warn("Could not start '{}' for errand {}", sanitizeForLogging(processDefinitionName), sanitizeForLogging(errandId), e);
		}
	}

	private Map<String, Object> startVariables(final String municipalityId, final String namespace, final FinancialAssistanceEntity entity) {
		final Map<String, Object> variables = new HashMap<>();
		variables.put(VAR_MUNICIPALITY_ID, municipalityId);
		variables.put(VAR_NAMESPACE, namespace);

		final var applicantPartyId = partyId(entity, ROLE_APPLICANT);
		final var coApplicantPartyId = partyId(entity, ROLE_CO_APPLICANT);
		applicantPartyId.ifPresent(id -> variables.put(VAR_APPLICANT, id));
		coApplicantPartyId.ifPresent(id -> variables.put(VAR_CO_APPLICANT, id));

		// The SSBTEK/financial-aid lookup keys on personnummer; resolve it from the partyId. The co-applicant is optional
		// — seed an empty personnummer so the (unconditional) co-applicant fetch resolves to an empty basis.
		variables.put(VAR_PERSONAL_NUMBER, personalNumber(municipalityId, applicantPartyId));
		variables.put(VAR_CO_APPLICANT_PERSONAL_NUMBER, personalNumber(municipalityId, coApplicantPartyId));

		applicationMonth(entity).ifPresent(month -> {
			variables.put(VAR_APPLICATION_MONTH, month);
			// Fetch comparison- (M-2) through the application period (M): SSBTEK payouts attributed to the control period
			// (M-1) are often paid the following month, so the window reaches into M; the rules DMN then selects the
			// relevant periods.
			final var window = YearMonth.parse(month);
			variables.put(VAR_FROM_DATE, window.minusMonths(2).atDay(1).toString());
			variables.put(VAR_TO_DATE, window.atEndOfMonth().toString());
		});
		return variables;
	}

	/**
	 * A household member's personnummer resolved from their partyId, or {@code ""} when the role is absent/unresolvable.
	 */
	private String personalNumber(final String municipalityId, final Optional<String> memberPartyId) {
		return memberPartyId
			.flatMap(id -> citizenService.getPersonalNumber(municipalityId, id))
			.orElse("");
	}

	/** The partyId of the first person holding {@code role} on the application, when one carries a partyId. */
	private static Optional<String> partyId(final FinancialAssistanceEntity entity, final String role) {
		return Optional.ofNullable(entity.getPersons()).orElseGet(List::of).stream()
			.filter(person -> role.equals(person.getRole()))
			.map(FaPerson::getPartyId)
			.filter(StringUtils::hasText)
			.findFirst();
	}

	/** The application month (ISO {@code yyyy-MM}) from the period, present only when both year and month are set. */
	private static Optional<String> applicationMonth(final FinancialAssistanceEntity entity) {
		return Optional.ofNullable(entity.getPeriodYear())
			.flatMap(year -> Optional.ofNullable(entity.getPeriodMonth())
				.map(month -> YearMonth.of(year, month).toString()));
	}
}
