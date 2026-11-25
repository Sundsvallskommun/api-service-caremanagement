package apptest;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.eventlog.integration.db.ErrandEventRepository;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@WireMockAppTestSuite(files = "classpath:/LifecareProfessionalWebIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-lifecare-calculation-it.sql",
	"/db/scripts/testdata-lifecare-professionalweb-it.sql"
})
class LifecareProfessionalWebIT extends AbstractAppTest {

	private static final String ERRAND_ID = "77777777-7777-7777-7777-777777777777";
	private static final String ERRAND_WITHOUT_DECISION_ID = "88888888-8888-8888-8888-888888888888";
	private static final String PATH = "/2281/MY_NAMESPACE/errands/financial-assistance";
	private static final String SENT_BY = "X-Sent-By";
	private static final String CASEWORKER = "joe01doe; type=adAccount";
	private static final String APPLICANT_DOCUMENTS = "/api-lifecare-integrator/2281/professional-web/persons/aaaaaaaa-0000-4000-8000-000000000001/documents";
	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";

	@Autowired
	private ErrandEventRepository errandEventRepository;

	@Autowired
	private FinancialAssistanceRepository financialAssistanceRepository;

	@Test
	void test01_decisionReasonsAreReadThroughTheIntegrator() {
		setupCall()
			.withServicePath(PATH + "/lifecare/decision-types/153/reasons")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_decisionPdfIsPrintedThroughTheIntegrator() throws IOException {
		setupCall()
			.withServicePath(PATH + "/" + ERRAND_ID + "/lifecare/decision/pdf")
			.withHttpMethod(GET)
			.withHeader(SENT_BY, CASEWORKER)
			.withExpectedResponseStatus(OK)
			.withExpectedBinaryResponse("decision.pdf")
			.sendRequestAndVerifyResponse();

		assertThat(errandEventRepository.findAll())
			.extracting(ErrandEventEntity::getErrandId, ErrandEventEntity::getAction, ErrandEventEntity::getTarget, ErrandEventEntity::getLifecareId)
			.contains(tuple(ERRAND_ID, "READ", "DECISION", "815"));
	}

	@Test
	void test03_paymentIsRegisteredThroughTheIntegrator() {
		// The stubs match the household check on the payment date and the create on the body built from the underlag.
		setupCall()
			.withServicePath(PATH + "/" + ERRAND_ID + "/lifecare/payments")
			.withHttpMethod(POST)
			.withHeader(SENT_BY, CASEWORKER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		wiremock.verify(1, postRequestedFor(urlPathEqualTo("/api-lifecare-integrator/2281/professional-web/services/7700/payments")));
		verifyStubs();

		assertThat(financialAssistanceRepository.findWithLifecarePaymentIdsByErrandId(ERRAND_ID))
			.map(FinancialAssistanceEntity::getLifecarePaymentIds)
			.hasValueSatisfying(paymentIds -> assertThat(paymentIds).containsExactly("4242"));
		assertThat(errandEventRepository.findAll())
			.extracting(ErrandEventEntity::getErrandId, ErrandEventEntity::getAction, ErrandEventEntity::getTarget, ErrandEventEntity::getLifecareId)
			.contains(tuple(ERRAND_ID, "READ", "PAYEES", null), tuple(ERRAND_ID, "CREATE", "PAYMENT", "4242"));
	}

	@Test
	void test04_aDecisionLifecareRefusesIsUnprocessableWithTheIntegratorsDetail() {
		setupCall()
			.withServicePath(PATH + "/" + ERRAND_WITHOUT_DECISION_ID + "/lifecare/decision")
			.withHttpMethod(PUT)
			.withHeader(SENT_BY, CASEWORKER)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(UNPROCESSABLE_CONTENT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		wiremock.verify(1, postRequestedFor(urlPathEqualTo("/api-lifecare-integrator/2281/professional-web/services/8800/decisions")));
		verifyStubs();

		assertThat(financialAssistanceRepository.findByErrandId(ERRAND_WITHOUT_DECISION_ID))
			.hasValueSatisfying(entity -> assertThat(entity.getLifecareDecisionId()).isNull());
		assertThat(errandEventRepository.findAll())
			.extracting(ErrandEventEntity::getErrandId, ErrandEventEntity::getAction, ErrandEventEntity::getTarget)
			.contains(tuple(ERRAND_WITHOUT_DECISION_ID, "READ", "DECISION"))
			.doesNotContain(tuple(ERRAND_WITHOUT_DECISION_ID, "CREATE", "DECISION"));
	}

	@Test
	void test05_documentsAreListedByTheApplicantsPartyId() {
		setupCall()
			.withServicePath(PATH + "/" + ERRAND_ID + "/lifecare/documents")
			.withHttpMethod(GET)
			.withHeader(SENT_BY, CASEWORKER)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequest();

		wiremock.verify(1, getRequestedFor(urlPathEqualTo(APPLICANT_DOCUMENTS)));
		wiremock.verify(0, anyRequestedFor(urlPathMatching("/api-citizen/.*")));
		assertThat(errandEventRepository.findAll())
			.extracting(ErrandEventEntity::getErrandId, ErrandEventEntity::getAction, ErrandEventEntity::getTarget)
			.contains(tuple(ERRAND_ID, "READ", "JOURNAL_AND_DOCUMENTS"));
	}

	@Test
	void test06_documentsOfAnApplicantTheIntegratorCannotResolveAreNotFound() {
		setupCall()
			.withServicePath(PATH + "/" + ERRAND_ID + "/lifecare/documents")
			.withHttpMethod(GET)
			.withHeader(SENT_BY, CASEWORKER)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
