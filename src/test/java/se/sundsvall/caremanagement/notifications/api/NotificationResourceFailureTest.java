package se.sundsvall.caremanagement.notifications.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.notifications.api.model.Notification;
import se.sundsvall.caremanagement.notifications.service.NotificationService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class NotificationResourceFailureTest {

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
	void createNotification_blankOwnerId() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Notification.create().withType("CREATE").withDescription("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNotification.notification.ownerId", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNotification_blankDescription() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Notification.create().withOwnerId("jane01doe").withType("CREATE"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNotification.notification.description", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNotification_invalidType() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Notification.create().withOwnerId("o").withType("INVALID").withDescription("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNotification.notification.type", "must be one of: [DELETE, CREATE, UPDATE]")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNotification_invalidSubType() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Notification.create().withOwnerId("o").withType("CREATE").withSubType("BANANA").withDescription("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNotification.notification.subType", "must be one of: [SYSTEM, MESSAGE, DECISION, ATTACHMENT, STAKEHOLDER, ERRAND, PARAMETER]")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNotification_typeMissing() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Notification.create().withOwnerId("o").withDescription("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNotification.notification.type", "must not be null")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNotification_badMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", "abc", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(Notification.create().withOwnerId("o").withType("CREATE").withDescription("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNotification.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNotification_badNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.bodyValue(Notification.create().withOwnerId("o").withType("CREATE").withDescription("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNotification.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createNotification_badErrandIdUuid() {
		webTestClient.post()
			.uri(uri -> uri.path(ERRAND_BASE).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(Notification.create().withOwnerId("o").withType("CREATE").withDescription("d"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNotification.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readNotification_badNotificationIdUuid() {
		webTestClient.get()
			.uri(uri -> uri.path(ERRAND_BASE + "/{notificationId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "notificationId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readNotification.notificationId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateNotification_invalidEnumInPatch() {
		webTestClient.patch()
			.uri(uri -> uri.path(ERRAND_BASE + "/{notificationId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "notificationId", NOTIFICATION_ID)))
			.bodyValue(Notification.create().withType("MAGIC"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("updateNotification.notification.type", "must be one of: [DELETE, CREATE, UPDATE]")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readNotificationsByOwner_missingOwnerId() {
		webTestClient.get()
			.uri(uri -> uri.path("/{municipalityId}/{namespace}/notifications").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody()
			.jsonPath("$.title").isEqualTo("Bad Request")
			.jsonPath("$.status").isEqualTo(400)
			.jsonPath("$.detail").isEqualTo("Required parameter 'ownerId' is not present.");

		verifyNoInteractions(serviceMock);
	}

	@Test
	void readNotificationsByOwner_blankOwnerId() {
		webTestClient.get()
			.uri(uri -> uri.path("/{municipalityId}/{namespace}/notifications").queryParam("ownerId", " ")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readNotificationsByOwner.ownerId", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}
}
