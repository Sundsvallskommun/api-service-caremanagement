package se.sundsvall.caremanagement.journal.integration.db.model;

/**
 * The write-protection lifecycle of a journal entry. A new entry starts {@link #WORKING} (an editable working note);
 * once
 * {@link #LOCKED} it is a "finalised record" and can no longer be edited or deleted.
 */
public enum JournalEntryStatus {
	WORKING,
	LOCKED
}
