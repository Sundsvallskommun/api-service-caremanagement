package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovalRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceApprovalService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceApprovalResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private FinancialAssistanceApprovalService approvalServiceMock;

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

	@Test
	void setSectionApprovalMissingApproved() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/sections/CALCULATION/approval").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
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
	void setSectionApprovalInvalidMunicipalityId() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/" + ERRAND_ID + "/sections/CALCULATION/approval").build(Map.of("municipalityId", "x", "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(SectionApprovalRequest.create().withApproved(true))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("setSectionApproval.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(approvalServiceMock);
	}

	@Test
	void setSectionApprovalInvalidErrandId() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/not-a-uuid/sections/CALCULATION/approval").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.contentType(APPLICATION_JSON)
			.bodyValue(SectionApprovalRequest.create().withApproved(true))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("setSectionApproval.errandId", "not a valid UUID")));

		verifyNoInteractions(approvalServiceMock);
	}
}
