package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * A jobbstimulans period on the errand, mirrored out of Lifecare by the RPA supplements ingest. Decision support for
 * the handläggare — the actual jobbstimulans amount on the normberäkning stays the caseworker's call.
 */
@Schema(description = "A jobbstimulans period on the errand, mirrored out of Lifecare.")
public record JobStimulusPeriod(

	@Schema(description = "Whose period it is", examples = "APPLICANT", allowableValues = {
		"APPLICANT", "CO_APPLICANT"
	}) String role,

	@Schema(description = "Period start", examples = "2021-01-01") LocalDate fromDate,

	@Schema(description = "Period end; null for an open period", examples = "2021-12-31") LocalDate toDate) {
}
