package se.sundsvall.caremanagement.types.financialassistance.service;

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
	void aFlaggedRowWithoutARuleTextStillWarns() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_DAY_CHECK), anyMap()))
			.thenReturn(List.of(mapWithNullRule()));

		final var result = service.dayCheck(MUNICIPALITY_ID, false, null, null);

		assertThat(result.warning()).isTrue();
		assertThat(result.rule()).isNull();
	}

	@Test
	void anUnavailableDecisionDegradesToNoWarningRatherThanFailing() {
		// The whole point of the best-effort contract: the daily prepare must never be blocked because the engine
		// blinked or the table is not deployed. Silence here is a deliberate choice, not an accident - and it is the
		// safe direction, because the alternative is an errand that never gets prepared at all.
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY_DAY_CHECK), anyMap()))
			.thenThrow(new IllegalStateException("engine unreachable"));

		final var result = service.dayCheck(MUNICIPALITY_ID, true, null, 22);

		assertThat(result.warning()).isFalse();
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
