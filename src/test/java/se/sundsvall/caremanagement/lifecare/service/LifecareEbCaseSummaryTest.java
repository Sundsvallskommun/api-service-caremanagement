package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LifecareEbCaseSummaryTest {

	@Test
	void accessors() {
		final var period = YearMonth.of(2026, 5);
		final var summary = new LifecareEbCaseSummary(true, true, period, true, Set.of("198202022397"));

		assertThat(summary.hasOpenCase()).isTrue();
		assertThat(summary.hasDecisionForReferenceMonth()).isTrue();
		assertThat(summary.latestDecisionPeriod()).isEqualTo(period);
		assertThat(summary.hasCalculation()).isTrue();
		assertThat(summary.coApplicantPersonIds()).containsExactly("198202022397");
	}

	@Test
	void noneIsEmpty() {
		final var none = LifecareEbCaseSummary.none();

		assertThat(none.hasOpenCase()).isFalse();
		assertThat(none.hasDecisionForReferenceMonth()).isFalse();
		assertThat(none.latestDecisionPeriod()).isNull();
		assertThat(none.hasCalculation()).isFalse();
		assertThat(none.coApplicantPersonIds()).isEmpty();
	}
}
