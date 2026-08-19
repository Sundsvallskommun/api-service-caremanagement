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
import se.sundsvall.caremanagement.eventlog.api.model.ErrandEventCount;
import se.sundsvall.caremanagement.eventlog.api.model.ErrandEventEntry;
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
		final var event = new ErrandEventEntry("ev1", ERRAND_ID, MUNICIPALITY_ID, NAMESPACE, "HTTP", "READ", "errand", "Öppnade ärendet",
			"GET", "/path", "joe001doe", "adAccount", "req-1", 200, FIXED_TIMESTAMP);
		when(serviceMock.listForErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), isNull(), isNull(), isNull(), eq(true))).thenReturn(List.of(event));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(ErrandEventEntry.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null, null, true);
	}

	@Test
	void listWithFilters() {
		when(serviceMock.listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "UPDATE", "joe001doe", "EVENT", false)).thenReturn(List.of());

		webTestClient.get()
			.uri(uri -> uri.path(PATH)
				.queryParam("action", "UPDATE").queryParam("actor", "joe001doe").queryParam("source", "EVENT").queryParam("includeReads", "false")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(ErrandEventEntry.class);

		verify(serviceMock).listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "UPDATE", "joe001doe", "EVENT", false);
	}

	@Test
	void count() {
		when(serviceMock.countForErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), isNull(), isNull(), isNull(), eq(true))).thenReturn(7L);

		final var body = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/count").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ErrandEventCount.class)
			.returnResult()
			.getResponseBody();

		assertThat(body).isNotNull();
		assertThat(body.count()).isEqualTo(7L);
		verify(serviceMock).countForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null, null, true);
	}

	@Test
	void countWithFilters() {
		when(serviceMock.countForErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), isNull(), isNull(), isNull(), eq(false))).thenReturn(3L);

		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/count").queryParam("includeReads", "false")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).countForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null, null, false);
	}

	@Test
	void listEmpty() {
		when(serviceMock.listForErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), isNull(), isNull(), isNull(), eq(true))).thenReturn(List.of());

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(ErrandEventEntry.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEmpty();
	}
}
