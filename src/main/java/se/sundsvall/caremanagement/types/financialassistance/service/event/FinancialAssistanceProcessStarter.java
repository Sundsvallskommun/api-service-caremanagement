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
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Starts an EB process on an errand and seeds the household start variables the BPMN/DMN flow reads. The renewal intake
 * ({@link FinancialAssistanceErrandCreatedListener}) and the manual-review release
 * ({@link FinancialAssistanceReleaseListener}) start the full decision-support process
 * ({@code rakel-ekonomiskt-bistand}); the supplementary intake starts the lighter tilläggsansökan process
 * ({@code rakel-ekonomiskt-bistand-tillaggsansokan}) and the new-application intake the nyansökan process
 * ({@code rakel-ekonomiskt-bistand-nyansokan}) — both reuse the same first steps (status + actualisation) without
 * SSBTEK/normberäkning, the nyansökan additionally building a normberäkning straight from what the citizen declared. In
 * each case the actualisation step resolves the handläggare off the applicant's most recent Lifecare insats (for a
 * tilläggsansökan: the ongoing återansökan's caseworker), so the errand ends up on that caseworker rather than the
 * default assignee (a nyansökan has no prior insats and keeps the default assignee).
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

	/** The EB decision-support BPMN process (renewal), deployed to Operaton via the modeler. businessKey = errandId. */
	static final String PROCESS_DEFINITION_NAME = "rakel-ekonomiskt-bistand";

	/** The EB tilläggsansökan BPMN process — status + actualisation only, no SSBTEK/normberäkning. */
	static final String PROCESS_DEFINITION_NAME_SUPPLEMENTARY = "rakel-ekonomiskt-bistand-tillaggsansokan";

	/**
	 * The EB nyansökan BPMN process — status + actualisation, then a normberäkning built from the application (no
	 * SSBTEK/daily loop).
	 */
	static final String PROCESS_DEFINITION_NAME_NEW = "rakel-ekonomiskt-bistand-nyansokan";

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

	/** Start the EB decision-support process (renewal) on {@code errandId} and link the instance back. Best-effort. */
	void start(final String municipalityId, final String namespace, final String errandId, final FinancialAssistanceEntity entity) {
		start(PROCESS_DEFINITION_NAME, municipalityId, namespace, errandId, entity);
	}

	/** Start the EB tilläggsansökan process on {@code errandId} and link the instance back. Best-effort. */
	void startSupplementary(final String municipalityId, final String namespace, final String errandId, final FinancialAssistanceEntity entity) {
		start(PROCESS_DEFINITION_NAME_SUPPLEMENTARY, municipalityId, namespace, errandId, entity);
	}

	/** Start the EB nyansökan process on {@code errandId} and link the instance back. Best-effort. */
	void startNew(final String municipalityId, final String namespace, final String errandId, final FinancialAssistanceEntity entity) {
		start(PROCESS_DEFINITION_NAME_NEW, municipalityId, namespace, errandId, entity);
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
