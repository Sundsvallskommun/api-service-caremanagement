package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_PDF;

class FinancialAssistanceLifecareResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void readDocumentContentInvalidPartyId() {
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
	void readDocumentContentMissingPartyId() {
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
