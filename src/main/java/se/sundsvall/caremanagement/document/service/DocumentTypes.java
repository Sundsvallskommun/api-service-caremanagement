package se.sundsvall.caremanagement.document.service;

import java.util.List;
import se.sundsvall.caremanagement.document.api.model.DocumentMetadata;
import se.sundsvall.caremanagement.document.api.model.DocumentType;

/**
 * The provisional document type catalogue — an English {@code code} paired with the Swedish Lifecare label.
 *
 * <p>
 * Lifecare document types ("Typ"/Dokumenttyp) are configured per municipality (IFO-handbok §3.4.14), so this is
 * <em>not</em> an authoritative Sundsvall set and {@code Document.type} is intentionally not validated against it. It
 * exists so the frontend (Draken) has a dropdown source now and an RPA flow has a label to select later; in practice
 * the
 * served catalogue is the seeded {@code DOCUMENT_TYPE} lookups for the namespace, falling back to this set when none
 * are
 * seeded. The Lifecare screens showed Sundsvall-specific labels ("BE Dokument", "BE Brev"); the generic types below are
 * common IFO document types, provisional until the real Sundsvall configuration is seeded.
 * </p>
 */
public final class DocumentTypes {

	private DocumentTypes() {}

	/** Provisional document types. The Swedish {@code displayName} is what an RPA flow selects in Lifecare. */
	public static final List<DocumentType> TYPES = List.of(
		type("LETTER", "Brev"),
		type("DOCUMENT", "Dokument"),
		type("INVESTIGATION", "Utredning"),
		type("SERVICE_STATEMENT", "Tjänsteutlåtande"),
		type("DECISION_NOTIFICATION", "Beslutsmeddelande"),
		type("COMMUNICATION", "Kommunicering"),
		type("FORM", "Blankett"));

	/** The assembled metadata response the metadata endpoint returns when no types are seeded for the namespace. */
	public static DocumentMetadata metadata() {
		return DocumentMetadata.create().withTypes(TYPES);
	}

	private static DocumentType type(final String code, final String displayName) {
		return DocumentType.create().withCode(code).withDisplayName(displayName);
	}
}
