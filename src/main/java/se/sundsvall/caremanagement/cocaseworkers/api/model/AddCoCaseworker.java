package se.sundsvall.caremanagement.cocaseworkers.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to add a co-caseworker (medhandläggare) to an errand")
public record AddCoCaseworker(
	@Schema(description = "User id of the co-caseworker to add", examples = "jane01doe") @NotBlank @Size(max = 64) String userId) {
}
