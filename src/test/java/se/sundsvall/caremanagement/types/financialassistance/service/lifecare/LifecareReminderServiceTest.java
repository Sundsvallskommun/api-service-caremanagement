package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderRequest;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareReminderService.PATH_CREATE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareReminderService.PATH_EDIT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareReminderService.PATH_LIST;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareReminderService.PATH_PROPOSAL;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareReminderService.PATH_REMOVE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareReminderService.PATH_UPDATE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareReminderFixtures.CURRENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareReminderFixtures.PROPOSAL;

@ExtendWith(MockitoExtension.class)
class LifecareReminderServiceTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final LifecareErrand ERRAND = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 2, null, null, null, 2026, 9);
	private static final Map<String, String> BY_SERVICE = Map.of("id", "2");

	/** Today is pinned so the dates below stay in the future however long the tests live. */
	private static final Clock TODAY = Clock.fixed(LocalDateTime.of(2026, 9, 23, 0, 30).atZone(ZoneId.of("Europe/Stockholm")).toInstant(),
		ZoneId.of("Europe/Stockholm"));

	private static final String LIST = """
		{ "reminders": [ { "reminderId": 40, "reminderDate": "2026-09-23", "status": 3, "statusText": "Ej påbörjad", "priority": 2,
		  "priorityText": "Normal", "personId": "199001122390", "personName": "Jeppson, Test", "caseworkerId": "TEST",
		  "caseworkerName": "Test Handläggare", "type": 3, "typeText": "Manuell bevakning insats", "text": "Hej", "objectType": 7083,
		  "objectTypeName": "IFO.Insats" } ] }
		""";

	@Mock
	private ProfessionalWebClient client;
	@Mock
	private LifecareErrandService errandService;
	@Mock
	private LifecareAccessRecorder recorder;
	@Mock
	private StakeholderService stakeholderService;

	@Captor
	private ArgumentCaptor<ObjectNode> bodyCaptor;

	private LifecareReminderService service;

	@BeforeEach
	void setUp() {
		service = new LifecareReminderService(client, errandService, recorder, stakeholderService, TODAY);
	}

	private static JsonNode json(final String text) {
		return JSON.readTree(text);
	}

	private static JsonNode proposal() {
		return json(PROPOSAL);
	}

	private static LifecareReminderRequest request(final String date) {
		return LifecareReminderRequest.create().withReminderDate(date).withText("Kontrollera hyran").withPriority(2).withStatus(3);
	}

	private void givenErrand() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
	}

	@Test
	void list() {
		givenErrand();
		when(client.get(PATH_LIST, BY_SERVICE)).thenReturn(json(LIST));

		final var result = service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).extracting(LifecareReminder::getId).containsExactly(40);
		verify(recorder).read(ERRAND, "REMINDERS", "Läste bevakningar i Lifecare");
	}

	@Test
	void listIsNotServedWhenTheReadCannotBeLogged() {
		givenErrand();
		when(client.get(PATH_LIST, BY_SERVICE)).thenReturn(json(LIST));
		doThrow(new IllegalStateException("log down")).when(recorder).read(any(), anyString(), anyString());

		assertThatThrownBy(() -> service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void listWithoutAnInsats() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null, null, null, null, null));

		assertThatThrownBy(() -> service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(CONFLICT));
		verifyNoInteractions(client, recorder);
	}

	@Test
	void options() {
		givenErrand();
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal());

		final var options = service.options(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(options.getDefaultPriority()).isEqualTo(2);
		assertThat(options.getPriorities()).hasSize(2);
	}

	@Test
	void createHangsTheBevakningOnTheInsatsForTheApplicant() {
		givenErrand();
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal());
		when(errandService.applicantPersonalNumber(ERRAND)).thenReturn("199001122390");
		when(stakeholderService.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Stakeholder.create().withRole("CO_APPLICANT").withFirstName("Other").withLastName("Person"),
			Stakeholder.create().withRole("APPLICANT").withFirstName("Test").withLastName("Jeppson")));

		service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request("2026-09-30"));

		verify(client).post(eq(PATH_CREATE), eq(Map.of()), bodyCaptor.capture());
		final var body = bodyCaptor.getValue();
		assertThat(body.path("objectType").intValue()).isEqualTo(7083);
		assertThat(body.path("objectTypeName").stringValue()).isEqualTo("IFO.Insats");
		assertThat(body.path("objectId").intValue()).isEqualTo(2);
		assertThat(body.path("personId").stringValue()).isEqualTo("199001122390");
		assertThat(body.path("personName").stringValue()).isEqualTo("Jeppson, Test");
		assertThat(body.path("caseworkerId").stringValue()).isEqualTo("TEST");
		assertThat(body.path("caseworkerName").stringValue()).isEqualTo("Test Handläggare");
		assertThat(body.path("type").intValue()).isEqualTo(3);
		assertThat(body.path("reminderDate").stringValue()).isEqualTo("2026-09-30");
		verify(recorder).written(ERRAND, LifecareAccessEntry.CREATE, "REMINDER", "Lade till en bevakning i Lifecare", null);
	}

	@Test
	void createTakesTodayAndAnApplicantWithoutAName() {
		givenErrand();
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal());
		when(errandService.applicantPersonalNumber(ERRAND)).thenReturn("199001122390");
		when(stakeholderService.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());

		service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request("2026-09-23"));

		verify(client).post(eq(PATH_CREATE), eq(Map.of()), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().path("personName").stringValue()).isEmpty();
	}

	@Test
	void createRefusesADateInThePast() {
		givenErrand();

		assertRefused(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request("2026-09-22")), BAD_REQUEST, LifecareReminderService.PAST_DATE);
		verifyNoInteractions(client);
	}

	@Test
	void createRefusesAnInsatsWithoutACaseworker() {
		givenErrand();
		final var proposal = proposal();
		((ObjectNode) proposal.path("reminderTypeObjects").path(1).path("associations").path(0)).put("caseworkerId", "");
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal);

		assertRefused(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request("2026-09-30")), BAD_REQUEST, LifecareReminderService.NO_CASEWORKER);
		verify(client, never()).post(anyString(), anyMap(), any());
	}

	@Test
	void createRefusesWhenLifecareOffersNoManualInsatsBevakning() {
		givenErrand();
		final var proposal = proposal();
		((ObjectNode) proposal.path("reminderTypeObjects").path(1)).put("text", "IFO.Beslut");
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal);

		assertRefused(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request("2026-09-30")), BAD_REQUEST, LifecareReminderService.NO_MANUAL_REMINDER);
	}

	@Test
	void createRefusesWhenTheInsatsIsNotAmongTheAssociations() {
		givenErrand();
		final var proposal = proposal();
		((ObjectNode) proposal.path("reminderForAdd")).put("mainObjectId", "99");
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal);

		assertRefused(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request("2026-09-30")), BAD_REQUEST, LifecareReminderService.NO_MANUAL_REMINDER);
	}

	@Test
	void createRefusesWhenTheManualTypeIsInactive() {
		givenErrand();
		final var proposal = proposal();
		((ObjectNode) proposal.path("reminderTypeObjects").path(1).path("associations").path(0).path("reminderTypes").path(0)).put("isActive", false);
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal);

		assertRefused(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request("2026-09-30")), BAD_REQUEST, LifecareReminderService.NO_MANUAL_REMINDER);
	}

	@Test
	void createRefusesAPriorityOrStatusLifecareDoesNotOffer() {
		givenErrand();
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal());

		assertRefused(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request("2026-09-30").withPriority(9)), BAD_REQUEST,
			LifecareReminderService.UNKNOWN_CODES);
		assertRefused(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request("2026-09-30").withStatus(9)), BAD_REQUEST,
			LifecareReminderService.UNKNOWN_CODES);
	}

	@Test
	void createOnAProposalWithoutABlankBevakning() {
		givenErrand();
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(json("{}"));

		assertRefused(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request("2026-09-30")), BAD_GATEWAY,
			LifecareReminderService.UNEXPECTED_ANSWER.formatted(PATH_PROPOSAL));
	}

	@Test
	void updateSavesTheChangeAndLogsIt() {
		givenErrand();
		when(client.get(PATH_LIST, BY_SERVICE)).thenReturn(json(LIST));
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal());
		when(client.get(PATH_EDIT, Map.of("reminderId", "40", "readOptions", "false")))
			.thenReturn(json("{\"reminder\": " + CURRENT + "}"));

		service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 40, request("2026-09-23").withText("Hejsdfdsf"));

		verify(client).post(eq(PATH_UPDATE), eq(Map.of()), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().path("reminderId").intValue()).isEqualTo(40);
		assertThat(bodyCaptor.getValue().path("text").stringValue()).isEqualTo("Hejsdfdsf");
		assertThat(bodyCaptor.getValue().path("isDateDirty").booleanValue()).isFalse();
		verify(recorder).written(ERRAND, LifecareAccessEntry.UPDATE, "REMINDER", "Ändrade en bevakning i Lifecare", "40");
	}

	@Test
	void updateMarksAnOverdueBevakningDoneWithoutTouchingItsDate() {
		givenErrand();
		when(client.get(PATH_LIST, BY_SERVICE)).thenReturn(json(LIST));
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal());
		final var overdue = (ObjectNode) json(CURRENT);
		overdue.put("reminderDate", "2026-09-01");
		when(client.get(PATH_EDIT, Map.of("reminderId", "40", "readOptions", "false"))).thenReturn(JSON.createObjectNode().set("reminder", overdue));

		service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 40, request("2026-09-01").withStatus(2));

		verify(client).post(eq(PATH_UPDATE), eq(Map.of()), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().path("statusText").stringValue()).isEqualTo("Klar");
	}

	@Test
	void updateRefusesMovingTheDateIntoThePast() {
		givenErrand();
		when(client.get(PATH_LIST, BY_SERVICE)).thenReturn(json(LIST));
		when(client.get(PATH_PROPOSAL, BY_SERVICE)).thenReturn(proposal());
		when(client.get(PATH_EDIT, Map.of("reminderId", "40", "readOptions", "false")))
			.thenReturn(json("{\"reminder\": " + CURRENT + "}"));

		assertRefused(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 40, request("2026-09-01")), BAD_REQUEST, LifecareReminderService.PAST_DATE);
		verify(client, never()).post(anyString(), anyMap(), any());
	}

	@Test
	void updateRefusesABevakningNotOnTheInsats() {
		givenErrand();
		when(client.get(PATH_LIST, BY_SERVICE)).thenReturn(json(LIST));

		assertRefused(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 99, request("2026-09-30")), NOT_FOUND, LifecareReminderService.NOT_ON_INSATS);
		verify(client, never()).post(anyString(), anyMap(), any());
		verifyNoInteractions(recorder);
	}

	@Test
	void removeRemovesAndLogs() {
		givenErrand();
		when(client.get(PATH_LIST, BY_SERVICE)).thenReturn(json(LIST));

		service.remove(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 40);

		verify(client).delete(PATH_REMOVE, Map.of("id", 40));
		verify(recorder).written(ERRAND, LifecareAccessEntry.DELETE, "REMINDER", "Tog bort en bevakning i Lifecare", "40");
	}

	@Test
	void removeRefusesABevakningNotOnTheInsats() {
		givenErrand();
		when(client.get(PATH_LIST, BY_SERVICE)).thenReturn(json(LIST));

		assertRefused(() -> service.remove(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 99), NOT_FOUND, LifecareReminderService.NOT_ON_INSATS);
		verify(client, never()).delete(anyString(), any());
	}

	private static void assertRefused(final ThrowingCallable call, final HttpStatus status,
		final String detail) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
			assertThat(problem.getStatus()).isEqualTo(status);
			assertThat(problem.getDetail()).isEqualTo(detail);
		});
	}
}
