package se.sundsvall.caremanagement.api;

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
import se.sundsvall.caremanagement.api.model.Decision;
import se.sundsvall.caremanagement.service.ErrandDecisionService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class ErrandDecisionResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String DECISION_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/decisions";

	@MockitoBean
	private ErrandDecisionService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createDecision() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Decision.class))).thenReturn(DECISION_ID);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Decision.create().withDecisionType("PAYMENT").withValue("APPROVED"))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Decision.class));
	}

	@Test
	void readDecisions() {
		when(serviceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Decision.create()));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Decision.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		verify(serviceMock).readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readDecision() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DECISION_ID)).thenReturn(Decision.create().withId(DECISION_ID));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{decisionId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "decisionId", DECISION_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Decision.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DECISION_ID);
	}

	@Test
	void deleteDecision() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{decisionId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "decisionId", DECISION_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, DECISION_ID);
	}
}
