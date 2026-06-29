package se.sundsvall.caremanagement.conversation.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The number of unread messages in the errand's conversation for the calling side")
public record UnreadCount(
	@Schema(description = "Number of messages addressed to the caller that the caller has not yet marked as read", examples = "3") long unreadCount) {
}
