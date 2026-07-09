package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class FinancialAssistanceWarningResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void createWarning_invalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/errand-1/warnings").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(CreateWarningRequest.create().withType("NEW_INCOME").withMessage("Inkomst saknas"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(warningServiceMock);
	}

	@Test
	void listWarnings_invalidMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/errand-1/warnings").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(warningServiceMock);
	}
}
