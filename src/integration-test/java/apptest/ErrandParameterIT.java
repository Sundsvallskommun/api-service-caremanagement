package apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.integration.db.ParameterRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/ErrandParameterIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandParameterIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String RUNNING_PROCESS_ERRAND_ID = "55555555-5555-5555-5555-555555555555";
	private static final String UNKNOWN_ERRAND_ID = "33333333-3333-3333-3333-333333333333";
	private static final String PARAMETER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
	private static final String UNKNOWN_PARAMETER_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
	private static final String BASE_PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/%s/parameters";
	private static final String PATH_WITH_ID = BASE_PATH + "/%s";

	@Autowired
	private ParameterRepository parameterRepository;

	@Test
	void test01_createParameter() {
		setupCall()
			.withServicePath(BASE_PATH.formatted(ERRAND_ID))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^/2281/MY_NAMESPACE/errands/11111111-1111-1111-1111-111111111111/parameters/[a-f0-9-]+$"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readParameters() {
		setupCall()
			.withServicePath(BASE_PATH.formatted(ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_readParameter() {
		setupCall()
			.withServicePath(PATH_WITH_ID.formatted(ERRAND_ID, PARAMETER_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateParameter() {
		setupCall()
			.withServicePath(PATH_WITH_ID.formatted(ERRAND_ID, PARAMETER_ID))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteParameter() {
		assertThat(parameterRepository.existsById(PARAMETER_ID)).isTrue();

		setupCall()
			.withServicePath(PATH_WITH_ID.formatted(ERRAND_ID, PARAMETER_ID))
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(parameterRepository.existsById(PARAMETER_ID)).isFalse();
	}

	@Test
	void test06_readParameterNotFound() {
		setupCall()
			.withServicePath(PATH_WITH_ID.formatted(ERRAND_ID, UNKNOWN_PARAMETER_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test07_readParameterInUnknownErrand() {
		setupCall()
			.withServicePath(PATH_WITH_ID.formatted(UNKNOWN_ERRAND_ID, PARAMETER_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test08_createParameterSyncsOperatonVariable() {
		setupCall()
			.withServicePath(BASE_PATH.formatted(RUNNING_PROCESS_ERRAND_ID))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^/2281/MY_NAMESPACE/errands/55555555-5555-5555-5555-555555555555/parameters/[a-f0-9-]+$"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}
}
