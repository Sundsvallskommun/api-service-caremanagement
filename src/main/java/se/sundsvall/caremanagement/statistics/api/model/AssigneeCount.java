package se.sundsvall.caremanagement.statistics.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Number of errands assigned to a given user")
public record AssigneeCount(
	@Schema(description = "The assigned user id", examples = "joe01doe") String assignedUserId,
	@Schema(description = "Number of errands assigned to the user", examples = "5") long count) {
}
