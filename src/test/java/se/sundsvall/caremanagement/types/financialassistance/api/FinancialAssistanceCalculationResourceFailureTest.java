package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class FinancialAssistanceCalculationResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void prepareCalculationInvalidApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/prepare").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("123").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("applicant", "not a valid UUID"),
				tuple("errandId", "not a valid UUID")));

		verifyNoInteractions(calculationServiceMock);
	}

	@Test
	void commitCalculationInvalidMonth() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/commit").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-13"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("applicationMonth", "must be an ISO year-month (yyyy-MM)"),
				tuple("errandId", "not a valid UUID")));

		verifyNoInteractions(calculationServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/calculation/prepare",
		"/calculation/commit",
		"/calculation/from-application"
	})
	void calculationMissingErrandId(final String path) {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + path).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("errandId", "not a valid UUID")));

		verifyNoInteractions(calculationServiceMock);
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
