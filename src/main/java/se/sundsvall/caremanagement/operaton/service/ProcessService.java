package se.sundsvall.caremanagement.operaton.service;

import generated.se.sundsvall.operaton.CorrelationMessageRequest;
import generated.se.sundsvall.operaton.ProcessDefinitionResponse;
import generated.se.sundsvall.operaton.ProcessDefinitionsResponse;
import generated.se.sundsvall.operaton.StartProcessInstanceRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.operaton.integration.OperatonClient;
import se.sundsvall.caremanagement.operaton.integration.db.ProcessMessageRetryRepository;
import se.sundsvall.caremanagement.operaton.integration.db.model.ProcessMessageRetryEntity;
import se.sundsvall.caremanagement.operaton.integration.model.EvaluateDecisionRequest;
import se.sundsvall.caremanagement.operaton.integration.model.EvaluateDecisionResponse;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.json.JsonMapper;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Operaton glue — generic. Type modules call into this when they need to kick off, correlate,
 * or terminate a BPMN flow. The parameter-swamp variants ({@code updateVariable(Parameter)} /
 * {@code deleteVariable(String)}) have been removed; pass a {@code Map<String, Object>} directly.
 */
@Service
public class ProcessService {

	private static final String NO_DEFINITION_FOUND_MESSAGE = "No Operaton process definition found with name '%s'";

	/** A queued message waits this long before its first retry. */
	static final long FIRST_RETRY_DELAY_MINUTES = 1;
	static final String RETRY_STATUS_PENDING = "PENDING";
	static final int MAX_ERROR_LENGTH = 1024;

	private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

	private final OperatonClient operatonClient;
	private final ErrandAccessGuard errandGuard;
	private final ProcessMessageRetryRepository processMessageRetryRepository;

	ProcessService(final OperatonClient operatonClient, final ErrandAccessGuard errandGuard, final ProcessMessageRetryRepository processMessageRetryRepository) {
		this.operatonClient = operatonClient;
		this.errandGuard = errandGuard;
		this.processMessageRetryRepository = processMessageRetryRepository;
	}

	/**
	 * Starts a process in Operaton with the given definition name, business key, and seed variables.
	 * Returns the process instance id, or {@link Optional#empty()} if {@code processDefinitionName} is null.
	 * Throws a {@link Problem} with BAD_REQUEST if no definition matches the name.
	 */
	public Optional<String> startProcess(final String municipalityId, final String processDefinitionName,
		final String businessKey, final Map<String, Object> variables) {

		return ofNullable(processDefinitionName)
			.map(name -> {
				final var key = resolveDefinitionKey(municipalityId, name);
				final var response = operatonClient.startProcessInstance(municipalityId, new StartProcessInstanceRequest()
					.processDefinitionKey(key)
					.businessKey(businessKey)
					.variables(ofNullable(variables).orElseGet(Map::of)));
				return response.getId();
			});
	}

	/**
	 * Correlates a BPMN message to the process instance identified by {@code businessKey} (the errandId). Used to resume a
	 * process waiting on a receive task or message catch event, e.g. when a caseworker clicks Approve/Reject.
	 * The {@code variables} map (if non-null) is set on the process instance as part of the correlation.
	 *
	 * <p>
	 * Asserts the errand exists in the caller's tenant first, so a caller cannot nudge a process by naming an errandId
	 * that does not belong to their (municipalityId, namespace).
	 */
	public void correlateMessage(final String municipalityId, final String namespace, final String messageName, final String businessKey, final Map<String, Object> variables) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, businessKey);
		operatonClient.correlateMessage(municipalityId, new CorrelationMessageRequest()
			.messageName(messageName)
			.businessKey(businessKey)
			.processVariables(ofNullable(variables).orElseGet(Map::of)));
	}

	/**
	 * Queue a message that could not be correlated for another attempt. Call it in the transaction that recorded what
	 * the message reports — then the record and the retry commit together, and a saved decision can never silently leave
	 * its process waiting. The scheduled retry re-sends it with backoff until the engine accepts it, and gives up after
	 * a few days; see {@code ProcessMessageRetryWorker}.
	 *
	 * @param firstError why the first attempt failed, kept on the row (truncated) for whoever reads it next
	 */
	public void queueMessageRetry(final String municipalityId, final String namespace, final String messageName, final String businessKey,
		final Map<String, Object> variables, final String firstError) {

		final var now = OffsetDateTime.now();
		processMessageRetryRepository.save(ProcessMessageRetryEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withErrandId(businessKey)
			.withMessageName(messageName)
			.withVariables(JSON_MAPPER.writeValueAsString(ofNullable(variables).orElseGet(Map::of)))
			.withStatus(RETRY_STATUS_PENDING)
			.withAttempts(1)
			.withLastError(truncate(firstError))
			.withNextAttempt(now.plusMinutes(FIRST_RETRY_DELAY_MINUTES))
			.withCreated(now));
	}

	/** The variables a queued message carries, as they were queued. */
	public static Map<String, Object> variablesOf(final ProcessMessageRetryEntity retry) {
		return ofNullable(retry.getVariables())
			.map(json -> JSON_MAPPER.readerForMapOf(Object.class).<Map<String, Object>>readValue(json))
			.orElseGet(Map::of);
	}

	/** An error text cut to what the retry row can hold. */
	public static String truncate(final String error) {
		if (error == null || error.length() <= MAX_ERROR_LENGTH) {
			return error;
		}
		return error.substring(0, MAX_ERROR_LENGTH);
	}

	/**
	 * Evaluate a deployed DMN decision in the engine and return its result rows (one map of output name → value per
	 * matched rule). Used by type modules to run modeler-editable rules — e.g. the calculation expense cap — without
	 * reaching into the Operaton REST client directly. Returns an empty list when the decision produces no rows.
	 */
	public List<Map<String, Object>> evaluateDecision(final String municipalityId, final String decisionKey, final Map<String, Object> variables) {
		final var response = operatonClient.evaluateDecision(municipalityId, decisionKey, new EvaluateDecisionRequest(ofNullable(variables).orElseGet(Map::of)));
		return ofNullable(response).map(EvaluateDecisionResponse::results).orElseGet(List::of);
	}

	private String resolveDefinitionKey(final String municipalityId, final String name) {
		return ofNullable(operatonClient.getProcessDefinitionsByName(municipalityId, name))
			.map(ProcessDefinitionsResponse::getProcessDefinitions)
			.flatMap(list -> list.stream().findFirst())
			.map(ProcessDefinitionResponse::getKey)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, NO_DEFINITION_FOUND_MESSAGE.formatted(name)));
	}
}
