package se.sundsvall.caremanagement.journal.service;

import java.util.List;
import se.sundsvall.caremanagement.journal.api.model.JournalEntryMetadata;
import se.sundsvall.caremanagement.journal.api.model.JournalEntryType;

/**
 * The provisional journal entry type catalogue — an English {@code code} paired with the Swedish Lifecare label.
 *
 * <p>
 * Lifecare journal types ("Typ"/Journaltyp) are configured per municipality (IFO-handbok §3.4.16.4 / §3.4.17), so this
 * is <em>not</em> an authoritative Sundsvall set and {@code JournalEntry.type} is intentionally not validated against
 * it.
 * It exists so the frontend (Draken) has a dropdown source now and an RPA flow has a label to select later. Replace
 * with the real Sundsvall Lifecare configuration once known. {@code "Journalfört meddelande"} is the one confirmed
 * value
 * (from the Lifecare screens); the rest are common IFO journal types, provisional until verified.
 * </p>
 */
public final class JournalEntryTypes {

	private JournalEntryTypes() {}

	/** Provisional journal entry types. The Swedish {@code displayName} is what an RPA flow selects in Lifecare. */
	public static final List<JournalEntryType> TYPES = List.of(
		type("JOURNALED_MESSAGE", "Journalfört meddelande"),
		type("SERVICE_NOTE", "Tjänsteanteckning"),
		type("PHONE_CONTACT", "Telefonkontakt"),
		type("VISIT", "Besök"),
		type("HOME_VISIT", "Hembesök"),
		type("INCOMING_DOCUMENT", "Inkommen handling"),
		type("OTHER", "Övrigt"));

	/** The assembled metadata response the metadata endpoint returns. */
	public static JournalEntryMetadata metadata() {
		return JournalEntryMetadata.create().withTypes(TYPES);
	}

	private static JournalEntryType type(final String code, final String displayName) {
		return JournalEntryType.create().withCode(code).withDisplayName(displayName);
	}
}
