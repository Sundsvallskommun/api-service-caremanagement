package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

/**
 * Request to create a financial assistance income warning directly on an errand — the careM "temp stage", with no
 * Lifecare round-trip.
 * The warning is created {@code OPEN}; a caseworker acknowledges or closes it via the warning PATCH endpoint.
 */
@Schema(description = "Request to create a financial assistance income warning on an errand (no Lifecare round-trip).")
public class CreateWarningRequest {

	@Schema(description = "The warning type", examples = "UNHANDLED_INCOME", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
		"UNHANDLED_INCOME", "INCOME_CHANGE", "MISSING_SSBTEK", "NEW_INCOME", "NEW_EXPENSE", "NEW_PERSON", "INCOME_DROPPED", "HOUSEHOLD_CHANGE"
	})
	@NotBlank
	@OneOf({
		"UNHANDLED_INCOME", "INCOME_CHANGE", "MISSING_SSBTEK", "NEW_INCOME", "NEW_EXPENSE", "NEW_PERSON", "INCOME_DROPPED", "HOUSEHOLD_CHANGE"
	})
	private String type;

	@Schema(description = "Human-readable warning text", examples = "Swish deposits: 2 400 kr - not transferred, requires manual assessment", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String message;

	@Schema(description = "A stable key for the income the warning concerns (benefit/incomeType) — the dedup key. Derived from the message when omitted.", examples = "Swish deposits")
	private String sourceKey;

	public static CreateWarningRequest create() {
		return new CreateWarningRequest();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public CreateWarningRequest withType(final String type) {
		this.type = type;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public CreateWarningRequest withMessage(final String message) {
		this.message = message;
		return this;
	}

	public String getSourceKey() {
		return sourceKey;
	}

	public void setSourceKey(final String sourceKey) {
		this.sourceKey = sourceKey;
	}

	public CreateWarningRequest withSourceKey(final String sourceKey) {
		this.sourceKey = sourceKey;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final CreateWarningRequest that = (CreateWarningRequest) o;
		return Objects.equals(type, that.type) && Objects.equals(message, that.message) && Objects.equals(sourceKey, that.sourceKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, message, sourceKey);
	}

	@Override
	public String toString() {
		return "CreateWarningRequest{" +
			"type='" + type + '\'' +
			", message='" + message + '\'' +
			", sourceKey='" + sourceKey + '\'' +
			'}';
	}
}
