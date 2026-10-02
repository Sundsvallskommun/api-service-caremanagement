package se.sundsvall.caremanagement.usersettings.api;

import java.util.Map;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class UserSettingsResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String AD_ACCOUNT = "joe01doe";
	private static final String PATH = "/{municipalityId}/user-settings/{adAccount}";

	@MockitoBean
	private UserSettingsService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(serviceMock);
	}

	@Test
	void readUserSettings() {
		final var settings = UserSettings.create().withAdAccount(AD_ACCOUNT).withSsbtekOpenInNewWindow(true);
		when(serviceMock.read(MUNICIPALITY_ID, AD_ACCOUNT)).thenReturn(settings);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "adAccount", AD_ACCOUNT)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(UserSettings.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(settings);
		verify(serviceMock).read(MUNICIPALITY_ID, AD_ACCOUNT);
	}

	@Test
	void saveUserSettings() {
		final var settings = UserSettings.create().withSsbtekOpenInNewWindow(false);

		webTestClient.put()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "adAccount", AD_ACCOUNT)))
			.bodyValue(settings)
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL);

		verify(serviceMock).save(MUNICIPALITY_ID, AD_ACCOUNT, settings);
	}

	@Test
	void deleteUserSettings() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "adAccount", AD_ACCOUNT)))
			.exchange()
			.expectStatus().isNoContent()
			.expectHeader().contentType(ALL);

		verify(serviceMock).delete(MUNICIPALITY_ID, AD_ACCOUNT);
	}
}
