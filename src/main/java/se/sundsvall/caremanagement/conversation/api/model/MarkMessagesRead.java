package se.sundsvall.caremanagement.conversation.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "The messages the caller has read, to be marked as read for the calling side")
public record MarkMessagesRead(

	@Schema(description = "Ids of the read messages. Must reference messages on the same errand.", examples = "[\"f47ac10b-58cc-4372-a567-0e02b2c3d479\"]") @NotEmpty List<@ValidUuid String> messageIds) {
}
