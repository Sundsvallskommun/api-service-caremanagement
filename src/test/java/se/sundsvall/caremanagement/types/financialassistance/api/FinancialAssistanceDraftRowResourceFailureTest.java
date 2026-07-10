package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

class FinancialAssistanceDraftRowResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void addDraftIncomeInvalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(new NormIncomeInput().withTypeId(20))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("addDraftIncome.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(draftRowServiceMock);
	}

	@Test
	void deleteDraftIncomeInvalidMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes/r1").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteDraftIncome.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(draftRowServiceMock);
	}
}
