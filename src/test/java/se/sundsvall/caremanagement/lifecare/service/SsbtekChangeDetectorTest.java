package se.sundsvall.caremanagement.lifecare.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;

class SsbtekChangeDetectorTest {

	private static final YearMonth MAY_2026 = YearMonth.of(2026, 5); // kontroll = April, jämförelse = March

	private static SsbtekIncome income(final String forman, final String date, final double amount) {
		return new SsbtekIncome(forman, null, null, BigDecimal.valueOf(amount), LocalDate.parse(date), APPLICANT);
	}

	@Test
	void noWarningWhenChangeWithinThreshold() {
		final var incomes = List.of(
			income("Bostadsbidrag", "2026-03-10", 1000),  // jämförelse
			income("Bostadsbidrag", "2026-04-10", 1050)); // kontroll, +5%

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026)).isEmpty();
	}

	@Test
	void warnsOnIncreaseAboveThreshold() {
		final var incomes = List.of(
			income("Bostadsbidrag", "2026-03-10", 1000),  // jämförelse
			income("Bostadsbidrag", "2026-04-10", 1200)); // kontroll, +20%

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026)).singleElement().satisfies(warning -> {
			assertThat(warning.forman()).isEqualTo("Bostadsbidrag");
			assertThat(warning.jamforelseSum()).isEqualByComparingTo("1000");
			assertThat(warning.kontrollSum()).isEqualByComparingTo("1200");
			assertThat(warning.changePercent()).isEqualByComparingTo("20.0");
		});
	}

	@Test
	void warnsOnDecreaseAboveThreshold() {
		final var incomes = List.of(
			income("Underhållsstöd", "2026-03-10", 1000),
			income("Underhållsstöd", "2026-04-10", 800)); // -20%

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026)).singleElement()
			.satisfies(warning -> assertThat(warning.changePercent()).isEqualByComparingTo("-20.0"));
	}

	@Test
	void warnsWhenFormanDisappearedInKontrollperiod() {
		final var incomes = List.of(income("Barnbidrag", "2026-03-10", 1250)); // only jämförelse -> -100%

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026)).singleElement().satisfies(warning -> {
			assertThat(warning.kontrollSum()).isEqualByComparingTo("0");
			assertThat(warning.changePercent()).isEqualByComparingTo("-100.0");
		});
	}

	@Test
	void doesNotWarnForFormanOnlyInKontrollperiod() {
		// New income (only in kontroll, not jämförelse) is not a "change" under this rule.
		final var incomes = List.of(income("Bostadsbidrag", "2026-04-10", 1000));

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026)).isEmpty();
	}

	@Test
	void sumsMultipleAmountsPerPeriodBeforeComparing() {
		final var incomes = List.of(
			income("Bostadsbidrag", "2026-03-05", 600),
			income("Bostadsbidrag", "2026-03-20", 400),   // jämförelse total 1000
			income("Bostadsbidrag", "2026-04-10", 1200)); // kontroll 1200, +20%

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026)).singleElement().satisfies(warning -> {
			assertThat(warning.jamforelseSum()).isEqualByComparingTo("1000");
			assertThat(warning.changePercent()).isEqualByComparingTo("20.0");
		});
	}

	@Test
	void exactlyAtThresholdDoesNotWarn() {
		final var incomes = List.of(
			income("Bostadsbidrag", "2026-03-10", 1000),
			income("Bostadsbidrag", "2026-04-10", 1120)); // +12.0%, not strictly greater

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026)).isEmpty();
	}

	@Test
	void honoursCustomThreshold() {
		final var incomes = List.of(
			income("Bostadsbidrag", "2026-03-10", 1000),
			income("Bostadsbidrag", "2026-04-10", 1200)); // +20%

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026, BigDecimal.valueOf(25))).isEmpty();
		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026, BigDecimal.valueOf(5))).hasSize(1);
	}

	@Test
	void dedupIsCaseInsensitiveAcrossPeriods() {
		final var incomes = List.of(
			income("Bostadsbidrag", "2026-03-10", 1000),
			income("bostadsbidrag", "2026-04-10", 1200)); // same förmån, different case -> compared together

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026)).hasSize(1);
	}

	@Test
	void skipsFormanWhoseJamforelseSumIsZero() {
		// Can't compute a percentage off a zero baseline — the förmån is skipped, not divided-by-zero.
		final var incomes = List.of(
			income("Bostadsbidrag", "2026-03-10", 500),
			income("Bostadsbidrag", "2026-03-11", -500), // jämförelse nets to 0
			income("Bostadsbidrag", "2026-04-10", 1200)); // kontroll

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026)).isEmpty();
	}

	@Test
	void ignoresIncomesWithoutAnAmountWhenSumming() {
		final var incomes = List.of(
			new SsbtekIncome("Bostadsbidrag", null, null, null, LocalDate.parse("2026-03-10"), APPLICANT), // no amount → ignored
			income("Bostadsbidrag", "2026-03-10", 1000),
			income("Bostadsbidrag", "2026-04-10", 1200));

		assertThat(SsbtekChangeDetector.detectIncomeChanges(incomes, MAY_2026)).singleElement()
			.satisfies(warning -> assertThat(warning.jamforelseSum()).isEqualByComparingTo("1000"));
	}

	@Test
	void nullOrEmptyIncomesReturnsEmpty() {
		assertThat(SsbtekChangeDetector.detectIncomeChanges(null, MAY_2026)).isEmpty();
		assertThat(SsbtekChangeDetector.detectIncomeChanges(List.of(), MAY_2026)).isEmpty();
	}
}
