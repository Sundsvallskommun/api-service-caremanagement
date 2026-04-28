package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Objects;

@Schema(
	description = "Request body for correlating a BPMN message to the process instance currently running for an errand. The errand id is used as the process business key, so the message is delivered to that specific process. Use this whenever something outside the process (a handläggare action, an external event, an admin override) needs to resume or interact with a running process instance.")
public class ProcessMessageRequest {

	@Schema(description = "BPMN message name, matching the `name` attribute on the `<bpmn:message>` element the receive task references", examples = "PaymentDecisionReceived")
	@NotBlank
	private String messageName;

	@Schema(description = "Process variables to set when correlating the message")
	private Map<String, Object> variables;

	public static ProcessMessageRequest create() {
		return new ProcessMessageRequest();
	}

	public String getMessageName() {
		return messageName;
	}

	public void setMessageName(final String messageName) {
		this.messageName = messageName;
	}

	public ProcessMessageRequest withMessageName(final String messageName) {
		this.messageName = messageName;
		return this;
	}

	public Map<String, Object> getVariables() {
		return variables;
	}

	public void setVariables(final Map<String, Object> variables) {
		this.variables = variables;
	}

	public ProcessMessageRequest withVariables(final Map<String, Object> variables) {
		this.variables = variables;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ProcessMessageRequest that = (ProcessMessageRequest) o;
		return Objects.equals(messageName, that.messageName) && Objects.equals(variables, that.variables);
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageName, variables);
	}

	@Override
	public String toString() {
		return "ProcessMessageRequest{" +
			"messageName='" + messageName + '\'' +
			", variables=" + variables +
			'}';
	}
}
