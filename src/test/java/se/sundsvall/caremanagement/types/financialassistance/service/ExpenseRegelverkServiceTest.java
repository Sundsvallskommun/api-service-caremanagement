package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.ExpenseRegelverkService.DECISION_KEY;

@ExtendWith(MockitoExtension.class)
class ExpenseRegelverkServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ProcessService processServiceMock;

	@InjectMocks
	private ExpenseRegelverkService service;

	@Test
	void capReturnsApprovedAmountWhenPresent() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenReturn(List.of(Map.of("approvedAmount", "8500")));

		final var result = service.cap(MUNICIPALITY_ID, "RENT", null, "RENTAL", 2, "RIKSNORM", new BigDecimal("9000"));

		assertThat(result).isEqualByComparingTo(new BigDecimal("8500"));
	}

	@Test
	void capFallsBackToAppliedWhenResultsEmpty() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap())).thenReturn(List.of());

		final var result = service.cap(MUNICIPALITY_ID, "RENT", null, "RENTAL", 2, "RIKSNORM", new BigDecimal("9000"));

		assertThat(result).isEqualByComparingTo(new BigDecimal("9000"));
	}

	@Test
	void capFallsBackToAppliedWhenRowHasNoApprovedAmount() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenReturn(List.of(Map.of("somethingElse", "1")));

		final var result = service.cap(MUNICIPALITY_ID, "RENT", null, "RENTAL", 2, "RIKSNORM", new BigDecimal("9000"));

		assertThat(result).isEqualByComparingTo(new BigDecimal("9000"));
	}

	@Test
	void capFallsBackToAppliedWhenDecisionThrows() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenThrow(new RuntimeException("operaton down"));

		// nulls in the context exercise the nz()/null-coalescing variable building before the failure
		final var result = service.cap(MUNICIPALITY_ID, "ELECTRICITY", null, null, null, null, new BigDecimal("1200"));

		assertThat(result).isEqualByComparingTo(new BigDecimal("1200"));
	}
}
