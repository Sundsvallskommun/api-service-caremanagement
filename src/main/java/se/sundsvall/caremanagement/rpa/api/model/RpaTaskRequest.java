package se.sundsvall.caremanagement.rpa.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Objects;
import se.sundsvall.caremanagement.rpa.service.RpaAction;
import se.sundsvall.dept44.common.validators.annotation.MemberOf;

/**
 * Request to enqueue an RPA task on an errand. {@code action} selects the Lifecare GUI flow the robot runs (see
 * {@code RpaAction}); {@code parameters} are optional extra hints placed in the queue item's {@code SpecificContent}.
 */
@Schema(description = "Request to enqueue a UiPath RPA task on an errand.")
public class RpaTaskRequest {

	@Schema(description = "The RPA action — selects the Lifecare flow the robot runs", examples = "FETCH_SUPPLEMENTS", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
		"FETCH_SUPPLEMENTS", "WRITE_NORMBERAKNING", "WRITE_DECISION", "WRITE_JOURNAL", "WRITE_DOCUMENT", "WRITE_MONITORING", "REGISTER_PAYMENT"
	})
	@NotBlank
	@MemberOf(RpaAction.class)
	private String action;

	@Schema(description = "Optional extra hints for the robot, merged into the queue item SpecificContent")
	private Map<String, String> parameters;

	public static RpaTaskRequest create() {
		return new RpaTaskRequest();
	}

	public String getAction() {
		return action;
	}

	public void setAction(final String action) {
		this.action = action;
	}

	public RpaTaskRequest withAction(final String action) {
		this.action = action;
		return this;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(final Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public RpaTaskRequest withParameters(final Map<String, String> parameters) {
		this.parameters = parameters;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final RpaTaskRequest that = (RpaTaskRequest) o;
		return Objects.equals(action, that.action) && Objects.equals(parameters, that.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(action, parameters);
	}

	@Override
	public String toString() {
		return "RpaTaskRequest{" +
			"action='" + action + '\'' +
			", parameters=" + parameters +
			'}';
	}
}
