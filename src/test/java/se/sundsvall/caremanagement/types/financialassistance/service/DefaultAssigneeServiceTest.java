package se.sundsvall.caremanagement.types.financialassistance.service;

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
import static se.sundsvall.caremanagement.types.financialassistance.service.DefaultAssigneeService.DECISION_KEY;

@ExtendWith(MockitoExtension.class)
class DefaultAssigneeServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ProcessService processServiceMock;

	@InjectMocks
	private DefaultAssigneeService service;

	@Test
	void resolveReturnsConfiguredAssignee() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenReturn(List.of(Map.of("assignedUserId", "  joa01doe  ")));

		assertThat(service.resolve(MUNICIPALITY_ID)).contains("joa01doe");
	}

	@Test
	void resolveEmptyWhenNoRows() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap())).thenReturn(List.of());

		assertThat(service.resolve(MUNICIPALITY_ID)).isEmpty();
	}

	@Test
	void resolveEmptyWhenAssigneeBlank() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenReturn(List.of(Map.of("assignedUserId", "   ")));

		assertThat(service.resolve(MUNICIPALITY_ID)).isEmpty();
	}

	@Test
	void resolveEmptyWhenOutputMissing() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenReturn(List.of(Map.of("somethingElse", "x")));

		assertThat(service.resolve(MUNICIPALITY_ID)).isEmpty();
	}

	@Test
	void resolveEmptyWhenDecisionThrows() {
		when(processServiceMock.evaluateDecision(eq(MUNICIPALITY_ID), eq(DECISION_KEY), anyMap()))
			.thenThrow(new RuntimeException("operaton down"));

		assertThat(service.resolve(MUNICIPALITY_ID)).isEmpty();
	}
}
