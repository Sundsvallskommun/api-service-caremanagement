package se.sundsvall.caremanagement.notifications.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.notifications.api.model.Notification;
import se.sundsvall.caremanagement.notifications.service.NotificationService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class NotificationResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String NOTIFICATION_ID = randomUUID().toString();
	private static final String ERRAND_BASE = "/{municipalityId}/{namespace}/errands/{errandId}/notifications";

	@MockitoBean
	private NotificationService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void createNotification() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Notification.class))).thenReturn(NOTIFICATION_ID);

		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Notification.create().withOwnerId("jane01doe").withType("CREATE").withSubType("ERRAND").withDescription("d"))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().exists("Location");

		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Notification.class));
	}

	@Test
	void readNotifications() {
		when(serviceMock.readAllByErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Sort.class)))
			.thenReturn(List.of(Notification.create().withId(NOTIFICATION_ID)));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Notification.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).readAllByErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Sort.class));
	}

	@Test
	void readNotification() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTIFICATION_ID))
			.thenReturn(Notification.create().withId(NOTIFICATION_ID).withOwnerId("jane01doe"));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE + "/{notificationId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "notificationId", NOTIFICATION_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Notification.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getOwnerId()).isEqualTo("jane01doe");
	}

	@Test
	void updateNotification() {
		webTestClient.patch()
			.uri(uri -> uri.path(ERRAND_BASE + "/{notificationId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "notificationId", NOTIFICATION_ID)))
			.bodyValue(Notification.create().withAcknowledged(true))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(NOTIFICATION_ID), any(Notification.class));
	}

	@Test
	void deleteNotification() {
		webTestClient.delete()
			.uri(uri -> uri.path(ERRAND_BASE + "/{notificationId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "notificationId", NOTIFICATION_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTIFICATION_ID);
	}

	@Test
	void acknowledgeAll() {
		when(serviceMock.acknowledgeAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(3);

		webTestClient.put()
			.uri(uri -> uri.path(ERRAND_BASE + "/acknowledged").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).acknowledgeAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readNotificationsByOwner() {
		when(serviceMock.readAllByOwner(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("jane01doe"), any(Sort.class)))
			.thenReturn(List.of(Notification.create().withId(NOTIFICATION_ID).withOwnerId("jane01doe")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path("/{municipalityId}/{namespace}/notifications").queryParam("ownerId", "jane01doe")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Notification.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
	}
}
