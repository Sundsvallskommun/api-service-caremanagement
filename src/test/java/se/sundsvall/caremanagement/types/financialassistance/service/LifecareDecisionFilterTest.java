package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;

import static org.assertj.core.api.Assertions.assertThat;

class LifecareDecisionFilterTest {

	private final LifecareDecisionFilter filter = new LifecareDecisionFilter(Set.of(2, 7));

	@Test
	void isFinancialAssistanceMatchesTheConfiguredServices() {
		assertThat(filter.isFinancialAssistance(decision(2, "2026-09-01"))).isTrue();
		assertThat(filter.isFinancialAssistance(decision(7, ""))).isTrue();
		assertThat(filter.isFinancialAssistance(decision(21, "2026-09-01"))).isFalse();
		assertThat(filter.isFinancialAssistance(decision(0, ""))).isFalse();
		assertThat(filter.isFinancialAssistance(decision(null, "2026-09-01"))).isFalse();
	}

	@Test
	void isPreviousDecisionCandidateWithoutErrandInsatsUsesTheConfiguredServicesAndRequiresAPeriod() {
		assertThat(filter.isPreviousDecisionCandidate(decision(2, "2026-09-01"), Optional.empty())).isTrue();
		assertThat(filter.isPreviousDecisionCandidate(decision(2, ""), Optional.empty())).isFalse();
		assertThat(filter.isPreviousDecisionCandidate(decision(2, null), Optional.empty())).isFalse();
		assertThat(filter.isPreviousDecisionCandidate(decision(21, "2026-09-01"), Optional.empty())).isFalse();
	}

	@Test
	void isPreviousDecisionCandidateWithErrandInsatsMatchesThatInsatsOnly() {
		assertThat(filter.isPreviousDecisionCandidate(decision(25, "2026-09-01"), Optional.of(25))).isTrue();
		assertThat(filter.isPreviousDecisionCandidate(decision(2, "2026-09-01"), Optional.of(25))).isFalse();
		assertThat(filter.isPreviousDecisionCandidate(decision(null, "2026-09-01"), Optional.of(25))).isFalse();
		assertThat(filter.isPreviousDecisionCandidate(decision(25, ""), Optional.of(25))).isFalse();
	}

	private static DecisionView decision(final Integer serviceId, final String fromDate) {
		return new DecisionView(1, "2026-09-01", "Beslut", fromDate, "", "", "Anna", "IFO", serviceId, BigDecimal.ZERO, null, "", List.of());
	}
}
