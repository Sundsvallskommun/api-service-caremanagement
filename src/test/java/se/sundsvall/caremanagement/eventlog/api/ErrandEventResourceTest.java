package se.sundsvall.caremanagement.eventlog.api;

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
import se.sundsvall.caremanagement.eventlog.api.model.ErrandEvent;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class ErrandEventResourceTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/events";

	@MockitoBean
	private ErrandEventService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void list() {
		final var event = new ErrandEvent("ev1", ERRAND_ID, MUNICIPALITY_ID, NAMESPACE, "HTTP", "READ", "errand", "READ errand",
			"GET", "/path", "joe001doe", "adAccount", "req-1", 200, FIXED_TIMESTAMP);
		when(serviceMock.listForErrand(eq(ERRAND_ID), isNull(), isNull())).thenReturn(List.of(event));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(ErrandEvent.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).listForErrand(ERRAND_ID, null, null);
	}

	@Test
	void listWithFilters() {
		when(serviceMock.listForErrand(eq(ERRAND_ID), eq("READ"), eq("joe001doe"))).thenReturn(List.of());

		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("action", "READ").queryParam("actor", "joe001doe")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(ErrandEvent.class);

		verify(serviceMock).listForErrand(ERRAND_ID, "READ", "joe001doe");
	}

	@Test
	void listEmpty() {
		when(serviceMock.listForErrand(eq(ERRAND_ID), isNull(), isNull())).thenReturn(List.of());

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(ErrandEvent.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEmpty();
	}
}
