package se.sundsvall.caremanagement.types.financialassistance.api;

import java.math.BigDecimal;
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
import se.sundsvall.caremanagement.types.financialassistance.api.model.AppliedSsbtekChange;
import se.sundsvall.caremanagement.types.financialassistance.api.model.AppliedSsbtekChanges;
import se.sundsvall.caremanagement.types.financialassistance.service.CalculationSyncService;
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
class FinancialAssistanceCalculationSyncResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/calculation/ssbtek-changes";

	@MockitoBean
	private CalculationSyncService calculationSyncServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getChangesInvalidErrandId() {
		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field).containsExactly("getChanges.errandId");
		verifyNoInteractions(calculationSyncServiceMock);
	}

	@Test
	void appliedInvalidBody() {
		final var request = new AppliedSsbtekChanges(null, List.of(new AppliedSsbtekChange("SPOUSE", " ", BigDecimal.ONE)));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/applied").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations()).extracting(Violation::field)
			.containsExactlyInAnyOrder("calculationId", "applied[0].role", "applied[0].incomeType");
		verifyNoInteractions(calculationSyncServiceMock);
	}

	@Test
	void appliedEmptyList() {
		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/applied").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(new AppliedSsbtekChanges(4242, List.of()))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactly(tuple("applied", "must not be empty"));
		verifyNoInteractions(calculationSyncServiceMock);
	}
}
