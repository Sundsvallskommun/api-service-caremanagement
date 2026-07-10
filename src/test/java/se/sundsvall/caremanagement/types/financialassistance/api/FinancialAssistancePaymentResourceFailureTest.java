package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

class FinancialAssistancePaymentResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void checkPaymentStatusInvalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/payment-status").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(PaymentStatusRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("applicant", "not a valid UUID"),
				tuple("applicationMonth", "must not be null")));

		verifyNoInteractions(paymentServiceMock);
	}
}
