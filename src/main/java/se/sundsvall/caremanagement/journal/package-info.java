/**
 * Journal module — journalanteckningar (case-journal entries) attached to an errand. Universal across all errand types,
 * the same altitude as {@link se.sundsvall.caremanagement.notes Notes}, but richer: it models the Lifecare
 * journalanteckning shape so it can later be RPA:ed straight into Lifecare.
 *
 * <p>
 * Each entry carries a {@code type} (Lifecare "Typ"/Journaltyp — a municipality-configured, free-text value; the
 * {@code /journal-entries/metadata} catalogue offers a dropdown source, backed by the core metadata lookup store —
 * seeded {@code JOURNAL_ENTRY_TYPE} lookups for the namespace, or a built-in provisional set as fallback), a
 * {@code heading} (Rubrik),
 * free-text {@code text}, a documented {@code entryDateTime} (Datum/Tid, distinct from the system {@code created}
 * timestamp) and authorship ({@code createdBy} = Upprättad av/Ägare, {@code modifiedBy} = Ändrat av).
 * </p>
 *
 * <p>
 * It also models Lifecare's write-protection lifecycle: a freshly created entry is an editable {@code WORKING}
 * working note; locking it ({@code POST .../{id}/lock}) turns it into a {@code LOCKED} "finalised record" — after
 * which edits and deletes are rejected with {@code 409 Conflict}.
 * </p>
 */
@ApplicationModule(displayName = "Journal")
package se.sundsvall.caremanagement.journal;

import org.springframework.modulith.ApplicationModule;
