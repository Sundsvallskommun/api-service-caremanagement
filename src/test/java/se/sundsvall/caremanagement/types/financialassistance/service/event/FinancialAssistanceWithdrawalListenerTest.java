package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_AWAITING_DECISION;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_CLOSED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_NEEDS_MANUAL_REVIEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_RECEIVED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_SUPPLEMENT_REQUESTED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_UNDER_REVIEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_WITHDRAWN;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceWithdrawalListenerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "errand-1";
	private static final String MESSAGE = "ErrandWithdrawn";

	@Mock
	private ProcessService processServiceMock;

	@InjectMocks
	private FinancialAssistanceWithdrawalListener listener;

	private static ErrandStatusChanged event(final String typeSlug, final String fromStatus, final String toStatus) {
		return new ErrandStatusChanged(ERRAND_ID, typeSlug, MUNICIPALITY_ID, NAMESPACE, fromStatus, toStatus, "joa01doe",
			OffsetDateTime.parse("2026-09-30T09:00:00Z"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		SLUG_RENEWAL, SLUG_NEW, SLUG_SUPPLEMENTARY
	})
	void aWithdrawalEndsTheProcessOfEveryFinancialAssistanceType(final String typeSlug) {
		listener.endProcessOnWithdrawal(event(typeSlug, STATUS_AWAITING_DECISION, STATUS_WITHDRAWN));

		verify(processServiceMock).correlateMessage(MUNICIPALITY_ID, NAMESPACE, MESSAGE, ERRAND_ID, Map.of());
		verifyNoMoreInteractions(processServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		STATUS_RECEIVED, STATUS_NEEDS_MANUAL_REVIEW, STATUS_UNDER_REVIEW, STATUS_SUPPLEMENT_REQUESTED, STATUS_AWAITING_DECISION
	})
	void aWithdrawalIsAWithdrawalWhateverTheErrandWasDoing(final String fromStatus) {
		listener.endProcessOnWithdrawal(event(SLUG_RENEWAL, fromStatus, STATUS_WITHDRAWN));

		verify(processServiceMock).correlateMessage(MUNICIPALITY_ID, NAMESPACE, MESSAGE, ERRAND_ID, Map.of());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		STATUS_UNDER_REVIEW, STATUS_AWAITING_DECISION, STATUS_SUPPLEMENT_REQUESTED, STATUS_CLOSED
	})
	void anyOtherTransitionLeavesTheProcessAlone(final String toStatus) {
		listener.endProcessOnWithdrawal(event(SLUG_RENEWAL, STATUS_RECEIVED, toStatus));

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void leavingWithdrawnDoesNotEndAnything() {
		listener.endProcessOnWithdrawal(event(SLUG_RENEWAL, STATUS_WITHDRAWN, STATUS_CLOSED));

		verifyNoInteractions(processServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"support-errand", "financial-assistance"
	})
	void anErrandOfAnotherTypeHasNoProcessToEnd(final String typeSlug) {
		listener.endProcessOnWithdrawal(event(typeSlug, STATUS_RECEIVED, STATUS_WITHDRAWN));

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void anErrandWithoutATypeHasNoProcessToEnd() {
		listener.endProcessOnWithdrawal(event(null, STATUS_RECEIVED, STATUS_WITHDRAWN));

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void aTransitionWithoutATargetStatusIsIgnored() {
		listener.endProcessOnWithdrawal(event(SLUG_RENEWAL, STATUS_RECEIVED, null));

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void aMessageTheEngineCouldNotTakeIsQueuedForTheRetry() {
		final var failure = Problem.valueOf(BAD_GATEWAY, "operaton error: engine restarting");
		doThrow(failure).when(processServiceMock).correlateMessage(MUNICIPALITY_ID, NAMESPACE, MESSAGE, ERRAND_ID, Map.of());

		listener.endProcessOnWithdrawal(event(SLUG_RENEWAL, STATUS_AWAITING_DECISION, STATUS_WITHDRAWN));

		verify(processServiceMock).queueMessageRetry(MUNICIPALITY_ID, NAMESPACE, MESSAGE, ERRAND_ID, Map.of(), failure.getMessage());
	}

	@Test
	void anErrandWithNoRunningProcessIsDoneAndNothingIsQueued() {
		// An errand frozen for manual review has no process; one whose process already ended has nothing left to stop.
		doThrow(Problem.valueOf(NOT_FOUND, "No process instance is waiting for message 'ErrandWithdrawn'")).when(processServiceMock)
			.correlateMessage(MUNICIPALITY_ID, NAMESPACE, MESSAGE, ERRAND_ID, Map.of());

		listener.endProcessOnWithdrawal(event(SLUG_RENEWAL, STATUS_NEEDS_MANUAL_REVIEW, STATUS_WITHDRAWN));

		verify(processServiceMock, never()).queueMessageRetry(any(), any(), any(), any(), any(), any());
	}

	@Test
	void aFailureToQueueTheRetryIsNotSwallowed() {
		doThrow(Problem.valueOf(BAD_GATEWAY, "engine restarting")).when(processServiceMock).correlateMessage(any(), any(), any(), any(), any());
		doThrow(new IllegalStateException("db down")).when(processServiceMock).queueMessageRetry(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(MESSAGE), eq(ERRAND_ID), any(), any());
		final var event = event(SLUG_RENEWAL, STATUS_AWAITING_DECISION, STATUS_WITHDRAWN);

		assertThatThrownBy(() -> listener.endProcessOnWithdrawal(event))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("db down");
	}
}
