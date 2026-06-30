package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The number of active (OPEN/ACKNOWLEDGED) income warnings on the errand")
public record WarningCount(
	@Schema(description = "Number of active warnings — closed ones are not counted", examples = "3") long count) {
}
