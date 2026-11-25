package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.tree;

class PreviousCalculationPickerTest {

	/** Calculation/ListCalculations for insats 1 (capture 2026-09-24), newest first. */
	private static final JsonNode INSATS = tree("""
		[{"calculationId":31,"startDate":"2026-09-01","isFinalized":true},
		 {"calculationId":30,"startDate":"2026-10-01","isFinalized":true},
		 {"calculationId":29,"startDate":"2026-12-01","isFinalized":false},
		 {"calculationId":28,"startDate":"2026-11-01","isFinalized":false},
		 {"calculationId":12,"startDate":"2026-09-01","isFinalized":true},
		 {"calculationId":5,"startDate":"2026-10-01","isFinalized":true},
		 {"calculationId":2,"startDate":"2026-05-01","isFinalized":true},
		 {"calculationId":1,"startDate":"2026-01-01","isFinalized":false},
		 {"calculationId":0,"startDate":"","isFinalized":true}]""");

	private static Integer previousId(final String periodStart, final Integer own) {
		return PreviousCalculationPicker.pick(INSATS, periodStart, own).map(calculation -> calculation.path("calculationId").intValue()).orElse(null);
	}

	@Test
	void picksTheLatestPeriodBeforeTheErrandsOwn() {
		assertThat(previousId("2026-12-01", 29)).isEqualTo(28);
		assertThat(previousId("2026-09-01", 31)).isEqualTo(2);
	}

	@Test
	void prefersTheNewestOfSeveralBeräkningarForTheSamePeriod() {
		assertThat(previousId("2026-10-01", 30)).isEqualTo(31);
	}

	@Test
	void prefersASlutligBeräkningInTheSamePeriod() {
		final var samePeriod = tree("""
			[{"calculationId":40,"startDate":"2026-08-01","isFinalized":false},{"calculationId":39,"startDate":"2026-08-01","isFinalized":true}]""");

		assertThat(PreviousCalculationPicker.pick(samePeriod, "2026-09-01", null)).get().satisfies(calculation -> assertThat(calculation.path("calculationId").intValue()).isEqualTo(39));
	}

	@Test
	void neverTakesTheErrandsOwnAndTakesTheMostRecentOtherWithoutAPeriod() {
		assertThat(previousId(null, 29)).isEqualTo(28);
		assertThat(previousId("", 29)).isEqualTo(28);
	}

	@Test
	void hasNothingBeforeTheFirstPeriod() {
		assertThat(previousId("2026-01-01", null)).isNull();
		assertThat(PreviousCalculationPicker.pick(tree("{}"), null, null)).isEmpty();
	}
}
