package apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.cocaseworkers.integration.db.CoCaseworkerRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/CoCaseworkerIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class CoCaseworkerIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String UNKNOWN_ERRAND_ID = "33333333-3333-3333-3333-333333333333";
	private static final String SEEDED_CO_CASEWORKER_USER_ID = "coworker1";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/%s/co-caseworkers";

	@Autowired
	private CoCaseworkerRepository coCaseworkerRepository;

	@Test
	void test01_addCoCaseworker() {
		setupCall()
			.withServicePath(PATH.formatted(ERRAND_ID))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^/2281/MY_NAMESPACE/errands/" + ERRAND_ID + "/co-caseworkers/coworker2$"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(coCaseworkerRepository.existsByNamespaceAndMunicipalityIdAndErrandIdAndUserId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, "coworker2")).isTrue();
	}

	@Test
	void test02_readCoCaseworkers() {
		// coworker1 is seeded on this errand (testdata-it.sql) alongside its assignee1 owner.
		setupCall()
			.withServicePath(PATH.formatted(ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_addDuplicateCoCaseworkerConflicts() {
		setupCall()
			.withServicePath(PATH.formatted(ERRAND_ID))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CONFLICT)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_removeCoCaseworker() {
		assertThat(coCaseworkerRepository.existsByNamespaceAndMunicipalityIdAndErrandIdAndUserId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, SEEDED_CO_CASEWORKER_USER_ID)).isTrue();

		setupCall()
			.withServicePath(PATH.formatted(ERRAND_ID) + "/" + SEEDED_CO_CASEWORKER_USER_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(coCaseworkerRepository.existsByNamespaceAndMunicipalityIdAndErrandIdAndUserId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, SEEDED_CO_CASEWORKER_USER_ID)).isFalse();
	}

	@Test
	void test05_removeUnknownCoCaseworkerNotFound() {
		setupCall()
			.withServicePath(PATH.formatted(ERRAND_ID) + "/nobody")
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_addCoCaseworkerOnUnknownErrandNotFound() {
		setupCall()
			.withServicePath(PATH.formatted(UNKNOWN_ERRAND_ID))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
