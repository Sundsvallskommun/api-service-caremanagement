package se.sundsvall.caremanagement.statushistory.api;

import java.time.OffsetDateTime;
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
import se.sundsvall.caremanagement.statushistory.api.model.StatusHistoryEntry;
import se.sundsvall.caremanagement.statushistory.service.StatusHistoryService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class StatusHistoryResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/status-history";

	@MockitoBean
	private StatusHistoryService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void list() {
		final var entry = new StatusHistoryEntry("h1", ERRAND_ID, "OPEN", "CLOSED", "user", OffsetDateTime.now());
		when(serviceMock.listForErrand(ERRAND_ID)).thenReturn(List.of(entry));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(StatusHistoryEntry.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).listForErrand(ERRAND_ID);
	}

	@Test
	void listEmpty() {
		when(serviceMock.listForErrand(ERRAND_ID)).thenReturn(List.of());

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(StatusHistoryEntry.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEmpty();
	}
}
