package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndedErrandGateTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandQueryService errandQueryServiceMock;

	@InjectMocks
	private EndedErrandGate gate;

	@ParameterizedTest
	@ValueSource(strings = {
		"WITHDRAWN", "REJECTED", "CLOSED"
	})
	void anErrandInATerminalStatusHasEnded(final String status) {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.of(Errand.create().withId(ERRAND_ID).withStatus(status)));

		assertThat(gate.endedStatus(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).contains(status);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"RECEIVED", "NEEDS_MANUAL_REVIEW", "UNDER_REVIEW", "SUPPLEMENT_REQUESTED", "AWAITING_DECISION", "GRANTED", "PAID"
	})
	void anErrandStillInProgressHasNot(final String status) {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.of(Errand.create().withId(ERRAND_ID).withStatus(status)));

		assertThat(gate.endedStatus(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
	}

	@Test
	void anErrandWithoutAStatusHasNot() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.of(Errand.create().withId(ERRAND_ID)));

		assertThat(gate.endedStatus(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
	}

	@Test
	void anErrandThatDoesNotExistIsLeftToTheCallersOwnScopeCheck() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Optional.empty());

		assertThat(gate.endedStatus(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
	}
}
