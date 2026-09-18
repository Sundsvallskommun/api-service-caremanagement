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
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.PeriodRulesService.DECISION_KEY_DAY_CHECK;
import static se.sundsvall.caremanagement.types.financialassistance.service.PeriodRulesService.DECISION_KEY_PARENTAL_BENEFIT;

@ExtendWith(MockitoExtension.class)
class PeriodRulesServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ProcessService processServiceMock;

	@InjectMocks
	private PeriodRulesService service;

	@Test
	void dayCheckPassesTheTablesOwnVariableNames() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_DAY_CHECK), anyMap()))
			.thenReturn(List.of(Map.of("varning", true, "regel", "Antal uttagna dagar saknas i SSBTEK-svaret – manuell kontroll")));

		final var result = service.dayCheck(MUNICIPALITY_ID, true, null, 22);

		final ArgumentCaptor<Map<String, Object>> captor = captor();
		verify(processServiceMock).evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_DAY_CHECK), captor.capture());
		assertThat(captor.getValue()).containsOnly(entry("periodLasbar", true), entry("uttagnaDagar", null), entry("ickeRodaDagar", 22));
		assertThat(result.warning()).isTrue();
		assertThat(result.rule()).isEqualTo("Antal uttagna dagar saknas i SSBTEK-svaret – manuell kontroll");
	}

	@Test
	void parentalBenefitCheckPassesTheTablesOwnVariableNames() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_PARENTAL_BENEFIT), anyMap()))
			.thenReturn(List.of(Map.of("varning", true, "regel", "Glapp mellan föregående föräldrapenningperiod och denna – manuell kontroll")));

		final var result = service.parentalBenefitCheck(MUNICIPALITY_ID, true, BigDecimal.valueOf(30), 30, true, 6);

		final ArgumentCaptor<Map<String, Object>> captor = captor();
		verify(processServiceMock).evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_PARENTAL_BENEFIT), captor.capture());
		assertThat(captor.getValue()).containsOnly(entry("periodLasbar", true), entry("uttagnaDagar", BigDecimal.valueOf(30)),
			entry("dagarIPerioden", 30), entry("foregaendeManadFinns", true), entry("glappDagar", 6));
		assertThat(result.warning()).isTrue();
	}

	@Test
	void aDecimalDayCountIsPassedThroughUntouched() {
		// FK sends partial parental-benefit days; rounding them here would undo the decimal fix in the contract.
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_PARENTAL_BENEFIT), anyMap()))
			.thenReturn(List.of());

		service.parentalBenefitCheck(MUNICIPALITY_ID, true, new BigDecimal("4.5"), 5, true, 0);

		final ArgumentCaptor<Map<String, Object>> captor = captor();
		verify(processServiceMock).evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_PARENTAL_BENEFIT), captor.capture());
		assertThat(captor.getValue()).contains(entry("uttagnaDagar", new BigDecimal("4.5")));
	}

	@Test
	void noRowsMeansNoWarning() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_DAY_CHECK), anyMap())).thenReturn(List.of());

		final var result = service.dayCheck(MUNICIPALITY_ID, false, null, null);

		assertThat(result.warning()).isFalse();
		assertThat(result.rule()).isNull();
	}

	@Test
	void nullRowsMeanNoWarning() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_DAY_CHECK), anyMap())).thenReturn(null);

		assertThat(service.dayCheck(MUNICIPALITY_ID, false, null, null).warning()).isFalse();
	}

	@Test
	void anUnavailableEngineRaisesNoWarningRatherThanBlockingThePrepare() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_PARENTAL_BENEFIT), anyMap()))
			.thenThrow(new IllegalStateException("engine down"));

		final var result = service.parentalBenefitCheck(MUNICIPALITY_ID, true, BigDecimal.ONE, 1, true, 0);

		assertThat(result.warning()).isFalse();
		assertThat(result.rule()).isNull();
	}

	@Test
	void aFlaggedRowWithoutARuleTextStillWarns() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_DAY_CHECK), anyMap()))
			.thenReturn(List.of(mapWithNullRule()));

		final var result = service.dayCheck(MUNICIPALITY_ID, false, null, null);

		assertThat(result.warning()).isTrue();
		assertThat(result.rule()).isNull();
	}

	private static Map<String, Object> mapWithNullRule() {
		final var row = new HashMap<String, Object>();
		row.put("varning", true);
		row.put("regel", null);
		return row;
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<Map<String, Object>> captor() {
		return ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
	}
}
