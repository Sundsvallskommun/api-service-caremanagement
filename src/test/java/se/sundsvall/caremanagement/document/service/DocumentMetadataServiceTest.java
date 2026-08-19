package se.sundsvall.caremanagement.document.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.document.api.model.DocumentType;
import se.sundsvall.caremanagement.metadata.api.model.Lookup;
import se.sundsvall.caremanagement.metadata.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentMetadataServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";

	@Mock
	private MetadataService metadataServiceMock;

	@InjectMocks
	private DocumentMetadataService service;

	@Test
	void fallsBackToBuiltInCatalogueWhenNoneSeeded() {
		when(metadataServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, "DOCUMENT_TYPE")).thenReturn(List.of());

		final var metadata = service.metadata(MUNICIPALITY_ID, NAMESPACE);

		assertThat(metadata.getTypes()).isEqualTo(DocumentTypes.TYPES);
	}

	@Test
	void mapsSeededLookupsPreservingOrder() {
		when(metadataServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, "DOCUMENT_TYPE")).thenReturn(List.of(
			Lookup.create().withName("LETTER").withDisplayName("Brev"),
			Lookup.create().withName("DECISION").withDisplayName("Beslut")));

		final var metadata = service.metadata(MUNICIPALITY_ID, NAMESPACE);

		assertThat(metadata.getTypes()).extracting(DocumentType::getCode).containsExactly("LETTER", "DECISION");
		assertThat(metadata.getTypes()).extracting(DocumentType::getDisplayName).containsExactly("Brev", "Beslut");
	}
}
