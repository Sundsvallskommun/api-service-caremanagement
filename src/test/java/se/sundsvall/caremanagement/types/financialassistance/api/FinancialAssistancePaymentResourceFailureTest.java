package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

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
