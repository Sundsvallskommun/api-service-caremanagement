package apptest;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.stakeholders.integration.db.StakeholderRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@WireMockAppTestSuite(files = "classpath:/ErrandStakeholderIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class ErrandStakeholderIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String STAKEHOLDER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/%s/stakeholders";

	@Autowired
	private StakeholderRepository stakeholderRepository;

	@Test
	void test01_createStakeholder() {
		setupCall()
			.withServicePath(PATH.formatted(ERRAND_ID))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of("^/2281/MY_NAMESPACE/errands/11111111-1111-1111-1111-111111111111/stakeholders/[a-f0-9-]+$"))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readStakeholders() {
		setupCall()
			.withServicePath(PATH.formatted(ERRAND_ID))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test03_readStakeholder() {
		setupCall()
			.withServicePath(PATH.formatted(ERRAND_ID) + "/" + STAKEHOLDER_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_updateStakeholder() {
		setupCall()
			.withServicePath(PATH.formatted(ERRAND_ID) + "/" + STAKEHOLDER_ID)
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test05_deleteStakeholder() {
		assertThat(stakeholderRepository.existsById(STAKEHOLDER_ID)).isTrue();

		setupCall()
			.withServicePath(PATH.formatted(ERRAND_ID) + "/" + STAKEHOLDER_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(stakeholderRepository.existsById(STAKEHOLDER_ID)).isFalse();
	}
}
