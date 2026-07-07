package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_PDF;

class FinancialAssistanceLifecareResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void readDocumentContent_invalidPartyId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/documents/{documentId}/content").queryParam("partyId", "not-a-uuid").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "documentId", "a3f1c2d4-0000-1111-2222-333344445555")))
			.accept(APPLICATION_PDF)
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readDocumentContent_missingPartyId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/documents/{documentId}/content").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "documentId", "a3f1c2d4-0000-1111-2222-333344445555")))
			.accept(APPLICATION_PDF)
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

}
