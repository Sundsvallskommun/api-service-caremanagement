package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;

class LifecareCaseSummaryTest {

	@Test
	void accessors() {
		final var period = YearMonth.of(2026, MAY);
		final var summary = new LifecareCaseSummary(true, true, Set.of(period), period, true, true);

		assertThat(summary.hasFootprint()).isTrue();
		assertThat(summary.hasOpenCase()).isTrue();
		assertThat(summary.decisionMonths()).containsExactly(period);
		assertThat(summary.latestDecisionPeriod()).isEqualTo(period);
		assertThat(summary.hasCalculation()).isTrue();
		assertThat(summary.hasCoApplicant()).isTrue();
	}

	@Test
	void noneIsEmpty() {
		final var none = LifecareCaseSummary.none();

		assertThat(none.hasFootprint()).isFalse();
		assertThat(none.hasOpenCase()).isNull();
		assertThat(none.decisionMonths()).isEmpty();
		assertThat(none.latestDecisionPeriod()).isNull();
		assertThat(none.hasCalculation()).isFalse();
		assertThat(none.hasCoApplicant()).isFalse();
	}
}
