package se.sundsvall.caremanagement.journal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.journal.api.model.JournalEntryMetadata;
import se.sundsvall.caremanagement.journal.api.model.JournalEntryType;
import se.sundsvall.caremanagement.metadata.api.model.Lookup;
import se.sundsvall.caremanagement.metadata.service.MetadataService;

/**
 * Assembles the journal "Typ" catalogue served by the metadata endpoint. Reads the namespace-scoped, runtime-seeded
 * journal entry types from the core metadata lookup store (kind {@code JOURNAL_ENTRY_TYPE}, seeded and managed via the
 * {@code /{municipalityId}/{namespace}/metadata} endpoints); when a namespace has none seeded it falls back to the
 * built-in provisional catalogue in {@link JournalEntryTypes}, so the dropdown is never empty.
 */
@Service
public class JournalMetadataService {

	/** Must match the {@code LookupKind} value the metadata store is seeded under. */
	static final String JOURNAL_ENTRY_TYPE_KIND = "JOURNAL_ENTRY_TYPE";

	private final MetadataService metadataService;

	JournalMetadataService(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	@Transactional(readOnly = true)
	public JournalEntryMetadata metadata(final String municipalityId, final String namespace) {
		final var seeded = metadataService.readAll(municipalityId, namespace, JOURNAL_ENTRY_TYPE_KIND);
		if (seeded.isEmpty()) {
			return JournalEntryTypes.metadata();
		}
		return JournalEntryMetadata.create().withTypes(seeded.stream().map(JournalMetadataService::toType).toList());
	}

	private static JournalEntryType toType(final Lookup lookup) {
		return JournalEntryType.create().withCode(lookup.getName()).withDisplayName(lookup.getDisplayName());
	}
}
