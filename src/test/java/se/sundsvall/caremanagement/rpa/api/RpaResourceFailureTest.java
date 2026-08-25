package se.sundsvall.caremanagement.rpa.api;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.rpa.api.model.RpaTaskRequest;
import se.sundsvall.caremanagement.rpa.service.RpaService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class RpaResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/rpa-tasks";

	@MockitoBean
	private RpaService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void unknownAction() {
		webTestClient.post()
			.uri(PATH, MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(RpaTaskRequest.create().withAction("NOT_A_REAL_ACTION"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("action", "must be one of: [REGISTER_PAYMENT, WRITE_DECISION, FETCH_SUPPLEMENTS, WRITE_DOCUMENT, WRITE_JOURNAL, WRITE_NORMBERAKNING, WRITE_MONITORING]")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void blankAction() {
		webTestClient.post()
			.uri(PATH, MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(RpaTaskRequest.create().withAction(" "))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("action", "must not be blank"),
				tuple("action", "must be one of: [REGISTER_PAYMENT, WRITE_DECISION, FETCH_SUPPLEMENTS, WRITE_DOCUMENT, WRITE_JOURNAL, WRITE_NORMBERAKNING, WRITE_MONITORING]")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void invalidErrandId() {
		webTestClient.post()
			.uri(PATH, MUNICIPALITY_ID, NAMESPACE, "not-a-uuid")
			.contentType(APPLICATION_JSON)
			.bodyValue(RpaTaskRequest.create().withAction("FETCH_SUPPLEMENTS"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("enqueue.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void invalidMunicipalityId() {
		webTestClient.post()
			.uri(PATH, "abc", NAMESPACE, ERRAND_ID)
			.contentType(APPLICATION_JSON)
			.bodyValue(RpaTaskRequest.create().withAction("FETCH_SUPPLEMENTS"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("enqueue.municipalityId", "not a valid municipality ID")));

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
