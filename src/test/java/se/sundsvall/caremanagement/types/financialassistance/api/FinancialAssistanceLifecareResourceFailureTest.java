package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

class FinancialAssistanceLifecareResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void readDocumentContent_invalidPartyId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/documents/{documentId}/content").queryParam("partyId", "not-a-uuid").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "documentId", "a3f1c2d4-0000-1111-2222-333344445555")))
			.accept(APPLICATION_PDF)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readDocumentContent.partyId", "not a valid UUID")));

		verifyNoInteractions(lifecareServiceMock);
	}

	@Test
	void readDocumentContent_missingPartyId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/documents/{documentId}/content").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "documentId", "a3f1c2d4-0000-1111-2222-333344445555")))
			.accept(APPLICATION_PDF)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.jsonPath("$.title").isEqualTo("Bad Request")
			.jsonPath("$.status").isEqualTo(400)
			.jsonPath("$.detail").isEqualTo("Required parameter 'partyId' is not present.");

		verifyNoInteractions(lifecareServiceMock);
	}

}
