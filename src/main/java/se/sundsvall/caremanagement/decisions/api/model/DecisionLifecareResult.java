package se.sundsvall.caremanagement.decisions.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

/**
 * The report from Draken's BFF, which writes the decision into Lifecare itself, on how that went. The same shape as
 * the payee and payment reports: outcome, the id Lifecare gave, and Lifecare's own message.
 *
 * <p>
 * {@code ALREADY_EXISTS} counts as success. {@code detail} is required on {@code FAILED} and must be what Lifecare
 * actually said, because that text is what the caseworker gets to see.
 * </p>
 */
@Schema(description = "The report on writing a decision into Lifecare.")
public class DecisionLifecareResult {

	@Schema(description = "What happened when the decision was written to Lifecare",
		examples = "WRITTEN",
		requiredMode = Schema.RequiredMode.REQUIRED,
		allowableValues = {
			"WRITTEN", "ALREADY_EXISTS", "FAILED"
		})
	@NotBlank
	@OneOf({
		"WRITTEN", "ALREADY_EXISTS", "FAILED"
	})
	private String outcome;

	@Schema(description = "The decision id Lifecare gave, when it gave one. Stored as the decision's lifecareId", examples = "88123")
	@Size(max = 64)
	private String lifecareId;

	@Schema(description = "Lifecare's own message. Required when outcome is FAILED — it is shown to the caseworker as-is",
		examples = "Beslutet kunde inte registreras")
	@Size(max = 1024)
	private String detail;

	public static DecisionLifecareResult create() {
		return new DecisionLifecareResult();
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(final String outcome) {
		this.outcome = outcome;
	}

	public DecisionLifecareResult withOutcome(final String outcome) {
		this.outcome = outcome;
		return this;
	}

	public String getLifecareId() {
		return lifecareId;
	}

	public void setLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
	}

	public DecisionLifecareResult withLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
		return this;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(final String detail) {
		this.detail = detail;
	}

	public DecisionLifecareResult withDetail(final String detail) {
		this.detail = detail;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final DecisionLifecareResult that = (DecisionLifecareResult) o;
		return Objects.equals(outcome, that.outcome) && Objects.equals(lifecareId, that.lifecareId) && Objects.equals(detail, that.detail);
	}

	@Override
	public int hashCode() {
		return Objects.hash(outcome, lifecareId, detail);
	}

	@Override
	public String toString() {
		return "DecisionLifecareResult{" +
			"outcome='" + outcome + '\'' +
			", lifecareId='" + lifecareId + '\'' +
			", detail='" + detail + '\'' +
			'}';
	}
}
