package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;

/**
 * Starts the EB decision-support process when a {@code financial-assistance-renewal} errand is created — the Mina sidor
 * återansökan intake. Runs once the create transaction has committed ({@link ApplicationModuleListener} = after-commit,
 * async, new transaction) so the errand and its typed data are persisted and visible both to the re-read below and to
 * the Operaton workers that call back onto the errand the moment the flow starts.
 *
 * <p>
 * Only renewals start a process; new/supplementary errands are a no-op here. The process is started with
 * {@code businessKey = errandId} and seeded with the household start variables the BPMN/DMN flow reads: the
 * municipalityId and namespace, the applicant and optional co-applicant partyIds (from the application's persons), and
 * the application month derived from the period. Best-effort: a missing process definition or an unavailable engine is
 * logged and swallowed so it never disturbs the citizen's already-committed application — the flow can be (re)started
 * later.
 * </p>
 */
@Component
class FinancialAssistanceErrandCreatedListener {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceErrandCreatedListener.class);

	/** The EB BPMN process, deployed to Operaton via the modeler. Started with businessKey = errandId. */
	static final String PROCESS_DEFINITION_NAME = "rakel-ekonomiskt-bistand";

	// Start-variable keys the BPMN/DMN flow reads (scalars — Operaton receives a plain map, so no list indexing).
	static final String VAR_MUNICIPALITY_ID = "municipalityId";
	static final String VAR_NAMESPACE = "namespace";
	static final String VAR_APPLICANT = "applicant";
	static final String VAR_CO_APPLICANT = "coApplicant";
	static final String VAR_APPLICATION_MONTH = "applicationMonth";

	private final FinancialAssistanceRepository repository;
	private final ProcessService processService;
	private final ErrandService errandService;

	FinancialAssistanceErrandCreatedListener(final FinancialAssistanceRepository repository, final ProcessService processService,
		final ErrandService errandService) {
		this.repository = repository;
		this.processService = processService;
		this.errandService = errandService;
	}

	@ApplicationModuleListener
	void on(final ErrandCreated event) {
		if (!SLUG_RENEWAL.equals(event.typeSlug())) {
			return; // only the återansökan intake starts the EB process
		}
		repository.findByErrandId(event.errandId())
			.ifPresent(entity -> startProcess(event, entity));
	}

	private void startProcess(final ErrandCreated event, final FinancialAssistanceEntity entity) {
		try {
			processService.startProcess(event.municipalityId(), PROCESS_DEFINITION_NAME, event.errandId(), startVariables(event, entity))
				.ifPresent(processInstanceId -> errandService.linkProcessInstance(
					event.municipalityId(), event.namespace(), event.errandId(), processInstanceId));
		} catch (final RuntimeException e) {
			LOG.warn("Could not start '{}' for errand {}", PROCESS_DEFINITION_NAME, event.errandId(), e);
		}
	}

	private static Map<String, Object> startVariables(final ErrandCreated event, final FinancialAssistanceEntity entity) {
		final Map<String, Object> variables = new HashMap<>();
		variables.put(VAR_MUNICIPALITY_ID, event.municipalityId());
		variables.put(VAR_NAMESPACE, event.namespace());
		partyId(entity, ROLE_APPLICANT).ifPresent(id -> variables.put(VAR_APPLICANT, id));
		partyId(entity, ROLE_CO_APPLICANT).ifPresent(id -> variables.put(VAR_CO_APPLICANT, id));
		applicationMonth(entity).ifPresent(month -> variables.put(VAR_APPLICATION_MONTH, month));
		return variables;
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
