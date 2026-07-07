package se.sundsvall.caremanagement.decisions.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.caremanagement.core.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@Schema(
	description = "Decision recorded against an errand. Both system-generated decisions (e.g. a DMN-evaluated recommendation produced by a BPMN process) and human decisions (e.g. a caseworker approving a payment) are stored here, distinguished by `decisionType`. The list on the errand grows over time and is the audit trail of every decision made on the case.")
public class Decision {

	@Schema(description = "Unique identifier", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String id;

	@Schema(description = "Decision category. Free-form string; conventionally `RECOMMENDATION` for DMN-produced suggestions and `PAYMENT` for caseworker APPROVE/REJECT decisions, but namespaces are encouraged to define their own.", examples = "PAYMENT")
	@NotBlank(groups = OnCreate.class)
	@Size(max = 32)
	private String decisionType;

	@Schema(description = "Decision value. For binary outcomes use `APPROVED`/`REJECTED`; for richer outputs (e.g. a calculated amount) use the value itself or a short label.", examples = "APPROVED")
	@NotBlank(groups = OnCreate.class)
	private String value;

	@Schema(description = "Optional human-readable description or motivation for the decision", examples = "Decision proposal per ruleset: 7900 kr, no warning")
	private String description;

	@Schema(description = "Optional decision amount, in SEK. For a financial-assistance decision this is the granted amount (0 for a rejection); for a recommendation it is the recommended amount when the pipeline has computed one.", examples = "7900.00")
	private BigDecimal amount;

	@Schema(description = "Optional decision message communicated to the applicant — the free-text justification shown on the decision letter, kept separate from the internal `description`.",
		examples = "Du beviljas financial assistance för juni 2026 enligt riksnorm.")
	private String decisionMessage;

	@Schema(description = "Optional date the decision applies (the caseworker-chosen decision date), distinct from the server-assigned `created` audit timestamp.", examples = "2026-06-18")
	@DateTimeFormat(iso = DATE)
	private LocalDate decisionDate;

	@Schema(description = "Optional start of the period the decision covers (the month applied for, for a financial-assistance decision).", examples = "2026-06-01")
	@DateTimeFormat(iso = DATE)
	private LocalDate periodFrom;

	@Schema(description = "Optional end of the period the decision covers.", examples = "2026-06-30")
	@DateTimeFormat(iso = DATE)
	private LocalDate periodTo;

	@Schema(description = "Identifier of the actor that produced the decision. Use the caseworker userId for human decisions or a system identifier (e.g. `operaton`, `dmn-engine`) for automated ones.", examples = "jane01doe")
	@Size(max = 64)
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

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public Decision withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getDecisionMessage() {
		return decisionMessage;
	}

	public void setDecisionMessage(final String decisionMessage) {
		this.decisionMessage = decisionMessage;
	}

	public Decision withDecisionMessage(final String decisionMessage) {
		this.decisionMessage = decisionMessage;
		return this;
	}

	public LocalDate getDecisionDate() {
		return decisionDate;
	}

	public void setDecisionDate(final LocalDate decisionDate) {
		this.decisionDate = decisionDate;
	}

	public Decision withDecisionDate(final LocalDate decisionDate) {
		this.decisionDate = decisionDate;
		return this;
	}

	public LocalDate getPeriodFrom() {
		return periodFrom;
	}

	public void setPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
	}

	public Decision withPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
		return this;
	}

	public LocalDate getPeriodTo() {
		return periodTo;
	}

	public void setPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
	}

	public Decision withPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
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
		return Objects.equals(id, that.id) && Objects.equals(decisionType, that.decisionType) && Objects.equals(value, that.value) && Objects.equals(description, that.description)
			&& Objects.equals(amount, that.amount) && Objects.equals(decisionMessage, that.decisionMessage) && Objects.equals(decisionDate, that.decisionDate)
			&& Objects.equals(periodFrom, that.periodFrom) && Objects.equals(periodTo, that.periodTo) && Objects.equals(createdBy, that.createdBy) && Objects.equals(created,
				that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, decisionType, value, description, amount, decisionMessage, decisionDate, periodFrom, periodTo, createdBy, created);
	}

	@Override
	public String toString() {
		return "Decision{" +
			"id='" + id + '\'' +
			", decisionType='" + decisionType + '\'' +
			", value='" + value + '\'' +
			", description='" + description + '\'' +
			", amount=" + amount +
			", decisionMessage='" + decisionMessage + '\'' +
			", decisionDate=" + decisionDate +
			", periodFrom=" + periodFrom +
			", periodTo=" + periodTo +
			", createdBy='" + createdBy + '\'' +
			", created=" + created +
			'}';
	}
}
