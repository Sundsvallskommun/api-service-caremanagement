package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriod;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.buildJobStimulusAdd;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.toJobStimulusPeriods;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.serialised;

class LifecareJobStimulusMapperTest {

	/** Calculation/GetJobStimulusForService?businessType=8&amp;businessId=1 (capture 2026-09-24). */
	static final String CURRENT = """
		{
		  "applicant": {
		    "periods": [
		      { "jobStimulusId": 101, "personId": "19880209T050", "fromDate": "2021-01-01", "toDate": "2021-12-31", "updateTimestamp": "2026-08-21",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "880209-T050" },
		      { "jobStimulusId": 102, "personId": "19880209T050", "fromDate": "2022-07-08", "toDate": "2023-07-07", "updateTimestamp": "2026-08-21",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "880209-T050" },
		      { "jobStimulusId": 103, "personId": "19880209T050", "fromDate": "2026-01-01", "toDate": "2027-12-31", "updateTimestamp": "2026-08-21",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "880209-T050" }
		    ],
		    "personId": "19880209T050", "name": "Testsson, Test", "personIdFormatted": "880209-T050"
		  },
		  "coApplicant": null,
		  "hasCoApplicant": false
		}
		""";

	/** Calculation/SaveJobStimulus (capture 2026-09-24), field for field and in order. */
	private static final String CAPTURE = """
		{
		  "applicant": {
		    "periods": [
		      { "jobStimulusId": 101, "personId": "19880209T050", "fromDate": "2021-01-01", "toDate": "2021-12-31", "updateTimestamp": "2026-08-21",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "880209-T050", "isValid": true, "minDate": 0 },
		      { "jobStimulusId": 102, "personId": "19880209T050", "fromDate": "2022-07-08", "toDate": "2023-07-07", "updateTimestamp": "2026-08-21",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "880209-T050", "isValid": true, "minDate": "2021-12-31" },
		      { "jobStimulusId": 103, "personId": "19880209T050", "fromDate": "2026-01-01", "toDate": "2027-12-31", "updateTimestamp": "2026-08-21",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "880209-T050", "isValid": true, "minDate": "2023-07-07" },
		      { "personId": "19880209T050", "personIdFormatted": "880209-T050", "fromDate": "2028-01-15", "toDate": "2030-01-14", "markedForRemoval": false }
		    ],
		    "personId": "19880209T050", "name": "Testsson, Test", "personIdFormatted": "880209-T050"
		  },
		  "coApplicant": { "personId": "", "personIdFormatted": "", "name": "", "periods": [] }
		}
		""";

	private static ObjectNode current() {
		return (ObjectNode) json(CURRENT);
	}

	private static void assertRefused(final JsonNode current, final String reason) {
		assertThatThrownBy(() -> buildJobStimulusAdd(current, "2028-01-15", "2030-01-14"))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus()).isEqualTo(UNPROCESSABLE_CONTENT);
				assertThat(problem.getDetail()).contains(reason);
			});
	}

	@Test
	void turnsTheReadPeriodsAndANewOneIntoExactlyTheCapturedSaveBody() {
		assertThat(serialised(buildJobStimulusAdd(current(), "2028-01-15", "2030-01-14"))).isEqualTo(serialised(json(CAPTURE)));
	}

	@Test
	void addsTheFirstPeriodOfSomeoneWhoHasNone() {
		final var none = current();
		((ObjectNode) none.get("applicant")).putArray("periods");

		final var body = buildJobStimulusAdd(none, "2026-10-01", "2028-09-30");

		assertThat(serialised(body.get("applicant").get("periods"))).isEqualTo(
			"[{\"personId\":\"19880209T050\",\"personIdFormatted\":\"880209-T050\",\"fromDate\":\"2026-10-01\",\"toDate\":\"2028-09-30\",\"markedForRemoval\":false}]");
	}

	@Test
	void givesMinDateZeroAfterAPeriodWithoutEnd() {
		final var openEnded = current();
		((ObjectNode) openEnded.get("applicant").get("periods").get(0)).putNull("toDate");

		final var body = buildJobStimulusAdd(openEnded, "2028-01-15", "2030-01-14");

		assertThat(body.get("applicant").get("periods").get(1).get("minDate").asInt()).isZero();
	}

	@Test
	void refusesAHouseholdWithAMedsokandeWhosePeriodsWouldOtherwiseBeLost() {
		final var flagged = current().put("hasCoApplicant", true);
		final var withCoApplicant = current();
		withCoApplicant.putObject("coApplicant").put("name", "Testsson, Medsökande");

		assertRefused(flagged, "medsökande");
		assertRefused(withCoApplicant, "medsökande");
	}

	@Test
	void refusesAnInsatsWithoutSokande() {
		assertRefused(json("{ \"applicant\": null, \"coApplicant\": null, \"hasCoApplicant\": false }"), "Sökande finns inte");
	}

	@Test
	void readsThePeriodsPerPersonWithoutThePersonnummer() {
		final var raw = current();
		raw.set("coApplicant", json("""
			{ "periods": [
			    { "jobStimulusId": 201, "personId": "19900101T001", "fromDate": "2026-03-01", "toDate": "", "markedForRemoval": false },
			    { "jobStimulusId": 202, "personId": "19900101T001", "fromDate": "2025-01-01", "toDate": "2025-06-30", "markedForRemoval": true }
			  ], "personId": "19900101T001", "name": "Testsson, Medsökande" }
			"""));

		assertThat(toJobStimulusPeriods(raw)).containsExactly(
			new LifecareJobStimulusPeriod(101, "APPLICANT", "2021-01-01", "2021-12-31"),
			new LifecareJobStimulusPeriod(102, "APPLICANT", "2022-07-08", "2023-07-07"),
			new LifecareJobStimulusPeriod(103, "APPLICANT", "2026-01-01", "2027-12-31"),
			new LifecareJobStimulusPeriod(201, "CO_APPLICANT", "2026-03-01", null));
	}

	@Test
	void hasNoPeriodsForAHouseholdWithoutAny() {
		assertThat(toJobStimulusPeriods(json("{ \"applicant\": null, \"coApplicant\": null, \"hasCoApplicant\": false }"))).isEmpty();
	}
}
