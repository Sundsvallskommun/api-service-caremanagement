package se.sundsvall.caremanagement.types.financialassistance.api;

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
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceSsbtekService;
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
class FinancialAssistanceSsbtekResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/ssbtek";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private FinancialAssistanceSsbtekService ssbtekServiceMock;

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}

	@Test
	void getBasisInvalidPartyId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("partyId", "not-a-uuid").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("getBasis.partyId", "not a valid UUID")));

		verifyNoInteractions(ssbtekServiceMock);
	}

	@Test
	void getBasisInvalidMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).queryParam("partyId", PARTY_ID).build(Map.of("municipalityId", "invalid", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("getBasis.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(ssbtekServiceMock);
	}

	@Test
	void getBasisMissingPartyId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(ssbtekServiceMock);
	}
}
