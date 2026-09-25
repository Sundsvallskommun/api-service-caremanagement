package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

/** One income Draken's BFF wrote into the Lifecare calculation from SSBTEK. */
@Schema(description = "One income Draken's BFF wrote into the Lifecare calculation from SSBTEK")
public record AppliedSsbtekChange(

	@Schema(description = "Whose income", examples = "APPLICANT", allowableValues = {
		"APPLICANT", "CO_APPLICANT"
	}, requiredMode = Schema.RequiredMode.REQUIRED) @OneOf({
		"APPLICANT", "CO_APPLICANT"
	}) String role,

	@Schema(description = "The Lifecare income type name, as the change named it", examples = "Lön", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String incomeType,

	@Schema(description = "The amount now in the calculation; null when the income was removed from it", examples = "12400.00") BigDecimal amount) {
}
