package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareReminderService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecareReminderResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final String INVALID_MUNICIPALITY_ID = "bad-municipality-id";
	private static final String INVALID_UUID = "not-a-valid-uuid";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare/reminders";

	@MockitoBean
	private LifecareReminderService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}

	private static LifecareReminderRequest validRequest() {
		return LifecareReminderRequest.create().withReminderDate("2026-09-30").withText("Kontrollera hyran").withPriority(2).withStatus(3);
	}

	@Test
	void listRemindersWithInvalidMunicipalityId() {
		webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", INVALID_MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("listReminders.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void listRemindersWithInvalidErrandId() {
		webTestClient.get()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", INVALID_UUID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("listReminders.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createReminderWithInvalidBody() {
		webTestClient.post()
			.uri(builder -> builder.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(LifecareReminderRequest.create().withReminderDate("30/09/2026").withText(" "))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("reminderDate", "must be YYYY-MM-DD"),
				tuple("text", "must not be blank"),
				tuple("priority", "must not be null"),
				tuple("status", "must not be null")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateReminderWithInvalidReminderId() {
		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/{reminderId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID,
				"reminderId", 0)))
			.contentType(APPLICATION_JSON)
			.bodyValue(validRequest())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("updateReminder.reminderId", "must be greater than 0")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void removeReminderWithInvalidNamespace() {
		webTestClient.delete()
			.uri(builder -> builder.path(PATH + "/{reminderId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace!", "errandId",
				ERRAND_ID, "reminderId", 40)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
