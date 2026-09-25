package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderOptions;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareReminderMapper.ReminderTarget;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.isTrue;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareReminderMapper.activeCodeText;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareReminderMapper.toReminderCreateBody;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareReminderMapper.toReminderOptions;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareReminderMapper.toReminderUpdateBody;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareReminderMapper.toReminders;

/**
 * The bevakningar on an errand's insats, read and written live in Lifecare, the register of record. careM keeps nothing
 * of them but the access-log rows.
 *
 * <p>
 * A bevakning made from here is always a manual one on the insats itself (IFO.Insats, Manuell bevakning insats), for
 * the applicant, bevakad av the insats's own caseworker. A change or removal is only made to a bevakning Lifecare lists
 * for this errand's insats, so an id from elsewhere is refused rather than written.
 * </p>
 */
@Service
public class LifecareReminderService {

	static final String PATH_LIST = "api2/Reminders/ListRemindersByServiceId";
	static final String PATH_PROPOSAL = "api2/Reminders/GetProposalForService";
	static final String PATH_EDIT = "api2/Reminders/GetReminderEditComposite";
	static final String PATH_CREATE = "api2/Reminders/CreateReminder/";
	static final String PATH_UPDATE = "api2/Reminders/UpdateReminder/";
	static final String PATH_REMOVE = "api2/Reminders/RemoveReminder/";

	static final String TARGET_REMINDERS = "REMINDERS";
	static final String TARGET_REMINDER = "REMINDER";

	static final String INSATS_OBJECT_TYPE = "IFO.Insats";
	static final String INSATS_REMINDER_TYPE = "Manuell bevakning insats";

	static final String PAST_DATE = "Bevakningsdatumet kan inte ligga bakåt i tiden.";
	static final String NO_MANUAL_REMINDER = "Lifecare tillåter ingen manuell bevakning på insatsen.";
	static final String NO_CASEWORKER = "Insatsen har ingen handläggare i Lifecare som kan bevaka.";
	static final String UNKNOWN_CODES = "Prioriteten eller statusen finns inte i Lifecare.";
	static final String NOT_ON_INSATS = "Bevakningen finns inte på insatsen i Lifecare";
	static final String UNEXPECTED_ANSWER = "Lifecare answered %s without the expected object";

	private static final ZoneId SWEDISH_TIME = ZoneId.of("Europe/Stockholm");

	private final ProfessionalWebClient client;
	private final LifecareErrandService errandService;
	private final LifecareAccessRecorder recorder;
	private final StakeholderService stakeholderService;
	private final Clock clock;

	@Autowired
	LifecareReminderService(final ProfessionalWebClient client, final LifecareErrandService errandService, final LifecareAccessRecorder recorder,
		final StakeholderService stakeholderService) {
		this(client, errandService, recorder, stakeholderService, Clock.system(SWEDISH_TIME));
	}

	LifecareReminderService(final ProfessionalWebClient client, final LifecareErrandService errandService, final LifecareAccessRecorder recorder,
		final StakeholderService stakeholderService, final Clock clock) {
		this.client = client;
		this.errandService = errandService;
		this.recorder = recorder;
		this.stakeholderService = stakeholderService;
		this.clock = clock;
	}

	/**
	 * The bevakningar on the errand's insats, and on the beslut and aktualiseringar under it, soonest first.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the bevakningar
	 */
	public List<LifecareReminder> list(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var reminders = toReminders(readList(errand.requireServiceId()));
		recorder.read(errand, TARGET_REMINDERS, "Läste bevakningar i Lifecare");
		return reminders;
	}

	/**
	 * The priorities and statuses a bevakning can have, and what Lifecare proposes for a new one.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the options
	 */
	public LifecareReminderOptions options(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		return toReminderOptions(readProposal(errand.requireServiceId()));
	}

	/**
	 * Creates a bevakning on the errand's insats in Lifecare, for the applicant. Not idempotent: a second call creates a
	 * second bevakning.
	 *
	 * @param municipalityId the municipality
	 * @param namespace      the namespace
	 * @param errandId       the errand
	 * @param request        the bevakning
	 */
	public void create(final String municipalityId, final String namespace, final String errandId, final LifecareReminderRequest request) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var serviceId = errand.requireServiceId();
		if (isPast(request.getReminderDate())) {
			throw Problem.valueOf(BAD_REQUEST, PAST_DATE);
		}
		final var proposal = readProposal(serviceId);
		final var blank = requireObject(proposal.path("reminderForAdd"), PATH_PROPOSAL);
		final var target = resolveTarget(proposal, blank);
		resolveCodeTexts(proposal.path("options"), request);

		final var body = toReminderCreateBody(blank, target, errandService.applicantPersonalNumber(errand), applicantName(errand), request);
		client.post(PATH_CREATE, Map.of(), body);
		recorder.written(errand, LifecareAccessEntry.CREATE, TARGET_REMINDER, "Lade till en bevakning i Lifecare", null);
	}

	/**
	 * Changes a bevakning on the errand's insats: its date, text, priority or status, which is also how one is marked
	 * done. A changed date may not be in the past; an unchanged one may, so an overdue bevakning can still be marked done.
	 *
	 * @param municipalityId the municipality
	 * @param namespace      the namespace
	 * @param errandId       the errand
	 * @param reminderId     the Lifecare reminder id
	 * @param request        the change
	 */
	public void update(final String municipalityId, final String namespace, final String errandId, final int reminderId,
		final LifecareReminderRequest request) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var serviceId = errand.requireServiceId();
		assertOnInsats(serviceId, reminderId);
		final var proposal = readProposal(serviceId);
		final var current = requireObject(client.get(PATH_EDIT, params("reminderId", String.valueOf(reminderId), "readOptions", "false")).path("reminder"),
			PATH_EDIT);

		final var dateChanged = !Objects.equals(text(current.path("reminderDate")), request.getReminderDate());
		if (dateChanged && isPast(request.getReminderDate())) {
			throw Problem.valueOf(BAD_REQUEST, PAST_DATE);
		}
		final var texts = resolveCodeTexts(proposal.path("options"), request);

		client.post(PATH_UPDATE, Map.of(), toReminderUpdateBody(current, request, dateChanged, texts.get(0), texts.get(1)));
		recorder.written(errand, LifecareAccessEntry.UPDATE, TARGET_REMINDER, "Ändrade en bevakning i Lifecare", String.valueOf(reminderId));
	}

	/**
	 * Removes a bevakning from the errand's insats in Lifecare.
	 *
	 * @param municipalityId the municipality
	 * @param namespace      the namespace
	 * @param errandId       the errand
	 * @param reminderId     the Lifecare reminder id
	 */
	public void remove(final String municipalityId, final String namespace, final String errandId, final int reminderId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		assertOnInsats(errand.requireServiceId(), reminderId);
		client.delete(PATH_REMOVE, Map.of("id", reminderId));
		recorder.written(errand, LifecareAccessEntry.DELETE, TARGET_REMINDER, "Tog bort en bevakning i Lifecare", String.valueOf(reminderId));
	}

	private JsonNode readList(final int serviceId) {
		return client.get(PATH_LIST, Map.of("id", String.valueOf(serviceId)));
	}

	private JsonNode readProposal(final int serviceId) {
		return client.get(PATH_PROPOSAL, Map.of("id", String.valueOf(serviceId)));
	}

	private void assertOnInsats(final int serviceId, final int reminderId) {
		final var onInsats = readList(serviceId).path("reminders").valueStream()
			.anyMatch(reminder -> Integer.valueOf(reminderId).equals(integer(reminder.path("reminderId"))));
		if (!onInsats) {
			throw Problem.valueOf(NOT_FOUND, NOT_ON_INSATS);
		}
	}

	/**
	 * The insats the proposal's blank bevakning is bound to, its manual insats bevakning type and its caseworker.
	 */
	private static ReminderTarget resolveTarget(final JsonNode proposal, final ObjectNode blank) {
		final var objectTypeId = integer(blank.path("mainObjectType"));
		final var objectId = integer(blank.path("mainObjectId"));
		final var objectType = proposal.path("reminderTypeObjects").valueStream()
			.filter(candidate -> objectTypeId != null && objectTypeId.equals(integer(candidate.path("id"))))
			.filter(candidate -> INSATS_OBJECT_TYPE.equals(text(candidate.path("text"))))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, NO_MANUAL_REMINDER));
		final var insats = objectType.path("associations").valueStream()
			.filter(candidate -> objectId != null && objectId.equals(integer(candidate.path("key"))))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, NO_MANUAL_REMINDER));
		final var reminderType = insats.path("reminderTypes").valueStream()
			.filter(candidate -> isTrue(candidate.path("isActive")))
			.filter(candidate -> integer(candidate.path("id")) != null)
			.filter(candidate -> INSATS_REMINDER_TYPE.equals(text(candidate.path("text"))))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, NO_MANUAL_REMINDER));
		final var caseworkerId = Optional.ofNullable(text(insats.path("caseworkerId")))
			.filter(StringUtils::hasText)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, NO_CASEWORKER));
		final var caseworkerName = proposal.path("options").path("reminderCaseworkers").valueStream()
			.filter(candidate -> caseworkerId.equals(text(candidate.path("id"))))
			.map(candidate -> text(candidate.path("name")))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(caseworkerId);

		return new ReminderTarget(objectTypeId, text(objectType.path("text")), objectId, caseworkerId, caseworkerName,
			integer(reminderType.path("id")), text(reminderType.path("text")));
	}

	/**
	 * The texts of the priority and status picked, in that order, or a 400 when either is not an active code in Lifecare.
	 */
	private static List<String> resolveCodeTexts(final JsonNode options, final LifecareReminderRequest request) {
		final var priority = activeCodeText(options.path("reminderPriorityTypes"), request.getPriority());
		final var status = activeCodeText(options.path("reminderStatusTypes"), request.getStatus());
		if (priority.isEmpty() || status.isEmpty()) {
			throw Problem.valueOf(BAD_REQUEST, UNKNOWN_CODES);
		}
		return List.of(priority.get(), status.get());
	}

	/**
	 * The applicant as Lifecare names a person on a bevakning: Efternamn, Förnamn.
	 */
	private String applicantName(final LifecareErrand errand) {
		return stakeholderService.readAll(errand.municipalityId(), errand.namespace(), errand.errandId()).stream()
			.filter(stakeholder -> ROLE_APPLICANT.equals(stakeholder.getRole()))
			.findFirst()
			.map(LifecareReminderService::lifecareName)
			.orElse("");
	}

	private static String lifecareName(final Stakeholder stakeholder) {
		return Stream.of(stakeholder.getLastName(), stakeholder.getFirstName())
			.filter(StringUtils::hasText)
			.collect(Collectors.joining(", "));
	}

	private boolean isPast(final String date) {
		// Both are YYYY-MM-DD, so they compare as strings.
		return date.compareTo(LocalDate.now(clock).toString()) < 0;
	}

	private static ObjectNode requireObject(final JsonNode node, final String path) {
		if (node instanceof final ObjectNode object) {
			return object;
		}
		throw Problem.valueOf(BAD_GATEWAY, UNEXPECTED_ANSWER.formatted(path));
	}

	private static Map<String, String> params(final String firstKey, final String firstValue, final String secondKey, final String secondValue) {
		final var params = new LinkedHashMap<String, String>();
		params.put(firstKey, firstValue);
		params.put(secondKey, secondValue);
		return params;
	}
}
