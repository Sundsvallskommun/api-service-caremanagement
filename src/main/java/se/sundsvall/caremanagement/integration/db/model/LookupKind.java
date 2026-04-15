package se.sundsvall.caremanagement.integration.db.model;

/**
 * Discriminator for the generic {@link LookupEntity}. All namespace-scoped, municipality-scoped named reference values
 * are stored in a single {@code lookup} table; the {@code kind} column decides what role a given row plays.
 */
public enum LookupKind {
	CATEGORY,
	STATUS,
	TYPE,
	ROLE,
	CONTACT_REASON
}
