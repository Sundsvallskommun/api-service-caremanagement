package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderChoice;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderOptions;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static java.util.Comparator.nullsLast;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.textOrEmpty;

/**
 * Maps Lifecare's bevakning answers (Reminders/ListRemindersByServiceId, Reminders/GetProposalForService and
 * Reminders/GetReminderEditComposite) onto careM's API models, and builds the bodies Lifecare's CreateReminder and
 * UpdateReminder take back.
 *
 * <p>
 * The write bodies are always the object Lifecare handed out with a few fields set, in Lifecare's own field order, so
 * nothing Lifecare's editor sends is lost on the way back.
 * </p>
 */
public final class LifecareReminderMapper {

	private LifecareReminderMapper() {}

	/**
	 * What a new bevakning hangs on and who it is bevakad av, as resolved from Lifecare's proposal.
	 *
	 * @param objectType     the object type id, e.g. 7083 for IFO.Insats
	 * @param objectTypeName the object type name
	 * @param objectId       the insats id
	 * @param caseworkerId   the insats's caseworker
	 * @param caseworkerName the caseworker's name, the id when Lifecare gives no name
	 * @param type           the bevakning type id
	 * @param typeText       the bevakning type name
	 */
	public record ReminderTarget(int objectType, String objectTypeName, int objectId, String caseworkerId, String caseworkerName, int type,
		String typeText) {
	}

	/**
	 * The bevakningar, soonest first, without the personnummer Lifecare puts on every row.
	 *
	 * @param  list Lifecare's ListRemindersByServiceId answer
	 * @return      the bevakningar
	 */
	public static List<LifecareReminder> toReminders(final JsonNode list) {
		return list.path("reminders").valueStream()
			.map(LifecareReminderMapper::toReminder)
			.sorted(Comparator.comparing(LifecareReminder::getDate, nullsLast(Comparator.naturalOrder())))
			.toList();
	}

	static LifecareReminder toReminder(final JsonNode reminder) {
		return LifecareReminder.create()
			.withId(integer(reminder.path("reminderId")))
			.withDate(text(reminder.path("reminderDate")))
			.withStatus(textOrEmpty(reminder.path("statusText")))
			.withStatusCode(integer(reminder.path("status")))
			.withPriority(textOrEmpty(reminder.path("priorityText")))
			.withPriorityCode(integer(reminder.path("priority")))
			.withType(textOrEmpty(reminder.path("typeText")))
			.withObjectType(textOrEmpty(reminder.path("objectTypeName")))
			.withText(textOrEmpty(reminder.path("text")))
			.withCaseworker(Optional.ofNullable(text(reminder.path("caseworkerName")))
				.or(() -> Optional.ofNullable(text(reminder.path("caseworkerId"))))
				.orElse(""))
			.withCaseworkerId(textOrEmpty(reminder.path("caseworkerId")));
	}

	/**
	 * The form's choices out of Lifecare's proposal: the active priorities and statuses, and the ones Lifecare proposes
	 * for a new bevakning.
	 *
	 * @param  proposal Lifecare's GetProposalForService answer
	 * @return          the options
	 */
	public static LifecareReminderOptions toReminderOptions(final JsonNode proposal) {
		final var options = proposal.path("options");
		final var blank = proposal.path("reminderForAdd");
		return LifecareReminderOptions.create()
			.withPriorities(toChoices(options.path("reminderPriorityTypes")))
			.withStatuses(toChoices(options.path("reminderStatusTypes")))
			.withDefaultPriority(integer(blank.path("priority")))
			.withDefaultStatus(integer(blank.path("status")));
	}

	private static List<LifecareReminderChoice> toChoices(final JsonNode codes) {
		return codes.valueStream()
			.filter(code -> code.path("isActive").asBoolean(false))
			.map(code -> LifecareReminderChoice.create()
				.withCode(integer(code.path("code")))
				.withText(text(code.path("text"))))
			.toList();
	}

	/**
	 * The active code with the given value in one of Lifecare's code lists (priority or status).
	 *
	 * @param  codes the code list
	 * @param  code  the code picked
	 * @return       the code's text, empty when the code is not in the list or not active
	 */
	public static Optional<String> activeCodeText(final JsonNode codes, final Integer code) {
		return codes.valueStream()
			.filter(candidate -> candidate.path("isActive").asBoolean(false))
			.filter(candidate -> code != null && code.equals(integer(candidate.path("code"))))
			.findFirst()
			.map(candidate -> textOrEmpty(candidate.path("text")));
	}

	/**
	 * The CreateReminder body, as Lifecare's web app builds it: the proposal's blank bevakning, filled in, in its own field
	 * order.
	 *
	 * @param  blank      the proposal's reminderForAdd
	 * @param  target     what the bevakning hangs on and who it is bevakad av
	 * @param  personId   the applicant's personnummer
	 * @param  personName the applicant as Lifecare names a person (Efternamn, Förnamn)
	 * @param  request    what the caseworker filled in
	 * @return            the body to post
	 */
	public static ObjectNode toReminderCreateBody(final ObjectNode blank, final ReminderTarget target, final String personId, final String personName,
		final LifecareReminderRequest request) {
		final var body = blank.deepCopy();
		body.put("objectType", target.objectType());
		body.put("objectTypeName", target.objectTypeName());
		body.put("objectId", target.objectId());
		body.put("personId", personId);
		body.put("personName", personName);
		body.put("caseworkerId", target.caseworkerId());
		body.put("caseworkerName", target.caseworkerName());
		body.put("reminderDate", request.getReminderDate());
		body.put("status", request.getStatus());
		body.put("priority", request.getPriority());
		body.put("type", target.type());
		body.put("typeText", target.typeText());
		body.put("text", request.getText());
		return body;
	}

	/**
	 * The UpdateReminder body, as Lifecare's web app builds it: five editor fields first (taken from the bevakning when it
	 * carries them), then the bevakning as its editor opened it, with the caseworker's changes. Who it is bevakad av is
	 * left as it is.
	 *
	 * @param  current      the bevakning from GetReminderEditComposite
	 * @param  request      the change
	 * @param  dateChanged  whether the date was changed (isDateDirty)
	 * @param  priorityText the text of the priority picked
	 * @param  statusText   the text of the status picked
	 * @return              the body to post
	 */
	public static ObjectNode toReminderUpdateBody(final ObjectNode current, final LifecareReminderRequest request, final boolean dateChanged,
		final String priorityText, final String statusText) {
		final var body = JsonNodeFactory.instance.objectNode();
		body.put("flowable", true);
		body.put("isRecurring", false);
		body.put("recurringDays", 0);
		body.put("coCaseworkerId", "");
		body.put("isDateDirty", dateChanged);
		body.setAll(current.deepCopy());
		body.put("isDateDirty", dateChanged);
		body.put("reminderDate", request.getReminderDate());
		body.put("status", request.getStatus());
		body.put("statusText", statusText);
		body.put("priority", request.getPriority());
		body.put("priorityText", priorityText);
		body.put("text", request.getText());
		return body;
	}
}
