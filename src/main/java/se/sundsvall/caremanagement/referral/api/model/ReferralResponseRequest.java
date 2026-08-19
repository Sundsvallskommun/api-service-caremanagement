package se.sundsvall.caremanagement.referral.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body when registering a response on a referral — just the free-text response. The referral status flips to RESPONDED
 * in the service.
 */
@Schema(description = "Response to a referral.")
public record ReferralResponseRequest(

	@Schema(description = "Response to the referral", examples = "The authority has no objection.") @NotBlank @Size(max = 4096) String responseText) {
}
