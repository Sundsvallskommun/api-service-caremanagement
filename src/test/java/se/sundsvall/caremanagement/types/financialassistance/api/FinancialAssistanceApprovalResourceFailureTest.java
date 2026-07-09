package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovalRequest;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

class FinancialAssistanceApprovalResourceFailureTest extends AbstractFinancialAssistanceResourceTest {

	@Test
	void setSectionApproval_missingApproved() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/sections/CALCULATION/approval").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(SectionApprovalRequest.create()) // approved is required
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("approved", "must not be null")));

		verifyNoInteractions(approvalServiceMock);
	}

	@Test
	void setSectionApproval_invalidMunicipalityId() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/errand-1/sections/CALCULATION/approval").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(SectionApprovalRequest.create().withApproved(true))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("setSectionApproval.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(approvalServiceMock);
	}

}
