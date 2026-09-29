package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.List;
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
 * Maps Lifecare's jobbstimulans answer onto careM's periods, and builds the SaveJobStimulus body that adds one.
 */
final class LifecareJobStimulusMapper {

	static final String APPLICANT = "APPLICANT";
	static final String CO_APPLICANT = "CO_APPLICANT";
	static final String CO_APPLICANT_REFUSAL = "Hushållet har en medsökande. Jobbstimulans kan inte ändras från Drakel för sådana hushåll ännu. Gör det direkt i Lifecare.";

	private static final String FIELD_APPLICANT = "applicant";
	private static final String FIELD_CO_APPLICANT = "coApplicant";
	private static final String FIELD_PERIODS = "periods";
	private static final String FIELD_MIN_DATE = "minDate";
	private static final String FIELD_TO_DATE = "toDate";
	private static final String FIELD_PERSON_ID = "personId";
	private static final String FIELD_PERSON_ID_FORMATTED = "personIdFormatted";

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
	 * Builds the SaveJobStimulus body that adds a period for the sökande, the way Lifecare's web app does (capture
	 * 2026-09-24).
	 *
	 * <p>
	 * The endpoint replaces the person's whole set of periods, so every period Lifecare holds goes back in its order with
	 * the two fields the web app adds: isValid, and minDate, the end of the period before it (0 for the first). The new
	 * one goes last with only what the web app gives a new period; Lifecare numbers it. A household with a medsökande is
	 * refused (422): how the web app sends the medsökandes periods back is not captured, and getting it wrong would
	 * delete them.
	 * </p>
	 *
	 * @param  current  Lifecare's GetJobStimulusForService answer
	 * @param  fromDate the new period's start, yyyy-MM-dd
	 * @param  toDate   the new period's end, yyyy-MM-dd
	 * @return          the body to post
	 */
	static ObjectNode buildJobStimulusAdd(final JsonNode current, final String fromDate, final String toDate) {
		final var applicant = field(current, FIELD_APPLICANT);
		if (applicant == null || !applicant.isObject()) {
			throw refuse("Sökande finns inte på insatsen i Lifecare.");
		}
		if (flag(current, "hasCoApplicant") || field(current, FIELD_CO_APPLICANT) != null) {
			throw refuse(CO_APPLICANT_REFUSAL);
		}

		final var existing = array(applicant, FIELD_PERIODS);
		final var periods = NODES.arrayNode();
		for (var index = 0; index < existing.size(); index++) {
			final var period = copyOf(existing.get(index));
			period.put("isValid", true);
			if (index == 0) {
				period.put(FIELD_MIN_DATE, 0);
			} else {
				final var previousEnd = field(existing.get(index - 1), FIELD_TO_DATE);
				if (previousEnd == null) {
					period.put(FIELD_MIN_DATE, 0);
				} else {
					period.set(FIELD_MIN_DATE, previousEnd.deepCopy());
				}
			}
			periods.add(period);
		}
		final var added = NODES.objectNode();
		copyField(applicant, added, FIELD_PERSON_ID);
		copyField(applicant, added, FIELD_PERSON_ID_FORMATTED);
		added.put("fromDate", fromDate);
		added.put(FIELD_TO_DATE, toDate);
		added.put("markedForRemoval", false);
		periods.add(added);

		final var applicantBody = NODES.objectNode();
		applicantBody.set(FIELD_PERIODS, periods);
		copyField(applicant, applicantBody, FIELD_PERSON_ID);
		copyField(applicant, applicantBody, "name");
		copyField(applicant, applicantBody, FIELD_PERSON_ID_FORMATTED);

		final var noCoApplicant = NODES.objectNode();
		noCoApplicant.put(FIELD_PERSON_ID, "");
		noCoApplicant.put(FIELD_PERSON_ID_FORMATTED, "");
		noCoApplicant.put("name", "");
		noCoApplicant.set(FIELD_PERIODS, NODES.arrayNode());

		final var body = NODES.objectNode();
		body.set(FIELD_APPLICANT, applicantBody);
		body.set(FIELD_CO_APPLICANT, noCoApplicant);
		return body;
	}

	private static Stream<LifecareJobStimulusPeriod> periods(final JsonNode person, final String role) {
		return array(person, FIELD_PERIODS).stream()
			.filter(period -> !flag(period, "markedForRemoval"))
			.map(period -> new LifecareJobStimulusPeriod(integer(period, "jobStimulusId"), role, emptyToNull(text(period, "fromDate")), emptyToNull(text(period, FIELD_TO_DATE))));
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
