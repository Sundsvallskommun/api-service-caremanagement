package se.sundsvall.caremanagement.decisions.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class DecisionResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String DECISION_ID = randomUUID().toString();
	private static final String ERRAND_BASE = "/{municipalityId}/{namespace}/errands/{errandId}/decisions";

	@MockitoBean
	private DecisionService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createDecision_blankDecisionType() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Decision.create().withValue("APPROVED"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDecision_blankValue() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Decision.create().withDecisionType("PAYMENT"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDecision_emptyBody() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Decision.create())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDecision_badMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Decision.create().withDecisionType("PAYMENT").withValue("APPROVED"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDecision_badNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.bodyValue(Decision.create().withDecisionType("PAYMENT").withValue("APPROVED"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDecision_badErrandIdUuid() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(Decision.create().withDecisionType("PAYMENT").withValue("APPROVED"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisions_badMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisions_badNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisions_badErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecision_badErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE + "/{decisionId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid", "decisionId", DECISION_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecision_badDecisionIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE + "/{decisionId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "decisionId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteDecision_badErrandIdUuid() {
		webTestClient.delete()
			.uri(uri -> uri.path(ERRAND_BASE + "/{decisionId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid", "decisionId", DECISION_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteDecision_badDecisionIdUuid() {
		webTestClient.delete()
			.uri(uri -> uri.path(ERRAND_BASE + "/{decisionId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "decisionId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteDecision_badMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(ERRAND_BASE + "/{decisionId}").build(Map.of(
				"municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID, "decisionId", DECISION_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
