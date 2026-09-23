package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.List;
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
	void isPreviousDecisionCandidateAlsoRequiresAPeriod() {
		assertThat(filter.isPreviousDecisionCandidate(decision(2, "2026-09-01"))).isTrue();
		assertThat(filter.isPreviousDecisionCandidate(decision(2, ""))).isFalse();
		assertThat(filter.isPreviousDecisionCandidate(decision(2, null))).isFalse();
		assertThat(filter.isPreviousDecisionCandidate(decision(21, "2026-09-01"))).isFalse();
	}

	private static DecisionView decision(final Integer serviceId, final String fromDate) {
		return new DecisionView(1, "2026-09-01", "Beslut", fromDate, "", "", "Anna", "IFO", serviceId, BigDecimal.ZERO, null, "", List.of());
	}
}
