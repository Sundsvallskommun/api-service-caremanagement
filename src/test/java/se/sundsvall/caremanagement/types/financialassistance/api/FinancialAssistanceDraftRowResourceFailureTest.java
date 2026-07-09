package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class FinancialAssistanceDraftRowResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void addDraftIncome_invalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(new NormIncomeInput().withTypeId(20))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(draftRowServiceMock);
	}

	@Test
	void deleteDraftIncome_invalidMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/errand-1/calculation/draft/incomes/r1").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(draftRowServiceMock);
	}
}
