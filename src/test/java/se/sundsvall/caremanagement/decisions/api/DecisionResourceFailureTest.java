package se.sundsvall.caremanagement.decisions.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
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
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

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
	void createDecisionBlankDecisionType() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Decision.create().withValue("APPROVED"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createDecision.decision.decisionType", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDecisionBlankValue() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Decision.create().withDecisionType("PAYMENT"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createDecision.decision.value", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDecisionEmptyBody() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Decision.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createDecision.decision.decisionType", "must not be blank"),
				tuple("createDecision.decision.value", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDecisionBadMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Decision.create().withDecisionType("PAYMENT").withValue("APPROVED"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createDecision.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDecisionBadNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.bodyValue(Decision.create().withDecisionType("PAYMENT").withValue("APPROVED"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createDecision.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createDecisionBadErrandIdUuid() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(Decision.create().withDecisionType("PAYMENT").withValue("APPROVED"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createDecision.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisionsBadMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readDecisions.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisionsBadNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readDecisions.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisionsBadErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readDecisions.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisionBadErrandIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE + "/{decisionId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid", "decisionId", DECISION_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readDecision.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisionBadDecisionIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE + "/{decisionId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "decisionId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readDecision.decisionId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteDecisionBadErrandIdUuid() {
		webTestClient.delete()
			.uri(uri -> uri.path(ERRAND_BASE + "/{decisionId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid", "decisionId", DECISION_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteDecision.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteDecisionBadDecisionIdUuid() {
		webTestClient.delete()
			.uri(uri -> uri.path(ERRAND_BASE + "/{decisionId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "decisionId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteDecision.decisionId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteDecisionBadMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(ERRAND_BASE + "/{decisionId}").build(Map.of(
				"municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID, "decisionId", DECISION_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteDecision.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.isNotEmpty()
			.allSatisfy(violation -> assertThat(violation.field()).isNotBlank())
			.allSatisfy(violation -> assertThat(violation.message()).isNotBlank());
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}
}
