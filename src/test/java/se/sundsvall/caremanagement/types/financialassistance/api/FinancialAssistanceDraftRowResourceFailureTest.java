package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

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
