package se.sundsvall.caremanagement.notes.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The number of notes on the errand")
public record NoteCount(
	@Schema(description = "Number of notes attached to the errand", examples = "4") long count) {
}
