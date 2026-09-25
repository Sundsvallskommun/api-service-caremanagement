package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * A new jobbstimulans period for the sökande, written straight to the insats in Lifecare.
 */
@Schema(description = "A new jobbstimulans period for the sökande")
public record LifecareJobStimulusPeriodRequest(

	@Schema(description = "Period start, yyyy-MM-dd", examples = "2028-01-15", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Pattern(regexp = LifecareJobStimulusPeriodRequest.DATE_PATTERN,
		message = "fromDate must be YYYY-MM-DD") String fromDate,

	@Schema(description = "Period end, yyyy-MM-dd; left out, Lifecare's two-year rule sets it", examples = "2030-01-14") @Pattern(regexp = LifecareJobStimulusPeriodRequest.DATE_PATTERN,
		message = "toDate must be YYYY-MM-DD") String toDate) {

	static final String DATE_PATTERN = "^\\d{4}-\\d{2}-\\d{2}$";
}
