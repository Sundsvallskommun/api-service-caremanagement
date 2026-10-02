package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriod;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.NODES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.array;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.copyOf;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.field;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.flag;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.refuse;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.text;

/**
 * Maps Lifecare's jobbstimulans answer onto careM's periods, and builds the SaveJobStimulus bodies that add or remove
 * one, for the sökande or the medsökande.
 */
final class LifecareJobStimulusMapper {

	static final String APPLICANT = "APPLICANT";
	static final String CO_APPLICANT = "CO_APPLICANT";
	static final String NO_CO_APPLICANT_REFUSAL = "Insatsen har ingen medsökande i Lifecare.";

	private static final String FIELD_APPLICANT = "applicant";
	private static final String FIELD_CO_APPLICANT = "coApplicant";
	private static final String FIELD_PERIODS = "periods";
	private static final String FIELD_MIN_DATE = "minDate";
	private static final String FIELD_TO_DATE = "toDate";
	private static final String FIELD_PERSON_ID = "personId";
	private static final String FIELD_PERSON_ID_FORMATTED = "personIdFormatted";
	private static final String FIELD_JOB_STIMULUS_ID = "jobStimulusId";
	private static final String FIELD_MARKED_FOR_REMOVAL = "markedForRemoval";

	private LifecareJobStimulusMapper() {}

	/**
	 * The sökandes and the medsökandes periods, each marked with whose it is. Periods marked for removal are left out,
	 * and so is the personnummer.
	 *
	 * @param  raw Lifecare's GetJobStimulusForService (or SaveJobStimulus) answer
	 * @return     the periods
	 */
	static List<LifecareJobStimulusPeriod> toJobStimulusPeriods(final JsonNode raw) {
		return Stream.concat(periods(field(raw, FIELD_APPLICANT), APPLICANT), periods(field(raw, FIELD_CO_APPLICANT), CO_APPLICANT)).toList();
	}

	/**
	 * Builds the SaveJobStimulus body that adds a period for the sökande or the medsökande, the way Lifecare's web app does
	 * (captures 2026-09-24 and, for a household with a medsökande, 2026-09-30).
	 *
	 * <p>
	 * The endpoint replaces both persons' whole sets of periods, so every period Lifecare holds goes back in its order,
	 * the untouched person's too, with the two fields the web app adds: isValid, and minDate, the end of the person's
	 * period before it (0 for the first). The new one goes last among its person's with only what the web app gives a new
	 * period; Lifecare numbers it. Without a medsökande the web app sends an empty one.
	 * </p>
	 *
	 * @param  current  Lifecare's GetJobStimulusForService answer
	 * @param  role     whose period it is, APPLICANT or CO_APPLICANT
	 * @param  fromDate the new period's start, yyyy-MM-dd
	 * @param  toDate   the new period's end, yyyy-MM-dd
	 * @return          the body to post
	 */
	static ObjectNode buildJobStimulusAdd(final JsonNode current, final String role, final String fromDate, final String toDate) {
		final var applicant = applicantOf(current);
		final var coApplicant = coApplicantOf(current);
		final var forCoApplicant = CO_APPLICANT.equals(role);
		final JsonNode person;
		if (forCoApplicant) {
			person = coApplicant.orElseThrow(() -> refuse(NO_CO_APPLICANT_REFUSAL));
		} else {
			person = applicant;
		}

		final var added = NODES.objectNode();
		copyField(person, added, FIELD_PERSON_ID);
		copyField(person, added, FIELD_PERSON_ID_FORMATTED);
		added.put("fromDate", fromDate);
		added.put(FIELD_TO_DATE, toDate);
		added.put(FIELD_MARKED_FOR_REMOVAL, false);

		return saveBody(personBody(applicant, keep -> addedTo(keep, added, !forCoApplicant)),
			coApplicant.map(found -> personBody(found, keep -> addedTo(keep, added, forCoApplicant))).orElseGet(LifecareJobStimulusMapper::noCoApplicant));
	}

	/**
	 * Builds the SaveJobStimulus body that removes one of the sökandes or the medsökandes periods, the way Lifecare's web
	 * app does (capture 2026-09-30).
	 *
	 * <p>
	 * Lifecare has no call of its own for removing: the web app leaves the period out of its person's set and saves both
	 * persons' sets, with isValid and minDate counted again over what remains. It never sets markedForRemoval. Lifecare
	 * gives every period that is saved a new jobStimulusId.
	 * </p>
	 *
	 * @param  current       Lifecare's GetJobStimulusForService answer
	 * @param  jobStimulusId the period to remove
	 * @return               the body to post
	 */
	static ObjectNode buildJobStimulusRemove(final JsonNode current, final int jobStimulusId) {
		final var applicant = applicantOf(current);
		final UnaryOperator<List<JsonNode>> without = periods -> periods.stream()
			.filter(period -> !Objects.equals(integer(period, FIELD_JOB_STIMULUS_ID), jobStimulusId))
			.toList();

		return saveBody(personBody(applicant, without), coApplicantOf(current).map(coApplicant -> personBody(coApplicant, without)).orElseGet(LifecareJobStimulusMapper::noCoApplicant));
	}

	/** The sökande on the insats, refusing an insats without one. */
	private static JsonNode applicantOf(final JsonNode current) {
		final var applicant = field(current, FIELD_APPLICANT);
		if (applicant == null || !applicant.isObject()) {
			throw refuse("Sökande finns inte på insatsen i Lifecare.");
		}
		return applicant;
	}

	/** The medsökande on the insats: an object with its periods, null (or absent) without one. */
	private static Optional<JsonNode> coApplicantOf(final JsonNode current) {
		return Optional.ofNullable(field(current, FIELD_CO_APPLICANT)).filter(JsonNode::isObject);
	}

	private static List<JsonNode> addedTo(final List<JsonNode> periods, final ObjectNode added, final boolean theirs) {
		if (!theirs) {
			return periods;
		}
		final var withAdded = new ArrayList<>(periods);
		withAdded.add(added);
		return withAdded;
	}

	/**
	 * A person's part of the body: their periods as the web app sends them back, each with isValid and minDate (the end
	 * of the period before it, 0 for the first), and a new period as it is; then personId, name and personIdFormatted.
	 */
	private static ObjectNode personBody(final JsonNode person, final UnaryOperator<List<JsonNode>> change) {
		final var existing = array(person, FIELD_PERIODS);
		final var kept = change.apply(existing);
		final var periods = NODES.arrayNode();
		for (var index = 0; index < kept.size(); index++) {
			final var original = kept.get(index);
			if (!existing.contains(original)) {
				periods.add(original.deepCopy());
			} else {
				final var period = copyOf(original);
				period.put("isValid", true);
				final var previousEnd = previousEnd(kept, index);
				if (previousEnd == null) {
					period.put(FIELD_MIN_DATE, 0);
				} else {
					period.set(FIELD_MIN_DATE, previousEnd.deepCopy());
				}
				periods.add(period);
			}
		}
		final var body = NODES.objectNode();
		body.set(FIELD_PERIODS, periods);
		copyField(person, body, FIELD_PERSON_ID);
		copyField(person, body, "name");
		copyField(person, body, FIELD_PERSON_ID_FORMATTED);
		return body;
	}

	/** The end of the period before the one at index, null for the first or after a period without end. */
	private static JsonNode previousEnd(final List<JsonNode> periods, final int index) {
		if (index == 0) {
			return null;
		}
		return field(periods.get(index - 1), FIELD_TO_DATE);
	}

	/** Both persons' parts, which is the whole body SaveJobStimulus takes. */
	private static ObjectNode saveBody(final ObjectNode applicantBody, final ObjectNode coApplicantBody) {
		final var body = NODES.objectNode();
		body.set(FIELD_APPLICANT, applicantBody);
		body.set(FIELD_CO_APPLICANT, coApplicantBody);
		return body;
	}

	/** The empty medsökande the web app sends for a household without one. */
	private static ObjectNode noCoApplicant() {
		final var noCoApplicant = NODES.objectNode();
		noCoApplicant.put(FIELD_PERSON_ID, "");
		noCoApplicant.put(FIELD_PERSON_ID_FORMATTED, "");
		noCoApplicant.put("name", "");
		noCoApplicant.set(FIELD_PERIODS, NODES.arrayNode());
		return noCoApplicant;
	}

	private static Stream<LifecareJobStimulusPeriod> periods(final JsonNode person, final String role) {
		return array(person, FIELD_PERIODS).stream()
			.filter(period -> !flag(period, FIELD_MARKED_FOR_REMOVAL))
			.map(period -> new LifecareJobStimulusPeriod(integer(period, FIELD_JOB_STIMULUS_ID), role, emptyToNull(text(period, "fromDate")), emptyToNull(text(period, FIELD_TO_DATE))));
	}

	/** Lifecare sends an empty string for a period without an end. */
	private static String emptyToNull(final String value) {
		if (StringUtils.hasLength(value)) {
			return value;
		}
		return null;
	}

	private static void copyField(final JsonNode source, final ObjectNode target, final String name) {
		final var value = source.get(name);
		if (value != null) {
			target.set(name, value.deepCopy());
		}
	}
}
