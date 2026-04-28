package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.Objects;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(
	description = "Decision recorded against an errand. Both system-generated decisions (e.g. a DMN-evaluated recommendation produced by a BPMN process) and human decisions (e.g. a handläggare approving a payment) are stored here, distinguished by `decisionType`. The list on the errand grows over time and is the audit trail of every decision made on the case.")
public class Decision {

	@Schema(description = "Unique identifier", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String id;

	@Schema(description = "Decision category. Free-form string; conventionally `RECOMMENDATION` for DMN-produced suggestions and `PAYMENT` for handläggare APPROVE/REJECT decisions, but namespaces are encouraged to define their own.", examples = "PAYMENT")
	@NotBlank(groups = OnCreate.class)
	private String decisionType;

	@Schema(description = "Decision value. For binary outcomes use `APPROVED`/`REJECTED`; for richer outputs (e.g. a calculated amount) use the value itself or a short label.", examples = "APPROVED")
	@NotBlank(groups = OnCreate.class)
	private String value;

	@Schema(description = "Optional human-readable description or motivation for the decision", examples = "Beslutsförslag enligt regelverk: 7900 kr, ingen varning")
	private String description;

	@Schema(description = "Identifier of the actor that produced the decision. Use the handläggare userId for human decisions or a system identifier (e.g. `operaton`, `dmn-engine`) for automated ones.", examples = "jane01doe")
	private String createdBy;

	@Schema(description = "Timestamp the decision was recorded (server-assigned)", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private OffsetDateTime created;

	public static Decision create() {
		return new Decision();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Decision withId(final String id) {
		this.id = id;
		return this;
	}

	public String getDecisionType() {
		return decisionType;
	}

	public void setDecisionType(final String decisionType) {
		this.decisionType = decisionType;
	}

	public Decision withDecisionType(final String decisionType) {
		this.decisionType = decisionType;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public Decision withValue(final String value) {
		this.value = value;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Decision withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public Decision withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Decision withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Decision that = (Decision) o;
		return Objects.equals(id, that.id) && Objects.equals(decisionType, that.decisionType) && Objects.equals(value, that.value) && Objects.equals(description, that.description) && Objects.equals(createdBy, that.createdBy) && Objects.equals(created,
			that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, decisionType, value, description, createdBy, created);
	}

	@Override
	public String toString() {
		return "Decision{" +
			"id='" + id + '\'' +
			", decisionType='" + decisionType + '\'' +
			", value='" + value + '\'' +
			", description='" + description + '\'' +
			", createdBy='" + createdBy + '\'' +
			", created=" + created +
			'}';
	}
}
