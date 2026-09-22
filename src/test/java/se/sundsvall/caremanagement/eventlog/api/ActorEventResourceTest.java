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
import se.sundsvall.caremanagement.eventlog.api.model.ActorEventLog;
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
class ActorEventResourceTest {

	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ACTOR = "joe001doe";
	private static final String PATH = "/{municipalityId}/{namespace}/events";

	@MockitoBean
	private ErrandEventService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static ErrandEventEntry entry() {
		return new ErrandEventEntry("ev1", randomUUID().toString(), MUNICIPALITY_ID, NAMESPACE, "HTTP", "READ", "errand", "Öppnade ärendet",
			"GET", "/path", ACTOR, "adAccount", "req-1", 200, FIXED_TIMESTAMP);
	}

	@Test
	void listForActor() {
		when(serviceMock.listForActor(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ACTOR), isNull(), isNull(), isNull(), isNull()))
			.thenReturn(new ActorEventLog(List.of(entry()), 1));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("actor", ACTOR).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ActorEventLog.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.events()).hasSize(1);
		assertThat(response.total()).isEqualTo(1);
		verify(serviceMock).listForActor(MUNICIPALITY_ID, NAMESPACE, ACTOR, null, null, null, null);
	}

	@Test
	void listForActorPassesEveryFilterThrough() {
		final var from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
		final var to = OffsetDateTime.parse("2026-10-01T00:00:00Z");
		when(serviceMock.listForActor(MUNICIPALITY_ID, NAMESPACE, ACTOR, "READ", "HTTP", from, to))
			.thenReturn(new ActorEventLog(List.of(), 0));

		webTestClient.get()
			.uri(uri -> uri.path(PATH)
				.queryParam("actor", ACTOR)
				.queryParam("action", "READ")
				.queryParam("source", "HTTP")
				.queryParam("from", "2026-09-01T00:00:00Z")
				.queryParam("to", "2026-10-01T00:00:00Z")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).listForActor(MUNICIPALITY_ID, NAMESPACE, ACTOR, "READ", "HTTP", from, to);
	}

	@Test
	void listForActorReportsTheTotalSeparatelyFromThePage() {
		// The cap has to be visible: 2 rows out of 4213 must not read like the whole answer.
		when(serviceMock.listForActor(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ACTOR), isNull(), isNull(), isNull(), isNull()))
			.thenReturn(new ActorEventLog(List.of(entry(), entry()), 4213));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("actor", ACTOR).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ActorEventLog.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.events()).hasSize(2);
		assertThat(response.total()).isEqualTo(4213);
	}
}
