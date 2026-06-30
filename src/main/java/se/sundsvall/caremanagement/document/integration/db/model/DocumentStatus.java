package se.sundsvall.caremanagement.document.integration.db.model;

/**
 * The skrivskydd lifecycle of a document. A new document starts {@link #WORKING} (an editable draft); once
 * {@link #LOCKED} it is an "upprättad handling" and can no longer be edited or deleted.
 */
public enum DocumentStatus {
	WORKING,
	LOCKED
}
