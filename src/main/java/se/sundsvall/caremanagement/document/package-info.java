/**
 * Document module — Formal case documents attached to an errand. The sibling of
 * {@link se.sundsvall.caremanagement.journal journal entries}: in Lifecare both live in the same "Dokumentation"
 * container and share the same shape (Typ, Rubrik, text, Datum/Tid, authorship) and the write-protection lifecycle —
 * but a
 * Dokument is its own concept with its own type catalogue (Dokumenttyp), modelled here so it can later be RPA:ed into
 * Lifecare.
 *
 * <p>
 * Each document carries a {@code type} (Lifecare "Typ"/Dokumenttyp — a municipality-configured value; the
 * {@code /documents/metadata} catalogue offers a dropdown source, backed by the core metadata lookup store — seeded
 * {@code DOCUMENT_TYPE} lookups for the namespace, or a built-in provisional set as fallback), a {@code heading}
 * (Rubrik), free-text {@code text}, a documented {@code documentDate}/{@code documentTime} (Datum/Tid, distinct from
 * the
 * system {@code created} timestamp) and authorship ({@code createdBy} = Upprättad av/Ägare, {@code modifiedBy} = Ändrat
 * av).
 * </p>
 *
 * <p>
 * It also models Lifecare's write-protection lifecycle: a freshly created document is an editable {@code WORKING}
 * draft;
 * locking it ({@code POST .../{id}/lock}) turns it into a {@code LOCKED} "finalised record" — after which edits and
 * deletes are rejected with {@code 409 Conflict}.
 * </p>
 */
@ApplicationModule(displayName = "Documents")
package se.sundsvall.caremanagement.document;

import org.springframework.modulith.ApplicationModule;
