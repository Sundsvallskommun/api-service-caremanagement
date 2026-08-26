/**
 * Journal service layer.
 *
 * <p>
 * Exposed so the financial assistance supplements ingest can upsert Lifecare journal-entry mirrors through
 * {@link se.sundsvall.caremanagement.journal.service.JournalEntryService#mirrorFromLifecare} without reaching into the
 * module's API or persistence layer.
 * </p>
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.journal.service;

import org.springframework.modulith.NamedInterface;
