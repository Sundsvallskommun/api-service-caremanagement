package apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.usersettings.integration.db.UserSettingsRepository;
import se.sundsvall.caremanagement.usersettings.integration.db.model.UserSettingsEntity;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/UserSettingsIT/", classes = Application.class)
@Sql({
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata-it.sql"
})
class UserSettingsIT extends AbstractAppTest {

	private static final String REQUEST_FILE = "request.json";
	private static final String RESPONSE_FILE = "response.json";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String EXISTING_AD_ACCOUNT = "joe01doe";
	private static final String NEW_AD_ACCOUNT = "new01user";
	private static final String PATH = "/" + MUNICIPALITY_ID + "/user-settings/%s";

	@Autowired
	private UserSettingsRepository repository;

	@Test
	void test01_readUserSettings() {
		setupCall()
			.withServicePath(PATH.formatted(EXISTING_AD_ACCOUNT))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test02_readUserSettingsDefaults() {
		setupCall()
			.withServicePath(PATH.formatted(NEW_AD_ACCOUNT))
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();

		assertThat(repository.findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NEW_AD_ACCOUNT)).isEmpty();
	}

	@Test
	void test03_createUserSettings() {
		setupCall()
			.withServicePath(PATH.formatted("NEW01USER"))
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, NEW_AD_ACCOUNT))
			.map(UserSettingsEntity::getSsbtekOpenInNewWindow)
			.contains(false);
	}

	@Test
	void test04_updateUserSettings() {
		setupCall()
			.withServicePath(PATH.formatted(EXISTING_AD_ACCOUNT))
			.withHttpMethod(PUT)
			.withRequest(REQUEST_FILE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, EXISTING_AD_ACCOUNT))
			.map(UserSettingsEntity::getSsbtekOpenInNewWindow)
			.contains(true);
	}

	@Test
	void test05_deleteUserSettings() {
		setupCall()
			.withServicePath(PATH.formatted(EXISTING_AD_ACCOUNT))
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.withExpectedResponseBodyIsNull()
			.sendRequestAndVerifyResponse();

		assertThat(repository.findByMunicipalityIdAndAdAccount(MUNICIPALITY_ID, EXISTING_AD_ACCOUNT)).isEmpty();
	}
}
