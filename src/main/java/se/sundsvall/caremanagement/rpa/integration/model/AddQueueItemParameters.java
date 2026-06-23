package se.sundsvall.caremanagement.rpa.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for UiPath Orchestrator {@code Queues/UiPathODataSvc.AddQueueItem}: a single {@link QueueItemData}
 * wrapped in {@code itemData}.
 */
public record AddQueueItemParameters(

	@JsonProperty("itemData") QueueItemData itemData) {
}
