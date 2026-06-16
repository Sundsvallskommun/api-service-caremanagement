package se.sundsvall.caremanagement.notes.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNote(
	@NotBlank @Size(max = 8192) String body,
	@Size(max = 64) String modifiedBy) {}
