package se.sundsvall.caremanagement.document.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.document.api.model.DocumentMetadata;
import se.sundsvall.caremanagement.document.api.model.DocumentType;
import se.sundsvall.caremanagement.metadata.api.model.Lookup;
import se.sundsvall.caremanagement.metadata.service.MetadataService;

/**
 * Assembles the document "Typ" catalogue served by the metadata endpoint. Reads the namespace-scoped, runtime-seeded
 * document types from the core metadata lookup store (kind {@code DOCUMENT_TYPE}, seeded and managed via the
 * {@code /{municipalityId}/{namespace}/metadata} endpoints); when a namespace has none seeded it falls back to the
 * built-in provisional catalogue in {@link DocumentTypes}, so the dropdown is never empty.
 */
@Service
public class DocumentMetadataService {

	/** Must match the {@code LookupKind} value the metadata store is seeded under. */
	static final String DOCUMENT_TYPE_KIND = "DOCUMENT_TYPE";

	private final MetadataService metadataService;

	DocumentMetadataService(final MetadataService metadataService) {
		this.metadataService = metadataService;
	}

	@Transactional(readOnly = true)
	public DocumentMetadata metadata(final String municipalityId, final String namespace) {
		final var seeded = metadataService.readAll(municipalityId, namespace, DOCUMENT_TYPE_KIND);
		if (seeded.isEmpty()) {
			return DocumentTypes.metadata();
		}
		return DocumentMetadata.create().withTypes(seeded.stream().map(DocumentMetadataService::toType).toList());
	}

	private static DocumentType toType(final Lookup lookup) {
		return DocumentType.create().withCode(lookup.getName()).withDisplayName(lookup.getDisplayName());
	}
}
