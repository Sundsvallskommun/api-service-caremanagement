package se.sundsvall.caremanagement.service;

import generated.se.sundsvall.operaton.CorrelationMessageRequest;
import generated.se.sundsvall.operaton.ModifyVariablesRequest;
import generated.se.sundsvall.operaton.ProcessDefinitionResponse;
import generated.se.sundsvall.operaton.ProcessDefinitionsResponse;
import generated.se.sundsvall.operaton.StartProcessInstanceRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.api.model.Parameter;
import se.sundsvall.caremanagement.integration.operaton.OperatonClient;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ProcessService {

	private static final String NO_DEFINITION_FOUND_MESSAGE = "No Operaton process definition found with name '%s'";

	private final OperatonClient operatonClient;

	ProcessService(final OperatonClient operatonClient) {
		this.operatonClient = operatonClient;
	}

	/**
	 * Starts a process in Operaton with the given definition name, business key and parameters as variables. Returns the
	 * process instance id, or {@link Optional#empty()} if {@code processDefinitionName} is {@code null}. Throws a
	 * {@link Problem} with {@link org.springframework.http.HttpStatus#BAD_REQUEST} if no definition matches the name.
	 */
	public Optional<String> startProcess(final String municipalityId, final String processDefinitionName, final String businessKey, final List<Parameter> parameters) {
		return ofNullable(processDefinitionName)
			.map(name -> {
				final var key = resolveDefinitionKey(municipalityId, name);
				final var response = operatonClient.startProcessInstance(municipalityId, new StartProcessInstanceRequest()
					.processDefinitionKey(key)
					.businessKey(businessKey)
					.variables(toVariables(parameters)));
				return response.getId();
			});
	}

	/**
	 * Adds or updates a single variable on the running process instance. No-op if {@code processInstanceId} is
	 * {@code null} or the parameter has no key.
	 */
	public void updateVariable(final String municipalityId, final String processInstanceId, final Parameter parameter) {
		if (processInstanceId == null || parameter == null || parameter.getKey() == null) {
			return;
		}
		operatonClient.modifyProcessInstanceVariables(municipalityId, processInstanceId, new ModifyVariablesRequest()
			.modifications(Map.of(parameter.getKey(), ofNullable(parameter.getValues()).orElseGet(List::of))));
	}

	/**
	 * Removes a single variable from the running process instance. No-op if {@code processInstanceId} or
	 * {@code parameterKey} is {@code null}.
	 */
	public void deleteVariable(final String municipalityId, final String processInstanceId, final String parameterKey) {
		if (processInstanceId == null || parameterKey == null) {
			return;
		}
		operatonClient.modifyProcessInstanceVariables(municipalityId, processInstanceId, new ModifyVariablesRequest()
			.deletions(List.of(parameterKey)));
	}

	/**
	 * Correlates a BPMN message to the process instance identified by {@code businessKey}. Used to resume a process
	 * waiting on a receive task or message catch event, e.g. when a handläggare clicks Approve/Reject in the UI. The
	 * {@code variables} map (if non-empty) is set on the process instance as part of the correlation.
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

	private static Map<String, Object> toVariables(final List<Parameter> parameters) {
		return ofNullable(parameters).orElseGet(List::of).stream()
			.filter(parameter -> parameter.getKey() != null)
			.collect(toMap(Parameter::getKey, parameter -> ofNullable(parameter.getValues()).orElseGet(List::of), (a, _) -> a));
	}
}
