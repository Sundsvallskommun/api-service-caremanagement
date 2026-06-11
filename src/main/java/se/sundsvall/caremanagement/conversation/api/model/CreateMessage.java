package se.sundsvall.caremanagement.conversation.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "A new message in the errand's conversation")
public record CreateMessage(
	@Schema(description = "Direction: OUTBOUND = caseworker → applicant, INBOUND = applicant → caseworker", allowableValues = {
		"INBOUND", "OUTBOUND"
	}, examples = "OUTBOUND") @NotBlank @OneOf({
		"INBOUND", "OUTBOUND"
	}) String direction,

	@Schema(description = "Message text", examples = "Please complete your application with a valid certificate.") @NotBlank @Size(max = 8192) String body,

	@Schema(description = "Author id (the caseworker's user id or the applicant's identifier)", examples = "joe01doe") @Size(max = 64) String author) {
}
