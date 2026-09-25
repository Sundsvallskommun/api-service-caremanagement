package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderChoice;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareReminderMapper.ReminderTarget;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

class LifecareReminderMapperTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	static JsonNode json(final String text) {
		return JSON.readTree(text);
	}

	@Test
	void toReminderCreateBodyFillsTheBlankTheWayTheWebAppDoes() {
		final var blank = (ObjectNode) json(LifecareReminderFixtures.PROPOSAL).path("reminderForAdd");
		final var target = new ReminderTarget(7083, "IFO.Insats", 2, "TEST", "Test Handläggare", 3, "Manuell bevakning insats");
		final var request = LifecareReminderRequest.create().withReminderDate("2026-09-30").withText("Kontrollera hyran").withPriority(2).withStatus(3);

		final var body = LifecareReminderMapper.toReminderCreateBody(blank, target, "199001122390", "Jeppson, Test", request);

		// Same fields, order and null/empty mix as the captured CreateReminder body, on the insats (7083/2).
		final var expected = """
			{"reminderId":0,"receiverType":1,"objectType":7083,"objectTypeName":"IFO.Insats","objectId":2,"objectPropertyId":0,\
			"mainObjectType":7083,"mainObjectId":"2","personId":"199001122390","personName":"Jeppson, Test","caseworkerId":"TEST",\
			"caseworkerName":"Test Handläggare","reminderDate":"2026-09-30","status":3,"statusText":null,"priority":2,"priorityText":null,\
			"type":3,"typeText":"Manuell bevakning insats","text":"Kontrollera hyran","startComponent1":0,"startComponent2":0,\
			"updateTimestamp":"","updateSignature":null,"objectId2":null,"info":null,"customerId":0,"personIdFormatted":""}""";
		assertThat(body.toString()).isEqualTo(expected);
		// The proposal itself is left as it was.
		assertThat(blank.path("objectId").isNull()).isTrue();
	}

	@Test
	void toReminderUpdateBodyGivesTheCapturedBody() {
		final var current = (ObjectNode) json(LifecareReminderFixtures.CURRENT);
		final var request = LifecareReminderRequest.create().withReminderDate("2026-09-23").withText("Hejsdfdsf").withPriority(2).withStatus(3);

		final var body = LifecareReminderMapper.toReminderUpdateBody(current, request, false, "Normal", "Ej påbörjad");

		// POST Reminders/UpdateReminder/ (capture 2026-09-23), field for field and in order.
		final var expected = JSON.createObjectNode();
		expected.put("flowable", true);
		expected.put("isRecurring", false);
		expected.put("recurringDays", 0);
		expected.put("coCaseworkerId", "");
		expected.put("isDateDirty", false);
		expected.setAll(current.deepCopy());
		expected.put("text", "Hejsdfdsf");
		assertThat(body.toString()).isEqualTo(expected.toString());
	}

	@Test
	void toReminderUpdateBodyMarksTheDateDirtyAndSetsTheCodeTexts() {
		final var request = LifecareReminderRequest.create().withReminderDate("2026-10-01").withText("Hej").withPriority(2).withStatus(2);

		final var body = LifecareReminderMapper.toReminderUpdateBody((ObjectNode) json(LifecareReminderFixtures.CURRENT), request, true, "Normal", "Klar");

		assertThat(body.path("isDateDirty").booleanValue()).isTrue();
		assertThat(body.path("reminderDate").stringValue()).isEqualTo("2026-10-01");
		assertThat(body.path("status").intValue()).isEqualTo(2);
		assertThat(body.path("statusText").stringValue()).isEqualTo("Klar");
	}

	@Test
	void toReminderUpdateBodyKeepsEditorFieldsTheBevakningCarries() {
		final var current = (ObjectNode) json(LifecareReminderFixtures.CURRENT);
		current.put("flowable", false);
		current.put("recurringDays", 7);
		final var request = LifecareReminderRequest.create().withReminderDate("2026-09-23").withText("Hej").withPriority(2).withStatus(3);

		final var body = LifecareReminderMapper.toReminderUpdateBody(current, request, false, "Normal", "Ej påbörjad");

		assertThat(body.path("flowable").booleanValue()).isFalse();
		assertThat(body.path("recurringDays").intValue()).isEqualTo(7);
	}

	@Test
	void toRemindersListsSoonestFirstWithoutThePersonnummer() {
		final var list = json("""
			{ "reminders": [
			  { "reminderId": 39, "reminderDate": "2026-10-01", "status": 3, "statusText": "Ej påbörjad", "priority": 2, "priorityText": "Normal",
			    "personId": "199001122390", "personName": "Jeppson, Test", "caseworkerId": "TEST", "caseworkerName": null,
			    "type": 2, "typeText": null, "text": null, "objectType": 7012, "objectTypeName": null },
			  { "reminderId": 38, "reminderDate": "2026-09-23", "status": 3, "statusText": "Ej påbörjad", "priority": 2, "priorityText": "Normal",
			    "personId": "199001122390", "personName": "Jeppson, Test", "caseworkerId": "TEST", "caseworkerName": "Test Handläggare",
			    "type": 2, "typeText": "Manuell bevakning beslut", "text": "test av text", "objectType": 7012, "objectTypeName": "IFO.Beslut" } ] }
			""");

		final var reminders = LifecareReminderMapper.toReminders(list);

		assertThat(reminders).extracting(LifecareReminder::getId).containsExactly(38, 39);
		assertThat(reminders.getFirst()).isEqualTo(LifecareReminder.create()
			.withId(38)
			.withDate("2026-09-23")
			.withStatus("Ej påbörjad")
			.withStatusCode(3)
			.withPriority("Normal")
			.withPriorityCode(2)
			.withType("Manuell bevakning beslut")
			.withObjectType("IFO.Beslut")
			.withText("test av text")
			.withCaseworker("Test Handläggare")
			.withCaseworkerId("TEST"));
		// Nulls become empty strings, and the caseworker falls back to the id.
		assertThat(reminders.get(1)).satisfies(reminder -> {
			assertThat(reminder.getType()).isEmpty();
			assertThat(reminder.getText()).isEmpty();
			assertThat(reminder.getObjectType()).isEmpty();
			assertThat(reminder.getCaseworker()).isEqualTo("TEST");
		});
		assertThat(reminders.toString()).doesNotContain("199001122390");
	}

	@Test
	void toRemindersOnAnEmptyAnswer() {
		assertThat(LifecareReminderMapper.toReminders(MissingNode.getInstance())).isEmpty();
	}

	@Test
	void toReminderOptionsOffersTheActiveCodesAndProposesTheDefaults() {
		final var options = LifecareReminderMapper.toReminderOptions(json(LifecareReminderFixtures.PROPOSAL));

		assertThat(options.getPriorities()).extracting(LifecareReminderChoice::getText).containsExactly("Hög", "Normal");
		assertThat(options.getStatuses()).extracting(LifecareReminderChoice::getCode).containsExactly(2, 3);
		assertThat(options.getDefaultPriority()).isEqualTo(2);
		assertThat(options.getDefaultStatus()).isEqualTo(3);
	}

	@Test
	void activeCodeText() {
		final var priorities = json(LifecareReminderFixtures.PROPOSAL).path("options").path("reminderPriorityTypes");

		assertThat(LifecareReminderMapper.activeCodeText(priorities, 1)).contains("Hög");
		assertThat(LifecareReminderMapper.activeCodeText(priorities, 5)).isEmpty();
		assertThat(LifecareReminderMapper.activeCodeText(priorities, 9)).isEmpty();
		assertThat(LifecareReminderMapper.activeCodeText(priorities, null)).isEmpty();
	}
}
