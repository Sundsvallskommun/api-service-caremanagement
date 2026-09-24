package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

/**
 * The report on what happened when a manually added payee was put into Lifecare.
 *
 * <p>
 * {@code ALREADY_EXISTS} counts as success: no duplicate is created when the payee is already on
 * the client, and the caseworker's intent — "this payee must be selectable in Lifecare" — is satisfied either way.
 * {@code detail} is required on {@code FAILED} and must be what Lifecare actually said, because that text is what the
 * caseworker gets to see.
 * </p>
 */
@Schema(description = "The report on adding a payee to Lifecare.")
public class PayeeLifecareResult {

	@Schema(description = "What ended up happening in Lifecare", examples = "ADDED", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
		"ADDED", "ALREADY_EXISTS", "FAILED"
	})
	@NotBlank
	@OneOf({
		"ADDED", "ALREADY_EXISTS", "FAILED"
	})
	private String outcome;

	@Schema(description = "The payee id Lifecare gave, when it gave one", examples = "44213")
	@Size(max = 64)
	private String lifecarePayeeId;

	@Schema(description = "Lifecare's own message. Required when outcome is FAILED — it is shown to the caseworker as-is", examples = "Kontonummer har fel format")
	@Size(max = 1024)
	private String detail;

	public static PayeeLifecareResult create() {
		return new PayeeLifecareResult();
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(final String outcome) {
		this.outcome = outcome;
	}

	public PayeeLifecareResult withOutcome(final String outcome) {
		this.outcome = outcome;
		return this;
	}

	public String getLifecarePayeeId() {
		return lifecarePayeeId;
	}

	public void setLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
	}

	public PayeeLifecareResult withLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
		return this;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(final String detail) {
		this.detail = detail;
	}

	public PayeeLifecareResult withDetail(final String detail) {
		this.detail = detail;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PayeeLifecareResult that = (PayeeLifecareResult) o;
		return Objects.equals(outcome, that.outcome) && Objects.equals(lifecarePayeeId, that.lifecarePayeeId)
			&& Objects.equals(detail, that.detail);
	}

	@Override
	public int hashCode() {
		return Objects.hash(outcome, lifecarePayeeId, detail);
	}

	@Override
	public String toString() {
		return "PayeeLifecareResult{" +
			"outcome='" + outcome + '\'' +
			", lifecarePayeeId='" + lifecarePayeeId + '\'' +
			", detail='" + detail + '\'' +
			'}';
	}
}
