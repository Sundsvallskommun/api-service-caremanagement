package se.sundsvall.caremanagement.eventlog.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The number of activity events on the errand matching the given filters")
public record ErrandEventCount(
	@Schema(description = "Number of matching events", examples = "12") long count) {
}
