/**
 * Exposed so other modules can resolve namespace-scoped lookup catalogues via {@link MetadataService} — e.g. the
 * journal module backs its "Typ" dropdown with seeded {@code JOURNAL_ENTRY_TYPE} lookups.
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.metadata.service;

import org.springframework.modulith.NamedInterface;
