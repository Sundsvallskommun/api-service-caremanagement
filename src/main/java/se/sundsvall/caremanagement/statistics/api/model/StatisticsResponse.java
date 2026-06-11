package se.sundsvall.caremanagement.statistics.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Aggregated errand statistics for the caseworker interface")
public record StatisticsResponse(
	@Schema(description = "Total number of errands in the selection", examples = "42") long total,
	@Schema(description = "Number of errands per status") List<StatusCount> byStatus,
	@Schema(description = "Number of errands per assigned user") List<AssigneeCount> byAssignee,
	@Schema(description = "Number of errands without an assigned user", examples = "7") long unassigned) {
}
