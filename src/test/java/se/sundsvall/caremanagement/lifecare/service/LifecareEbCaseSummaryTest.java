package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static java.time.Month.*;
import static org.assertj.core.api.Assertions.assertThat;

class LifecareEbCaseSummaryTest {

	@Test
	void accessors() {
		final var period = YearMonth.of(2026, MAY);
		final var summary = new LifecareEbCaseSummary(true, Set.of(period), period, true, true);

		assertThat(summary.hasFootprint()).isTrue();
		assertThat(summary.decisionMonths()).containsExactly(period);
		assertThat(summary.latestDecisionPeriod()).isEqualTo(period);
		assertThat(summary.hasCalculation()).isTrue();
		assertThat(summary.hasCoApplicant()).isTrue();
	}

	@Test
	void noneIsEmpty() {
		final var none = LifecareEbCaseSummary.none();

		assertThat(none.hasFootprint()).isFalse();
		assertThat(none.decisionMonths()).isEmpty();
		assertThat(none.latestDecisionPeriod()).isNull();
		assertThat(none.hasCalculation()).isFalse();
		assertThat(none.hasCoApplicant()).isFalse();
	}
}
