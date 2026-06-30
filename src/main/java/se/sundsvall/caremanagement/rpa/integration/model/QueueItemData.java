package se.sundsvall.caremanagement.rpa.integration.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * The inner payload of a UiPath {@code AddQueueItem} call. Property names are PascalCase to match the Orchestrator
 * OData contract. {@code Reference} is the errandId (the robot uses it to read the case back from CareManagement and to
 * deduplicate re-runs); {@code SpecificContent} carries the action and any extra hints the robot needs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueItemData(

	@JsonProperty("Name") String name,

	@JsonProperty("Reference") String reference,

	@JsonProperty("Priority") String priority,

	@JsonProperty("SpecificContent") Map<String, String> specificContent) {
}
