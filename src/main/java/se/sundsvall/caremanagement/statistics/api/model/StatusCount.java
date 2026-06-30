package se.sundsvall.caremanagement.statistics.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Number of errands in a given status")
public record StatusCount(
	@Schema(description = "Status", examples = "DECIDED") String status,
	@Schema(description = "Number of errands in the status", examples = "12") long count) {
}
