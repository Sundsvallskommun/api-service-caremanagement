package se.sundsvall.caremanagement.journal.service;

import java.time.OffsetDateTime;

/**
 * The fields of a Lifecare journal entry to mirror onto an errand — the input to
 * {@link JournalEntryService#mirrorFromLifecare}. {@code lifecareId} is the entry's id in Lifecare's document list (the
 * upsert key), the rest are the mirrored Lifecare fields: Typ, Rubrik, the plain-text body, Datum/Tid and Upprättad av.
 */
public record LifecareJournalEntryMirror(
	String lifecareId,
	String type,
	String heading,
	String text,
	OffsetDateTime entryDateTime,
	String createdBy) {}
