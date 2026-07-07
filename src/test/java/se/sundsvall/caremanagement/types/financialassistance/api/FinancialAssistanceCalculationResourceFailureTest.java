package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;

import static org.mockito.Mockito.verifyNoInteractions;

class FinancialAssistanceCalculationResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void prepareCalculation_invalidApplicant() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/prepare").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("123").withApplicationMonth("2026-06"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void commitCalculation_invalidMonth() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation/commit").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.bodyValue(CalculationRequest.create().withApplicant("f47ac10b-58cc-4372-a567-0e02b2c3d479").withApplicationMonth("2026-13"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

}
