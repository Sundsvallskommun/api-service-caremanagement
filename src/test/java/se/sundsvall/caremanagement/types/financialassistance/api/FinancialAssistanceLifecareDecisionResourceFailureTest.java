package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecareDecisionResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String BASE = "/{municipalityId}/{namespace}/errands/financial-assistance";
	private static final String DECISION_PATH = BASE + "/{errandId}/lifecare/decision";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private LifecareDecisionService serviceMock;

	private ConstraintViolationProblem get(final String path, final Map<String, String> variables) {
		return webTestClient.get()
			.uri(builder -> builder.path(path).build(variables))
			.accept(APPLICATION_JSON, APPLICATION_PDF, APPLICATION_PROBLEM_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();
	}

	private static void assertViolation(final ConstraintViolationProblem response, final String field, final String message) {
		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactly(tuple(field, message));
	}

	@Test
	void readDecisionWithInvalidMunicipalityId() {
		assertViolation(get(DECISION_PATH, Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)),
			"readDecision.municipalityId", "not a valid municipality ID");
		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisionTypesWithInvalidErrandId() {
		assertViolation(get(DECISION_PATH + "/types", Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-valid-uuid")),
			"readDecisionTypes.errandId", "not a valid UUID");
		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisionPdfWithInvalidNamespace() {
		final var response = get(DECISION_PATH + "/pdf", Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace!", "errandId", ERRAND_ID));

		assertThat(response).isNotNull();
		assertThat(response.getViolations()).extracting(Violation::field).containsExactly("readDecisionPdf.namespace");
		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDecisionReasonsWithANonPositiveCode() {
		assertViolation(get(BASE + "/lifecare/decision-types/{decisionCode}/reasons", Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE,
			"decisionCode", "0")), "readDecisionReasons.decisionCode", "must be greater than 0");
		verifyNoInteractions(serviceMock);
	}

	@Test
	void saveDecisionWithoutBeslutstyp() {
		final var response = webTestClient.put()
			.uri(builder -> builder.path(DECISION_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue("{\"amount\": 3000}")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertViolation(response, "decisionCode", "must not be null");
		verifyNoInteractions(serviceMock);
	}

	@Test
	void saveDecisionWithInvalidDate() {
		webTestClient.put()
			.uri(builder -> builder.path(DECISION_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue("{\"decisionCode\": 153, \"periodFrom\": \"2026-13-01\"}")
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
