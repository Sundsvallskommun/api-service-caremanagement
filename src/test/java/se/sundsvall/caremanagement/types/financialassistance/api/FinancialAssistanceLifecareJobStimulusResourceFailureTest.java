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
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriodRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.ErrandLifecareJobStimulusService;
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
class FinancialAssistanceLifecareJobStimulusResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare/job-stimulus-periods";

	@MockitoBean
	private ErrandLifecareJobStimulusService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void readJobStimulusPeriodsInvalidErrandId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> {
				final var body = result.getResponseBody();
				assertThat(body).isNotNull();
				assertThat(body.getStatus()).isEqualTo(BAD_REQUEST);
				assertThat(body.getViolations()).extracting(Violation::field, Violation::message)
					.containsExactly(tuple("readJobStimulusPeriods.errandId", "not a valid UUID"));
			});

		verifyNoInteractions(serviceMock);
	}

	@Test
	void addJobStimulusPeriodInvalidDates() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", randomUUID().toString())))
			.contentType(APPLICATION_JSON)
			.bodyValue(new LifecareJobStimulusPeriodRequest("2028-1-15", "15/01/2030"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> {
				final var body = result.getResponseBody();
				assertThat(body).isNotNull();
				assertThat(body.getViolations()).extracting(Violation::field, Violation::message)
					.containsExactlyInAnyOrder(tuple("fromDate", "fromDate must be YYYY-MM-DD"), tuple("toDate", "toDate must be YYYY-MM-DD"));
			});

		verifyNoInteractions(serviceMock);
	}

	@Test
	void addJobStimulusPeriodMissingFromDate() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", randomUUID().toString())))
			.contentType(APPLICATION_JSON)
			.bodyValue(Map.of())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> {
				final var body = result.getResponseBody();
				assertThat(body).isNotNull();
				assertThat(body.getViolations()).extracting(Violation::field, Violation::message)
					.containsExactly(tuple("fromDate", "must not be null"));
			});

		verifyNoInteractions(serviceMock);
	}
}
