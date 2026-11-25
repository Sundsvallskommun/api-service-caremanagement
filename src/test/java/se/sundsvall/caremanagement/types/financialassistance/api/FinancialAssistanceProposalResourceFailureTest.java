package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.service.DecisionProposalService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceProposalResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/decision-proposal";

	@MockitoBean
	private DecisionProposalService decisionProposalServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private ConstraintViolationProblem get(final String municipalityId, final String namespace, final String errandId) {
		return webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", municipalityId, "namespace", namespace, "errandId", errandId)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();
	}

	private void assertViolation(final ConstraintViolationProblem response, final String field, final String message) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactly(tuple(field, message));
		verifyNoInteractions(decisionProposalServiceMock);
	}

	@Test
	void invalidErrandId() {
		assertViolation(get(MUNICIPALITY_ID, NAMESPACE, "not-a-uuid"), "getDecisionProposal.errandId", "not a valid UUID");
	}

	@Test
	void invalidMunicipalityId() {
		assertViolation(get("bad-municipality-id", NAMESPACE, ERRAND_ID), "getDecisionProposal.municipalityId", "not a valid municipality ID");
	}

	@Test
	void invalidNamespace() {
		assertViolation(get(MUNICIPALITY_ID, "invalid namespace!", ERRAND_ID), "getDecisionProposal.namespace",
			"can only contain A-Z, a-z, 0-9, - and _");
	}
}
