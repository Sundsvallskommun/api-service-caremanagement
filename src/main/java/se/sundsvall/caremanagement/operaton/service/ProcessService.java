package se.sundsvall.caremanagement.operaton.service;

import generated.se.sundsvall.operaton.CorrelationMessageRequest;
import generated.se.sundsvall.operaton.ProcessDefinitionResponse;
import generated.se.sundsvall.operaton.ProcessDefinitionsResponse;
import generated.se.sundsvall.operaton.StartProcessInstanceRequest;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.operaton.integration.OperatonClient;
import se.sundsvall.dept44.problem.Problem;

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

	private final OperatonClient operatonClient;

	ProcessService(final OperatonClient operatonClient) {
		this.operatonClient = operatonClient;
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
	 * Correlates a BPMN message to the process instance identified by {@code businessKey}. Used to resume a
	 * process waiting on a receive task or message catch event, e.g. when a handläggare clicks Approve/Reject.
	 * The {@code variables} map (if non-null) is set on the process instance as part of the correlation.
	 */
	public void correlateMessage(final String municipalityId, final String messageName, final String businessKey, final Map<String, Object> variables) {
		operatonClient.correlateMessage(municipalityId, new CorrelationMessageRequest()
			.messageName(messageName)
			.businessKey(businessKey)
			.processVariables(ofNullable(variables).orElseGet(Map::of)));
	}

	private String resolveDefinitionKey(final String municipalityId, final String name) {
		return ofNullable(operatonClient.getProcessDefinitionsByName(municipalityId, name))
			.map(ProcessDefinitionsResponse::getProcessDefinitions)
			.flatMap(list -> list.stream().findFirst())
			.map(ProcessDefinitionResponse::getKey)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, NO_DEFINITION_FOUND_MESSAGE.formatted(name)));
	}
}
