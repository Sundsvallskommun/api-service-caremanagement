package apptest;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.eventlog.integration.db.ErrandEventRepository;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

@WireMockAppTestSuite(files = "classpath:/LifecareProfessionalWebIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql",
	"/db/scripts/testdata-lifecare-calculation-it.sql"
})
class LifecareProfessionalWebIT extends AbstractAppTest {

	private static final String ERRAND_ID = "77777777-7777-7777-7777-777777777777";
	private static final String PATH = "/2281/MY_NAMESPACE/errands/financial-assistance";
	private static final String SENT_BY = "X-Sent-By";
	private static final String CASEWORKER = "joe01doe; type=adAccount";

	@Autowired
	private ErrandEventRepository errandEventRepository;

	@Test
	void test01_decisionReasonsAreReadThroughTheIntegrator() {
		setupCall()
			.withServicePath(PATH + "/lifecare/decision-types/153/reasons")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
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
}
