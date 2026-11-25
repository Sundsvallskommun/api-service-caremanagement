package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderOptions;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareReminderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecareReminderResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare/reminders";
	private static final Map<String, Object> URI_VARIABLES = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);

	@MockitoBean
	private LifecareReminderService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static LifecareReminderRequest request() {
		return LifecareReminderRequest.create().withReminderDate("2026-09-30").withText("Kontrollera hyran").withPriority(2).withStatus(3);
	}

	@Test
	void listReminders() {
		when(serviceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(LifecareReminder.create().withId(40).withDate("2026-09-30")));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH).build(URI_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(LifecareReminder.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).singleElement().extracting(LifecareReminder::getId).isEqualTo(40);
		verify(serviceMock).list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readReminderOptions() {
		when(serviceMock.options(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(LifecareReminderOptions.create().withDefaultPriority(2));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/options").build(URI_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody(LifecareReminderOptions.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull().extracting(LifecareReminderOptions::getDefaultPriority).isEqualTo(2);
		verify(serviceMock).options(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void createReminder() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(URI_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(request())
			.exchange()
			.expectStatus().isCreated()
			.expectBody().isEmpty();

		verify(serviceMock).create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request());
	}

	@Test
	void updateReminder() {
		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{reminderId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID,
				"reminderId", 40)))
			.contentType(APPLICATION_JSON)
			.bodyValue(request())
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 40, request());
	}

	@Test
	void removeReminder() {
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{reminderId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID,
				"reminderId", 40)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).remove(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 40);
	}
}
