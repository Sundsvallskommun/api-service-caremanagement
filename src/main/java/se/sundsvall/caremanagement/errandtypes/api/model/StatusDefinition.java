package se.sundsvall.caremanagement.errandtypes.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One allowed status for an errand type — the {@code code} stored on the errand together with its human-readable
 * {@code displayName}. Lets a frontend (e.g. Draken) render the status without hard-coding a label per code. Emitted in
 * lifecycle order, not alphabetically.
 */
@Schema(description = "An allowed status for an errand type — the stored code plus its human-readable label.")
public record StatusDefinition(
	@Schema(description = "The status code stored on the errand", examples = "UNDER_REVIEW") String code,
	@Schema(description = "Human-readable label for the status", examples = "Under utredning") String displayName) {

	public StatusDefinition {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("code must not be blank");
		}
	}
}
