/**
 * Journal module — the catalogue of Lifecare journal types (Typ/Journaltyp), served at
 * {@code /journal-entries/metadata}. The Draken template admin tags each journal template with one of these codes.
 *
 * <p>
 * The journalanteckningar themselves live in Lifecare: careM reads and writes them on the errand's insats through
 * ProfessionalWeb (the financial assistance {@code /lifecare/documents} routes) and keeps no copy here.
 * </p>
 *
 * <p>
 * The catalogue is backed by the core metadata lookup store (seeded {@code JOURNAL_ENTRY_TYPE} lookups for the
 * namespace), with a built-in provisional set as fallback.
 * </p>
 */
@ApplicationModule(displayName = "Journal")
package se.sundsvall.caremanagement.journal;

import org.springframework.modulith.ApplicationModule;
