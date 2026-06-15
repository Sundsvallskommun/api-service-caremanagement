package se.sundsvall.caremanagement.lifecare.service;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SsbtekPeriodsTest {

	@Test
	void derivesTheThreePeriodsFromApplicationMonth() {
		final var periods = SsbtekPeriods.forApplicationMonth(YearMonth.of(2026, 5));

		assertThat(periods.ansokningsperiod()).isEqualTo(YearMonth.of(2026, 5));
		assertThat(periods.kontrollperiod()).isEqualTo(YearMonth.of(2026, 4));
		assertThat(periods.jamforelseperiod()).isEqualTo(YearMonth.of(2026, 3));
	}

	@Test
	void crossesTheYearBoundary() {
		final var periods = SsbtekPeriods.forApplicationMonth(YearMonth.of(2026, 1));

		assertThat(periods.kontrollperiod()).isEqualTo(YearMonth.of(2025, 12));
		assertThat(periods.jamforelseperiod()).isEqualTo(YearMonth.of(2025, 11));
	}

	@Test
	void classifiesDatesByPeriod() {
		final var periods = SsbtekPeriods.forApplicationMonth(YearMonth.of(2026, 5));

		assertThat(periods.isInKontrollperiod(LocalDate.parse("2026-04-15"))).isTrue();
		assertThat(periods.isInKontrollperiod(LocalDate.parse("2026-04-01"))).isTrue();
		assertThat(periods.isInKontrollperiod(LocalDate.parse("2026-04-30"))).isTrue();
		assertThat(periods.isInKontrollperiod(LocalDate.parse("2026-05-01"))).isFalse();
		assertThat(periods.isInKontrollperiod(LocalDate.parse("2026-03-31"))).isFalse();
		assertThat(periods.isInKontrollperiod(null)).isFalse();

		assertThat(periods.isInJamforelseperiod(LocalDate.parse("2026-03-10"))).isTrue();
		assertThat(periods.isInJamforelseperiod(LocalDate.parse("2026-04-01"))).isFalse();
		assertThat(periods.isInJamforelseperiod(null)).isFalse();
	}
}
