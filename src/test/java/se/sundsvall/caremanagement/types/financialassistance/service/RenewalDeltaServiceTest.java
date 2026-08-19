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
import static se.sundsvall.caremanagement.types.financialassistance.service.RenewalDeltaService.DECISION_KEY;

@ExtendWith(MockitoExtension.class)
class RenewalDeltaServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ProcessService processServiceMock;

	@InjectMocks
	private RenewalDeltaService service;

	@Test
	void classifyReturnsWarningAndRuleWhenPresent() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenReturn(List.of(Map.of("varning", true, "regel", "Kontrollera hyresunderlag")));

		final var result = service.classify(MUNICIPALITY_ID, "HOUSING_COST", 0, new BigDecimal("32"));

		assertThat(result.warning()).isTrue();
		assertThat(result.rule()).isEqualTo("Kontrollera hyresunderlag");
	}

	@Test
	void classifyReturnsUnflaggedWhenResultsEmpty() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap())).thenReturn(List.of());

		final var result = service.classify(MUNICIPALITY_ID, "HOUSEHOLD_SIZE", -1, BigDecimal.ZERO);

		assertThat(result.warning()).isFalse();
		assertThat(result.rule()).isNull();
	}

	@Test
	void classifyReturnsUnflaggedWhenRowHasNoWarning() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenReturn(List.of(Map.of("regel", "Inom tröskel")));

		final var result = service.classify(MUNICIPALITY_ID, "HOUSING_COST", 0, new BigDecimal("5"));

		assertThat(result.warning()).isFalse();
		assertThat(result.rule()).isEqualTo("Inom tröskel");
	}

	@Test
	void classifyFallsBackWhenDecisionThrows() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenThrow(new RuntimeException("operaton down"));

		// a null change kind + null percent exercise the orEmpty()/null-coalescing variable building before the failure
		final var result = service.classify(MUNICIPALITY_ID, null, 0, null);

		assertThat(result.warning()).isFalse();
		assertThat(result.rule()).isNull();
	}
}
