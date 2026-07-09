package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

class FinancialAssistanceWarningResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void createWarning_invalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/errand-1/warnings").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateWarningRequest.create().withType("NEW_INCOME").withMessage("Inkomst saknas"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createWarning.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(warningServiceMock);
	}

	@Test
	void listWarnings_invalidMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/errand-1/warnings").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("listWarnings.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(warningServiceMock);
	}
}
