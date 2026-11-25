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
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJobStimulusMapper.buildJobStimulusRemove;
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

	/**
	 * Calculation/SaveJobStimulus after the web app's remove (capture 2026-09-30, on the fixture's ids): 102 is left out.
	 */
	private static final String REMOVE_CAPTURE = """
		{
		  "applicant": {
		    "periods": [
		      { "jobStimulusId": 101, "personId": "19880209T050", "fromDate": "2021-01-01", "toDate": "2021-12-31", "updateTimestamp": "2026-08-21",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "880209-T050", "isValid": true, "minDate": 0 },
		      { "jobStimulusId": 103, "personId": "19880209T050", "fromDate": "2026-01-01", "toDate": "2027-12-31", "updateTimestamp": "2026-08-21",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "880209-T050", "isValid": true, "minDate": "2021-12-31" }
		    ],
		    "personId": "19880209T050", "name": "Testsson, Test", "personIdFormatted": "880209-T050"
		  },
		  "coApplicant": { "personId": "", "personIdFormatted": "", "name": "", "periods": [] }
		}
		""";

	/** GetJobStimulusForService on Jeppson Test, insats 24, a household with a medsökande (capture 2026-09-30). */
	private static final String WITH_CO_APPLICANT = """
		{
		  "applicant": {
		    "periods": [
		      { "jobStimulusId": 104, "personId": "19790101T030", "fromDate": "2026-01-01", "toDate": "2027-12-31", "updateTimestamp": "2026-09-23",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "790101-T030" },
		      { "jobStimulusId": 105, "personId": "19790101T030", "fromDate": "2028-01-01", "toDate": "2029-12-31", "updateTimestamp": "2026-09-23",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "790101-T030" }
		    ],
		    "personId": "19790101T030", "name": "Jeppson, Test", "personIdFormatted": "790101-T030"
		  },
		  "coApplicant": { "periods": [], "personId": "20120505T020", "name": "Jeppson, Medsökande", "personIdFormatted": "120505-T020" },
		  "hasCoApplicant": true
		}
		""";

	/**
	 * Calculation/SaveJobStimulus adding the medsökande's first period (capture 2026-09-30), field for field and in order.
	 */
	private static final String CO_APPLICANT_CAPTURE = """
		{
		  "applicant": {
		    "periods": [
		      { "jobStimulusId": 104, "personId": "19790101T030", "fromDate": "2026-01-01", "toDate": "2027-12-31", "updateTimestamp": "2026-09-23",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "790101-T030", "isValid": true, "minDate": 0 },
		      { "jobStimulusId": 105, "personId": "19790101T030", "fromDate": "2028-01-01", "toDate": "2029-12-31", "updateTimestamp": "2026-09-23",
		        "updateSignature": "ebb14eri", "markedForRemoval": false, "personIdFormatted": "790101-T030", "isValid": true, "minDate": "2027-12-31" }
		    ],
		    "personId": "19790101T030", "name": "Jeppson, Test", "personIdFormatted": "790101-T030"
		  },
		  "coApplicant": {
		    "periods": [
		      { "personId": "20120505T020", "personIdFormatted": "120505-T020", "fromDate": "2026-09-01", "toDate": "2028-08-31", "markedForRemoval": false }
		    ],
		    "personId": "20120505T020", "name": "Jeppson, Medsökande", "personIdFormatted": "120505-T020"
		  }
		}
		""";

	private static ObjectNode current() {
		return (ObjectNode) json(CURRENT);
	}

	private static void assertRefused(final JsonNode current, final String reason) {
		assertThatThrownBy(() -> buildJobStimulusAdd(current, "APPLICANT", "2028-01-15", "2030-01-14"))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus()).isEqualTo(UNPROCESSABLE_CONTENT);
				assertThat(problem.getDetail()).contains(reason);
			});
	}

	@Test
	void turnsTheReadPeriodsAndANewOneIntoExactlyTheCapturedSaveBody() {
		assertThat(serialised(buildJobStimulusAdd(current(), "APPLICANT", "2028-01-15", "2030-01-14"))).isEqualTo(serialised(json(CAPTURE)));
	}

	@Test
	void addsTheFirstPeriodOfSomeoneWhoHasNone() {
		final var none = current();
		((ObjectNode) none.get("applicant")).putArray("periods");

		final var body = buildJobStimulusAdd(none, "APPLICANT", "2026-10-01", "2028-09-30");

		assertThat(serialised(body.get("applicant").get("periods"))).isEqualTo(
			"[{\"personId\":\"19880209T050\",\"personIdFormatted\":\"880209-T050\",\"fromDate\":\"2026-10-01\",\"toDate\":\"2028-09-30\",\"markedForRemoval\":false}]");
	}

	@Test
	void givesMinDateZeroAfterAPeriodWithoutEnd() {
		final var openEnded = current();
		((ObjectNode) openEnded.get("applicant").get("periods").get(0)).putNull("toDate");

		final var body = buildJobStimulusAdd(openEnded, "APPLICANT", "2028-01-15", "2030-01-14");

		assertThat(body.get("applicant").get("periods").get(1).get("minDate").asInt()).isZero();
	}

	@Test
	void addsAMedsokandesPeriodSendingBothPersonsSetsBackLikeTheCapturedSaveBody() {
		assertThat(serialised(buildJobStimulusAdd(json(WITH_CO_APPLICANT), "CO_APPLICANT", "2026-09-01", "2028-08-31"))).isEqualTo(serialised(json(CO_APPLICANT_CAPTURE)));
	}

	@Test
	void keepsTheMedsokandesPeriodsWhenTheSokandeGetsOne() {
		final var current = (ObjectNode) json(WITH_CO_APPLICANT);
		((ObjectNode) current.get("coApplicant")).set("periods", json("""
			[{ "jobStimulusId": 115, "personId": "20120505T020", "fromDate": "2026-09-01", "toDate": "2028-08-31", "markedForRemoval": false },
			 { "jobStimulusId": 116, "personId": "20120505T020", "fromDate": "2028-09-01", "toDate": "2030-08-31", "markedForRemoval": false }]"""));

		final var body = buildJobStimulusAdd(current, "APPLICANT", "2030-01-01", "2031-12-31");

		assertThat(body.get("applicant").get("periods").values()).extracting(period -> period.get("fromDate").asString()).containsExactly("2026-01-01", "2028-01-01", "2030-01-01");
		final var coApplicantPeriods = body.get("coApplicant").get("periods");
		assertThat(coApplicantPeriods.values()).extracting(period -> period.get("jobStimulusId").asInt()).containsExactly(115, 116);
		// minDate is counted per person.
		assertThat(coApplicantPeriods.get(0).get("minDate").asInt()).isZero();
		assertThat(coApplicantPeriods.get(1).get("minDate").asString()).isEqualTo("2028-08-31");
		assertThat(body.get("coApplicant").get("name").asString()).isEqualTo("Jeppson, Medsökande");
	}

	@Test
	void refusesAPeriodForAMedsokandeTheInsatsDoesNotHave() {
		assertThatThrownBy(() -> buildJobStimulusAdd(current(), "CO_APPLICANT", "2026-09-01", "2028-08-31"))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus()).isEqualTo(UNPROCESSABLE_CONTENT);
				assertThat(problem.getDetail()).contains("ingen medsökande");
			});
	}

	@Test
	void refusesAnInsatsWithoutSokande() {
		assertRefused(json("{ \"applicant\": null, \"coApplicant\": null, \"hasCoApplicant\": false }"), "Sökande finns inte");
	}

	@Test
	void leavesTheRemovedPeriodOutAndCountsMinDateAgainLikeTheCapturedSaveBody() {
		assertThat(serialised(buildJobStimulusRemove(current(), 102))).isEqualTo(serialised(json(REMOVE_CAPTURE)));
	}

	@Test
	void removingTheFirstPeriodGivesTheNextMinDateZero() {
		final var periods = buildJobStimulusRemove(current(), 101).get("applicant").get("periods");

		assertThat(periods.values()).extracting(period -> period.get("jobStimulusId").asInt()).containsExactly(102, 103);
		assertThat(periods.get(0).get("minDate").asInt()).isZero();
		assertThat(periods.get(1).get("minDate").asString()).isEqualTo("2023-07-07");
	}

	@Test
	void removingTheOnlyPeriodSavesAnEmptySet() {
		final var single = current();
		final var only = single.get("applicant").get("periods").get(2);
		((ObjectNode) single.get("applicant")).putArray("periods").add(only);

		assertThat(buildJobStimulusRemove(single, 103).get("applicant").get("periods").isEmpty()).isTrue();
	}

	@Test
	void removesAMedsokandesPeriodKeepingTheSokandes() {
		final var current = (ObjectNode) json(WITH_CO_APPLICANT);
		((ObjectNode) current.get("coApplicant")).set("periods", json("""
			[{ "jobStimulusId": 115, "personId": "20120505T020", "fromDate": "2026-09-01", "toDate": "2028-08-31", "markedForRemoval": false }]"""));

		final var body = buildJobStimulusRemove(current, 115);

		assertThat(body.get("coApplicant").get("periods").isEmpty()).isTrue();
		assertThat(body.get("coApplicant").get("personId").asString()).isEqualTo("20120505T020");
		assertThat(body.get("applicant").get("periods").values()).extracting(period -> period.get("jobStimulusId").asInt()).containsExactly(104, 105);
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
