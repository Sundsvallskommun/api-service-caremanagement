package se.sundsvall.caremanagement.journal.integration.db.model;

/**
 * The skrivskydd lifecycle of a journal entry. A new entry starts {@link #WORKING} (an editable arbetsanteckning); once
 * {@link #LOCKED} it is an "upprättad handling" and can no longer be edited or deleted.
 */
public enum JournalEntryStatus {
	WORKING,
	LOCKED
}
