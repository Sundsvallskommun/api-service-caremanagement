package se.sundsvall.caremanagement.eventlog.api;

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
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class ActorEventResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ACTOR = "joe001doe";
	private static final String PATH = "/{municipalityId}/{namespace}/events";

	@MockitoBean
	private ErrandEventService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void listForActorWithInvalidMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("actor", ACTOR).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("listForActor.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void listForActorWithInvalidNamespace() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("actor", ACTOR).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "#invalid#")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertThat(result.getResponseBody()).isNotNull());

		verifyNoInteractions(serviceMock);
	}

	@Test
	void listForActorWithBlankActor() {
		// An empty actor would quietly return the whole namespace's log to whoever asked — the audit read has to name
		// the person it is following up.
		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("actor", " ").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("listForActor.actor", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void listForActorWithMissingActor() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void listForActorWithUnparseableTimestamp() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("actor", ACTOR).queryParam("from", "igår").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	private static void assertConstraintViolation(final ConstraintViolationProblem problem, final Tuple... expected) {
		assertThat(problem).isNotNull();
		assertThat(problem.getTitle()).isEqualTo("Constraint Violation");
		assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(problem.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(expected);
	}
}
