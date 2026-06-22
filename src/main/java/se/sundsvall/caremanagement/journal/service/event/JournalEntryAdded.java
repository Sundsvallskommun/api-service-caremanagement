package se.sundsvall.caremanagement.journal.service.event;

import java.time.OffsetDateTime;

public record JournalEntryAdded(
	String journalEntryId,
	String errandId,
	String type,
	String createdBy,
	OffsetDateTime timestamp) {}
