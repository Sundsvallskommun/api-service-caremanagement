package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One jobbstimulans period as Lifecare returns it. {@code jobStimulusId} is deliberately not modelled — Lifecare
 * regenerates every id on each save, so the id carries no identity. Periods flagged {@code markedForRemoval} are
 * dropped on ingest.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "One jobbstimulans period. Lifecare's unstable jobStimulusId is intentionally absent.")
public record LifecareJobStimulusPeriod(

	@Schema(description = "Period start (yyyy-MM-dd). Required — periods without a parseable date are reported FAILED.", examples = "2021-01-01") String fromDate,

	@Schema(description = "Period end (yyyy-MM-dd); optional", examples = "2021-12-31") String toDate,

	@Schema(description = "Lifecare's removal flag — a period marked for removal is dropped on ingest", examples = "false") Boolean markedForRemoval) {}
