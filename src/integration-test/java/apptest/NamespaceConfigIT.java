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
import se.sundsvall.caremanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/NamespaceConfigIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class NamespaceConfigIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String EXISTING_NAMESPACE = "MY_NAMESPACE";
	private static final String NEW_NAMESPACE = "BRAND_NEW";
	private static final String UNKNOWN_NAMESPACE = "DOES_NOT_EXIST";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/%s/namespace-config";

	@Autowired
	private NamespaceConfigRepository repository;

	@Test
	void test01_readNamespaceConfig() {
		setupCall()
			.withServicePath(PATH.formatted(EXISTING_NAMESPACE))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_createNamespaceConfig() {
		assertThat(repository.findByNamespaceAndMunicipalityId(NEW_NAMESPACE, MUNICIPALITY_ID)).isEmpty();

		setupCall()
			.withServicePath(PATH.formatted(NEW_NAMESPACE))
			.withHttpMethod(POST)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader(LOCATION, List.of(PATH.formatted(NEW_NAMESPACE)))
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.findByNamespaceAndMunicipalityId(NEW_NAMESPACE, MUNICIPALITY_ID)).isPresent();
	}

	@Test
	void test03_updateNamespaceConfig() {
		setupCall()
			.withServicePath(PATH.formatted(EXISTING_NAMESPACE))
			.withHttpMethod(PATCH)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test04_deleteNamespaceConfig() {
		assertThat(repository.findByNamespaceAndMunicipalityId(EXISTING_NAMESPACE, MUNICIPALITY_ID)).isPresent();

		setupCall()
			.withServicePath(PATH.formatted(EXISTING_NAMESPACE))
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.findByNamespaceAndMunicipalityId(EXISTING_NAMESPACE, MUNICIPALITY_ID)).isEmpty();
	}

	@Test
	void test05_readNamespaceConfigNotFound() {
		setupCall()
			.withServicePath(PATH.formatted(UNKNOWN_NAMESPACE))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test06_deleteNamespaceConfigNotFound() {
		setupCall()
			.withServicePath(PATH.formatted(UNKNOWN_NAMESPACE))
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
