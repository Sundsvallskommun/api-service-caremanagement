package se.sundsvall.caremanagement.journal.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.journal.api.model.JournalEntryMetadata;
import se.sundsvall.caremanagement.journal.api.model.JournalEntryType;
import se.sundsvall.caremanagement.metadata.api.model.Lookup;
import se.sundsvall.caremanagement.metadata.service.MetadataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class JournalEntryMetadataResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/journal-entries/metadata";

	@MockitoBean
	private MetadataService metadataServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void metadataFallsBackToBuiltInWhenNoneSeeded() {
		when(metadataServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, "JOURNAL_ENTRY_TYPE")).thenReturn(List.of());

		final var metadata = get();

		assertThat(metadata).isNotNull();
		assertThat(metadata.getTypes()).isNotEmpty();
		assertThat(metadata.getTypes()).extracting(JournalEntryType::getDisplayName).contains("Journalfört meddelande");
	}

	@Test
	void metadataReturnsSeededTypes() {
		when(metadataServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, "JOURNAL_ENTRY_TYPE")).thenReturn(List.of(
			Lookup.create().withName("CUSTOM").withDisplayName("Egen typ")));

		final var metadata = get();

		assertThat(metadata).isNotNull();
		assertThat(metadata.getTypes()).extracting(JournalEntryType::getCode).containsExactly("CUSTOM");
		assertThat(metadata.getTypes()).extracting(JournalEntryType::getDisplayName).containsExactly("Egen typ");
	}

	private JournalEntryMetadata get() {
		return webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(JournalEntryMetadata.class)
			.returnResult()
			.getResponseBody();
	}
}
