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

/**
 * Starts the EB decision-support process ({@code rakel-ekonomiskt-bistand}) on an errand and seeds the household start
 * variables the BPMN/DMN flow reads. Shared by the renewal intake ({@link FinancialAssistanceErrandCreatedListener})
 * and by the manual-review release ({@link FinancialAssistanceReleaseListener}) so both kick off an identical flow.
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

	/** The EB BPMN process, deployed to Operaton via the modeler. Started with businessKey = errandId. */
	static final String PROCESS_DEFINITION_NAME = "rakel-ekonomiskt-bistand";

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

	/** Start the EB process on {@code errandId} and link the instance back onto the errand. Best-effort. */
	void start(final String municipalityId, final String namespace, final String errandId, final FinancialAssistanceEntity entity) {
		try {
			processService.startProcess(municipalityId, PROCESS_DEFINITION_NAME, errandId, startVariables(municipalityId, namespace, entity))
				.ifPresent(processInstanceId -> errandService.linkProcessInstance(municipalityId, namespace, errandId, processInstanceId));
		} catch (final RuntimeException e) {
			LOG.warn("Could not start '{}' for errand {}", PROCESS_DEFINITION_NAME, errandId, e);
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
