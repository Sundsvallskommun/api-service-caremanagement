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
import static se.sundsvall.caremanagement.types.financialassistance.service.ExpenseRulesService.DECISION_KEY;

@ExtendWith(MockitoExtension.class)
class ExpenseRulesServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ProcessService processServiceMock;

	@InjectMocks
	private ExpenseRulesService service;

	@Test
	void verdictReturnsApprovedAmountAndBucketWhenPresent() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenReturn(List.of(Map.of("approvedAmount", "8500", "bucket", "SPECIAL_EXPENSE")));

		final var result = service.verdict(MUNICIPALITY_ID, "RENT", null, "RENTAL", 2, "NATIONAL_NORM", new BigDecimal("9000"));

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("8500"));
		assertThat(result.bucket()).isEqualTo("SPECIAL_EXPENSE");
		assertThat(result.varning()).isFalse();
		assertThat(result.regel()).isNull();
	}

	@Test
	void verdictReadsVarningAndRegelFromRow() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenReturn(List.of(Map.of("approvedAmount", "7500", "bucket", "EXPENSE", "varning", true, "regel", "Rent över schablon")));

		final var result = service.verdict(MUNICIPALITY_ID, "RENT", null, "RENTAL", 2, "NATIONAL_NORM", new BigDecimal("9000"));

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("7500"));
		assertThat(result.bucket()).isEqualTo("EXPENSE");
		assertThat(result.varning()).isTrue();
		assertThat(result.regel()).isEqualTo("Rent över schablon");
	}

	@Test
	void verdictFallsBackToAppliedWhenResultsEmpty() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap())).thenReturn(List.of());

		final var result = service.verdict(MUNICIPALITY_ID, "RENT", null, "RENTAL", 2, "NATIONAL_NORM", new BigDecimal("9000"));

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("9000"));
		assertThat(result.bucket()).isEqualTo("EXPENSE");
	}

	@Test
	void verdictFallsBackToAppliedWhenRowHasNoApprovedAmount() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenReturn(List.of(Map.of("somethingElse", "1")));

		final var result = service.verdict(MUNICIPALITY_ID, "RENT", null, "RENTAL", 2, "NATIONAL_NORM", new BigDecimal("9000"));

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("9000"));
		assertThat(result.bucket()).isEqualTo("EXPENSE");
	}

	@Test
	void verdictFallsBackToAppliedWhenDecisionThrows() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenThrow(new RuntimeException("operaton down"));

		// nulls in the context exercise the nz()/null-coalescing variable building before the failure
		final var result = service.verdict(MUNICIPALITY_ID, "ELECTRICITY", null, null, null, null, new BigDecimal("1200"));

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("1200"));
		assertThat(result.bucket()).isEqualTo("EXPENSE");
	}
}
