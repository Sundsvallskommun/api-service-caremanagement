package se.sundsvall.caremanagement.usersettings.api;

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
import se.sundsvall.caremanagement.usersettings.api.model.UserSettings;
import se.sundsvall.caremanagement.usersettings.service.UserSettingsService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class UserSettingsResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String AD_ACCOUNT = "joe01doe";
	private static final String PATH = "/{municipalityId}/user-settings/{adAccount}";
	private static final String AD_ACCOUNT_MESSAGE = "must be 1-64 characters of A-Z, a-z, 0-9, '.', '_' and '-'";

	@MockitoBean
	private UserSettingsService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void verifyNoServiceCalls() {
		verifyNoInteractions(serviceMock);
	}

	@Test
	void readUserSettingsBadMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "adAccount", AD_ACCOUNT)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readUserSettings.municipalityId", "not a valid municipality ID")));
	}

	@Test
	void readUserSettingsBadAdAccount() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "adAccount", "joe 01doe")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readUserSettings.adAccount", AD_ACCOUNT_MESSAGE)));
	}

	@Test
	void saveUserSettingsBadAdAccount() {
		webTestClient.put()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "adAccount", "a".repeat(65))))
			.bodyValue(UserSettings.create().withSsbtekOpenInNewWindow(true))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("saveUserSettings.adAccount", AD_ACCOUNT_MESSAGE)));
	}

	@Test
	void saveUserSettingsMissingSsbtekOpenInNewWindow() {
		webTestClient.put()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "adAccount", AD_ACCOUNT)))
			.bodyValue(UserSettings.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("ssbtekOpenInNewWindow", "must not be null")));
	}

	@Test
	void deleteUserSettingsBadMunicipalityId() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "adAccount", AD_ACCOUNT)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("deleteUserSettings.municipalityId", "not a valid municipality ID")));
	}

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}
}
