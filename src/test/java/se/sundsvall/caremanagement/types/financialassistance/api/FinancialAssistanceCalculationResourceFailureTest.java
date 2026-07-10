package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

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

	@Test
	void prepareCalculationMissingErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/prepare").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("errandId", "not a valid UUID")));

		verifyNoInteractions(calculationServiceMock);
	}

	@Test
	void commitCalculationMissingErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/commit").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("errandId", "not a valid UUID")));

		verifyNoInteractions(calculationServiceMock);
	}

	@Test
	void commitFromApplicationMissingErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/from-application").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("errandId", "not a valid UUID")));

		verifyNoInteractions(calculationServiceMock);
	}

}
