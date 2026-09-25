package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A jobbstimulans period on the errand's insats, as Lifecare holds it. The personnummer is left behind.
 */
@Schema(description = "A jobbstimulans period on the errand's insats, as Lifecare holds it")
public record LifecareJobStimulusPeriod(

	@Schema(description = "Lifecare's jobStimulusId", examples = "101") Integer id,

	@Schema(description = "Whose period it is", examples = "APPLICANT", allowableValues = {
		"APPLICANT", "CO_APPLICANT"
	}) String role,

	@Schema(description = "Period start (yyyy-MM-dd)", examples = "2026-01-01") String fromDate,

	@Schema(description = "Period end (yyyy-MM-dd); absent for an open-ended period", examples = "2027-12-31") String toDate) {
}
