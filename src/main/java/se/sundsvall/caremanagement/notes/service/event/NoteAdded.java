package se.sundsvall.caremanagement.notes.service.event;

import java.time.OffsetDateTime;

public record NoteAdded(
	String noteId,
	String errandId,
	String author,
	OffsetDateTime timestamp) {}
