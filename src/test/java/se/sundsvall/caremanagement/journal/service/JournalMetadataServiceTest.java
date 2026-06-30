package se.sundsvall.caremanagement.journal.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.journal.api.model.JournalEntryType;
import se.sundsvall.caremanagement.metadata.api.model.Lookup;
import se.sundsvall.caremanagement.metadata.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalMetadataServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";

	@Mock
	private MetadataService metadataServiceMock;

	@InjectMocks
	private JournalMetadataService service;

	@Test
	void fallsBackToBuiltInCatalogueWhenNoneSeeded() {
		when(metadataServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, "JOURNAL_ENTRY_TYPE")).thenReturn(List.of());

		final var metadata = service.metadata(MUNICIPALITY_ID, NAMESPACE);

		assertThat(metadata.getTypes()).isEqualTo(JournalEntryTypes.TYPES);
	}

	@Test
	void mapsSeededLookupsPreservingOrder() {
		when(metadataServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, "JOURNAL_ENTRY_TYPE")).thenReturn(List.of(
			Lookup.create().withName("PHONE_CONTACT").withDisplayName("Telefonkontakt"),
			Lookup.create().withName("VISIT").withDisplayName("Besök")));

		final var metadata = service.metadata(MUNICIPALITY_ID, NAMESPACE);

		assertThat(metadata.getTypes()).extracting(JournalEntryType::getCode).containsExactly("PHONE_CONTACT", "VISIT");
		assertThat(metadata.getTypes()).extracting(JournalEntryType::getDisplayName).containsExactly("Telefonkontakt", "Besök");
	}
}
