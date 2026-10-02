package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.service.HouseholdIdentifiersService;
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
class HouseholdIdentifiersResourceFailureTest {

	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/household-identifiers";

	@MockitoBean
	private HouseholdIdentifiersService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void verifyNoCalls() {
		verifyNoInteractions(serviceMock);
	}

	private static Map<String, String> variables(final String municipalityId, final String namespace, final String errandId) {
		return Map.of("municipalityId", municipalityId, "namespace", namespace, "errandId", errandId);
	}

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactlyInAnyOrder(violations);
	}

	@Test
	void getHouseholdIdentifiersWithInvalidPathVariables() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(variables("bad-municipality-id", "bad namespace", "not-a-valid-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("getHouseholdIdentifiers.municipalityId", "not a valid municipality ID"),
				tuple("getHouseholdIdentifiers.namespace", "can only contain A-Z, a-z, 0-9, - and _"),
				tuple("getHouseholdIdentifiers.errandId", "not a valid UUID")));
	}
}
