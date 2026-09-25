package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningRowInput;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.ErrandLifecareCalculationService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.ErrandNormberakningService;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecareCalculationResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare";

	@MockitoBean
	private ErrandLifecareCalculationService calculationServiceMock;
	@MockitoBean
	private ErrandNormberakningService normberakningServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void verifyNoCalls() {
		verifyNoInteractions(calculationServiceMock, normberakningServiceMock);
	}

	private static Map<String, String> variables(final String municipalityId, final String namespace, final String errandId) {
		return Map.of("municipalityId", municipalityId, "namespace", namespace, "errandId", errandId);
	}

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations()).extracting(Violation::field, Violation::message).containsExactlyInAnyOrder(violations);
	}

	@Test
	void readCalculationWithInvalidPathVariables() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/calculation").build(variables("bad-municipality-id", "bad namespace", "not-a-valid-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readCalculation.municipalityId", "not a valid municipality ID"),
				tuple("readCalculation.namespace", "can only contain A-Z, a-z, 0-9, - and _"),
				tuple("readCalculation.errandId", "not a valid UUID")));
	}

	@Test
	void saveCalculationWithoutBody() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/calculation").build(variables(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(Problem.class)
			.consumeWith(result -> assertThat(result.getResponseBody()).isNotNull());
	}

	@Test
	void readPdfWithInvalidErrandId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/calculation/pdf").build(variables(MUNICIPALITY_ID, NAMESPACE, "not-a-valid-uuid")))
			.accept(APPLICATION_PDF, APPLICATION_PROBLEM_JSON)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(), tuple("readCalculationPdf.errandId", "not a valid UUID")));
	}

	@Test
	void addRowToAnUnknownSection() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/normberakning/others").build(variables(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(NormberakningRowInput.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(), tuple("addNormberakningRow.section", "must be persons, incomes or expenses")));
	}

	@Test
	void addRowWithInvalidInput() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/normberakning/expenses").build(variables(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(NormberakningRowInput.create().withBucket("OTHER").withApplicantAmountDate("igår").withNote("x".repeat(81)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("bucket", "must be one of: [EXPENSE, SPECIAL_EXPENSE]"),
				tuple("applicantAmountDate", "must match \"^\\d{4}-\\d{2}-\\d{2}.*$\""),
				tuple("note", "size must be between 0 and 80")));
	}

	@Test
	void updateRowWithInvalidRowId() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/normberakning/incomes/bad_row").build(variables(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(NormberakningRowInput.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(), tuple("updateNormberakningRow.rowId", "not a valid row id")));
	}

	@Test
	void deleteRowInAnUnknownSection() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/normberakning/header/1").build(variables(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(), tuple("deleteNormberakningRow.section", "must be persons, incomes or expenses")));
	}

	@Test
	void restoreRowWithInvalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/normberakning/incomes/1/restore").build(variables("x", NAMESPACE, ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(), tuple("restoreNormberakningRow.municipalityId", "not a valid municipality ID")));
	}

	@Test
	void updateHeaderWithoutBody() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/normberakning/header").build(variables(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.exchange()
			.expectStatus().isBadRequest();
	}
}
