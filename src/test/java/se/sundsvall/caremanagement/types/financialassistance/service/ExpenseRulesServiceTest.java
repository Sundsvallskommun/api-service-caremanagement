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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseRulesServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ProcessService processServiceMock;

	@InjectMocks
	private ExpenseRulesService service;

	@Test
	void verdictRoutesRentToHyraDecisionAndReadsOutputs() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq("Decision_hyra"), anyMap()))
			.thenReturn(List.of(Map.of("approvedAmount", "7100", "bucket", "EXPENSE", "varning", true, "regel", "Över gränsvärde")));

		final var result = service.verdict(MUNICIPALITY_ID, "RENT", new BigDecimal("9000"), null, 35, 0, 1);

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("7100"));
		assertThat(result.bucket()).isEqualTo("EXPENSE");
		assertThat(result.warning()).isTrue();
		assertThat(result.rule()).isEqualTo("Över gränsvärde");
	}

	@Test
	void verdictRoutesMedicineToItsOwnDecision() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq("Decision_medicin"), anyMap()))
			.thenReturn(List.of(Map.of("approvedAmount", "400", "bucket", "SPECIAL_EXPENSE")));

		final var result = service.verdict(MUNICIPALITY_ID, "MEDICINE", new BigDecimal("400"), new BigDecimal("400"), null, null, null);

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("400"));
		assertThat(result.bucket()).isEqualTo("SPECIAL_EXPENSE");
		assertThat(result.warning()).isFalse();
		assertThat(result.rule()).isNull();
	}

	@Test
	void verdictFallsBackToAppliedAndStaticBucketWhenResultsEmpty() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq("Decision_medicin"), anyMap())).thenReturn(List.of());

		final var result = service.verdict(MUNICIPALITY_ID, "MEDICINE", new BigDecimal("500"), null, null, null, null);

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("500"));
		assertThat(result.bucket()).isEqualTo("SPECIAL_EXPENSE");
		assertThat(result.warning()).isFalse();
	}

	@Test
	void verdictFallsBackToAppliedWhenRowHasNoApprovedAmount() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq("Decision_hyra"), anyMap()))
			.thenReturn(List.of(Map.of("somethingElse", "1")));

		final var result = service.verdict(MUNICIPALITY_ID, "RENT", new BigDecimal("9000"), null, 35, 0, 1);

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("9000"));
		assertThat(result.bucket()).isEqualTo("EXPENSE");
	}

	@Test
	void verdictFallsBackToAppliedAndStaticBucketWhenDecisionThrows() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq("Decision_sjukresor"), anyMap()))
			.thenThrow(new RuntimeException("operaton down"));

		// nulls in the context exercise the null-coalescing variable building before the failure
		final var result = service.verdict(MUNICIPALITY_ID, "TRAVEL_MEDICAL_TRANSPORT", new BigDecimal("300"), null, null, null, null);

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("300"));
		assertThat(result.bucket()).isEqualTo("SPECIAL_EXPENSE");
	}

	@Test
	void verdictFallsBackWithoutCallingTheEngineForAnUnmappedCostType() {
		final var result = service.verdict(MUNICIPALITY_ID, "GADGETS", new BigDecimal("750"), null, null, null, null);

		assertThat(result.processAmount()).isEqualByComparingTo(new BigDecimal("750"));
		assertThat(result.bucket()).isEqualTo("EXPENSE");
		assertThat(result.warning()).isFalse();
		verifyNoInteractions(processServiceMock);
	}

	@Test
	void bucketForCostTypeMapsToTheFcArray() {
		assertThat(ExpenseRulesService.bucketForCostType("RENT")).isEqualTo("EXPENSE");
		assertThat(ExpenseRulesService.bucketForCostType("MEDICINE")).isEqualTo("SPECIAL_EXPENSE");
		assertThat(ExpenseRulesService.bucketForCostType("OTHER")).isEqualTo("SPECIAL_EXPENSE");
		assertThat(ExpenseRulesService.bucketForCostType("GADGETS")).isEqualTo("EXPENSE");
	}
}
