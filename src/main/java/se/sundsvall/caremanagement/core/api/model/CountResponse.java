package se.sundsvall.caremanagement.core.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Count of errands matching the supplied filter")
public record CountResponse(
	@Schema(description = "Number of matching errands", examples = "42") long count) {
}
