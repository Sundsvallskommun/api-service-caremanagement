package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.ApplicationRulesService.DECISION_KEY_CALCULATION;
import static se.sundsvall.caremanagement.types.financialassistance.service.ApplicationRulesService.DECISION_KEY_INCOME;
import static se.sundsvall.caremanagement.types.financialassistance.service.ApplicationRulesService.DECISION_KEY_QUESTIONS;

@ExtendWith(MockitoExtension.class)
class ApplicationRulesServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ProcessService processServiceMock;

	@InjectMocks
	private ApplicationRulesService service;

	@Test
	void questionReturnsCodeAndRuleWhenFlagged() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_QUESTIONS), anyMap()))
			.thenReturn(List.of(Map.of("varning", true, "varningskod", "CHILD_NOT_FULL_TIME", "regel", "Barn med PERSONNUMMER bor inte heltid")));

		final var result = service.question(MUNICIPALITY_ID, "BARN_BOENDE_OMFATTNING", "HALF_TIME");

		assertThat(result.warning()).isTrue();
		assertThat(result.code()).isEqualTo("CHILD_NOT_FULL_TIME");
		assertThat(result.rule()).isEqualTo("Barn med PERSONNUMMER bor inte heltid");

		final var captor = captureVariables(DECISION_KEY_QUESTIONS);
		assertThat(captor).containsOnly(entry("fraga", "BARN_BOENDE_OMFATTNING"), entry("svar", "HALF_TIME"));
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void questionNormalisesTheTablesNoValueMarker() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_QUESTIONS), anyMap()))
			.thenReturn(List.of(Map.of("varning", false, "varningskod", "-", "regel", "-")));

		final var result = service.question(MUNICIPALITY_ID, "BARN_BOENDE_OMFATTNING", "FULL_TIME");

		assertThat(result.warning()).isFalse();
		assertThat(result.code()).isNull();
		assertThat(result.rule()).isNull();

		verify(processServiceMock).evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_QUESTIONS), anyMap());
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void questionSendsEmptyStringsForNulls() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_QUESTIONS), anyMap())).thenReturn(List.of());

		final var result = service.question(MUNICIPALITY_ID, null, null);

		assertThat(result.warning()).isFalse();
		assertThat(result.code()).isNull();
		assertThat(result.rule()).isNull();
		assertThat(captureVariables(DECISION_KEY_QUESTIONS)).containsOnly(entry("fraga", ""), entry("svar", ""));
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void questionFallsBackWhenDecisionThrows() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_QUESTIONS), anyMap()))
			.thenThrow(new RuntimeException("operaton down"));

		final var result = service.question(MUNICIPALITY_ID, "BILAGOR", "JA");

		assertThat(result.warning()).isFalse();
		assertThat(result.rule()).isNull();

		verify(processServiceMock).evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_QUESTIONS), anyMap());
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void questionFallsBackWhenResultsAreNull() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_QUESTIONS), anyMap())).thenReturn(null);

		assertThat(service.question(MUNICIPALITY_ID, "BILAGOR", "NEJ").warning()).isFalse();

		verify(processServiceMock).evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_QUESTIONS), anyMap());
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void incomeAgainstPreviousPassesNullSumsThroughForTheTableToCoalesce() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_INCOME), anyMap()))
			.thenReturn(List.of(Map.of("varning", true, "varningskod", "INCOME_AMOUNT_MISMATCH_PREVIOUS_CALCULATION", "regel", "Underhållsbidraget skiljer")));

		final var result = service.incomeAgainstPrevious(MUNICIPALITY_ID, "CHILD_SUPPORT", false, true, null, new BigDecimal("800"));

		assertThat(result.warning()).isTrue();
		assertThat(result.code()).isEqualTo("INCOME_AMOUNT_MISMATCH_PREVIOUS_CALCULATION");

		final var expected = new HashMap<String, Object>();
		expected.put("inkomstslag", "CHILD_SUPPORT");
		expected.put("fannsForegaende", false);
		expected.put("finnsIAnsokan", true);
		expected.put("summaForegaende", null);
		expected.put("summaAnsokan", new BigDecimal("800"));

		assertThat(captureVariables(DECISION_KEY_INCOME)).containsExactlyInAnyOrderEntriesOf(expected);
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void incomeAgainstPreviousFallsBackWhenDecisionThrows() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_INCOME), anyMap()))
			.thenThrow(new RuntimeException("not deployed"));

		assertThat(service.incomeAgainstPrevious(MUNICIPALITY_ID, "SALARY", true, false, null, null).warning()).isFalse();

		verify(processServiceMock).evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_INCOME), anyMap());
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void againstPreviousCalculationSendsTheComparisonAndEquality() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_CALCULATION), anyMap()))
			.thenReturn(List.of(Map.of("varning", true, "varningskod", "NORM_MISMATCH_PREVIOUS_CALCULATION", "regel", "NORM mot NORM")));

		final var result = service.againstPreviousCalculation(MUNICIPALITY_ID, "NORM", false);

		assertThat(result.warning()).isTrue();
		assertThat(result.rule()).isEqualTo("NORM mot NORM");
		assertThat(captureVariables(DECISION_KEY_CALCULATION)).containsOnly(entry("jamforelse", "NORM"), entry("sammaSomForegaende", false));
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void againstPreviousCalculationFallsBackWhenDecisionThrows() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_CALCULATION), anyMap()))
			.thenThrow(new RuntimeException("operaton down"));

		assertThat(service.againstPreviousCalculation(MUNICIPALITY_ID, "ANTAL_I_BOSTADEN", true).warning()).isFalse();

		verify(processServiceMock).evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_CALCULATION), anyMap());
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void ruleVerdictNoneIsNeutral() {
		final var none = ApplicationRulesService.RuleVerdict.none();

		assertThat(none.warning()).isFalse();
		assertThat(none.code()).isNull();
		assertThat(none.rule()).isNull();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> captureVariables(final String decisionKey) {
		final var captor = ArgumentCaptor.forClass(Map.class);
		verify(processServiceMock).evaluateDecision(eq(MUNICIPALITY_ID), eq(decisionKey), captor.capture());
		return captor.getValue();
	}
}
