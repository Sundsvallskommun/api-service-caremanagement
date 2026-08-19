package se.sundsvall.caremanagement.conversation.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import se.sundsvall.caremanagement.conversation.spi.Direction;
import se.sundsvall.dept44.common.validators.annotation.MemberOf;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

@Schema(description = "A new message in the errand's conversation")
public record CreateMessage(
	@Schema(description = "Direction: OUTBOUND = caseworker → applicant, INBOUND = applicant → caseworker", allowableValues = {
		"INBOUND", "OUTBOUND"
	}, examples = "OUTBOUND") @NotBlank @MemberOf(Direction.class) String direction,

	@Schema(description = "Message text", examples = "Please complete your application with a valid certificate.") @NotBlank @Size(max = 8192) String body,

	@Schema(description = "Author id (the caseworker's user id or the applicant's identifier)", examples = "joe01doe") @Size(max = 64) String author,

	@Schema(description = "Id of the message this one replies to. Optional; when set it must reference a message on the same errand.", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479") @ValidUuid(nullable = true) String inReplyToId) {
}
