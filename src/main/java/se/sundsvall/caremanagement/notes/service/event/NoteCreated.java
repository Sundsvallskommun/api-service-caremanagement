package se.sundsvall.caremanagement.notes.service.event;

import java.time.OffsetDateTime;

public record NoteCreated(
	String noteId,
	String errandId,
	String municipalityId,
	String namespace,
	String author,
	OffsetDateTime timestamp) {}
