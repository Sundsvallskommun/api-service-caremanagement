package se.sundsvall.caremanagement.document.integration.db.model;

/**
 * The write-protection lifecycle of a document. A new document starts {@link #WORKING} (an editable draft); once
 * {@link #LOCKED} it is a "finalised record" and can no longer be edited or deleted.
 */
public enum DocumentStatus {
	WORKING,
	LOCKED
}
