package se.sundsvall.caremanagement.journal.service.event;

import java.time.OffsetDateTime;

public record JournalEntryCreated(
	String journalEntryId,
	String errandId,
	String municipalityId,
	String namespace,
	String type,
	String createdBy,
	OffsetDateTime timestamp) {}
