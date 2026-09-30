package se.sundsvall.caremanagement.operaton.service.scheduler.processmessageretry;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.operaton.integration.db.ProcessMessageRetryRepository;
import se.sundsvall.caremanagement.operaton.integration.db.model.ProcessMessageRetryEntity;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.dept44.problem.Problem;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ProcessMessageRetryWorkerTest {

	@Mock
	private ProcessMessageRetryRepository repositoryMock;

	@Mock
	private ProcessService processServiceMock;

	@InjectMocks
	private ProcessMessageRetryWorker worker;

	private static ProcessMessageRetryEntity pending(final String errandId, final int attempts, final OffsetDateTime created) {
		return ProcessMessageRetryEntity.create()
			.withId("r-" + errandId)
			.withMunicipalityId("2281")
			.withNamespace("FINANCIAL_ASSISTANCE")
			.withErrandId(errandId)
			.withMessageName("PaymentDecisionReceived")
			.withVariables("{\"paymentDecision\":\"APPROVED\"}")
			.withStatus("PENDING")
			.withAttempts(attempts)
			.withCreated(created)
			.withNextAttempt(created);
	}

	private static ProcessMessageRetryEntity pendingWithdrawal(final String errandId, final int attempts, final OffsetDateTime created) {
		return pending(errandId, attempts, created)
			.withMessageName("ErrandWithdrawn")
			.withVariables("{}");
	}

	@Test
	void aDeliveredMessageIsRemoved() {
		final var retry = pending("e1", 1, OffsetDateTime.now().minusMinutes(2));
		when(repositoryMock.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(eq("PENDING"), any())).thenReturn(List.of(retry));

		final var result = worker.retryDue();

		// Claimed (next attempt moved forward and saved) before the engine is called, then removed once delivered.
		final var order = inOrder(repositoryMock, processServiceMock);
		order.verify(repositoryMock).save(retry);
		order.verify(processServiceMock).correlateMessage("2281", "FINANCIAL_ASSISTANCE", "PaymentDecisionReceived", "e1", Map.of("paymentDecision", "APPROVED"));
		order.verify(repositoryMock).delete(retry);
		assertThat(result).isEqualTo(new ProcessMessageRetryWorker.Result(1, 1, 0));
	}

	@Test
	void aFailureToRecordTheFailureDoesNotStopTheBatch() {
		final var failing = pending("e1", 2, OffsetDateTime.now().minusHours(1));
		final var delivering = pending("e2", 1, OffsetDateTime.now().minusMinutes(2));
		when(repositoryMock.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(eq("PENDING"), any())).thenReturn(List.of(failing, delivering));
		doThrow(new IllegalStateException("engine down")).when(processServiceMock).correlateMessage(any(), any(), any(), eq("e1"), any());
		// The claim saves, the failure record does not.
		when(repositoryMock.save(failing)).thenReturn(failing).thenThrow(new IllegalStateException("db down"));

		final var result = worker.retryDue();

		verify(repositoryMock).delete(delivering);
		assertThat(result).isEqualTo(new ProcessMessageRetryWorker.Result(2, 1, 0));
	}

	@Test
	void aFailureBacksOffAndDoesNotStopTheBatch() {
		final var failing = pending("e1", 2, OffsetDateTime.now().minusHours(1));
		final var delivering = pending("e2", 1, OffsetDateTime.now().minusMinutes(2));
		when(repositoryMock.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(eq("PENDING"), any())).thenReturn(List.of(failing, delivering));
		doThrow(new IllegalStateException("Not Found: nothing waiting")).when(processServiceMock).correlateMessage(any(), any(), any(), eq("e1"), any());

		final var result = worker.retryDue();

		verify(repositoryMock, times(2)).save(failing); // the claim, then the failure
		assertThat(failing.getStatus()).isEqualTo("PENDING");
		assertThat(failing.getAttempts()).isEqualTo(3);
		assertThat(failing.getLastError()).isEqualTo("Not Found: nothing waiting");
		// Third attempt failed: the next one is four minutes out.
		assertThat(failing.getNextAttempt()).isCloseTo(OffsetDateTime.now().plusMinutes(4), within(5, SECONDS));
		verify(repositoryMock).delete(delivering);
		verify(repositoryMock, never()).delete(failing);
		assertThat(result).isEqualTo(new ProcessMessageRetryWorker.Result(2, 1, 0));
	}

	@Test
	void aRowOlderThanThreeDaysGivesUp() {
		final var retry = pending("e1", 80, OffsetDateTime.now().minusDays(3).minusMinutes(1));
		when(repositoryMock.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(eq("PENDING"), any())).thenReturn(List.of(retry));
		doThrow(new IllegalStateException("engine down")).when(processServiceMock).correlateMessage(any(), any(), any(), any(), any());

		final var result = worker.retryDue();

		verify(repositoryMock, times(2)).save(retry); // the claim, then giving up
		assertThat(retry.getStatus()).isEqualTo("GAVE_UP");
		assertThat(retry.getAttempts()).isEqualTo(81);
		assertThat(result).isEqualTo(new ProcessMessageRetryWorker.Result(1, 0, 1));
	}

	@Test
	void aWithdrawalTheEngineHasNoProcessToEndIsSettledAndRemoved() {
		final var retry = pendingWithdrawal("e1", 1, OffsetDateTime.now().minusMinutes(2));
		when(repositoryMock.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(eq("PENDING"), any())).thenReturn(List.of(retry));
		doThrow(Problem.valueOf(NOT_FOUND, "No process instance is waiting for message 'ErrandWithdrawn'")).when(processServiceMock)
			.correlateMessage("2281", "FINANCIAL_ASSISTANCE", "ErrandWithdrawn", "e1", Map.of());

		final var result = worker.retryDue();

		// Only the claim was saved; the row is removed instead of backing off, and is never marked as given up.
		verify(repositoryMock).save(retry);
		verify(repositoryMock).delete(retry);
		assertThat(retry.getStatus()).isEqualTo("PENDING");
		assertThat(result).isEqualTo(new ProcessMessageRetryWorker.Result(1, 1, 0));
	}

	@Test
	void aWithdrawalOlderThanThreeDaysThatHasNoProcessToEndIsNotGivenUpOn() {
		// Would have been GAVE_UP and logged as an error, had it been any other message.
		final var retry = pendingWithdrawal("e1", 80, OffsetDateTime.now().minusDays(3).minusMinutes(1));
		when(repositoryMock.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(eq("PENDING"), any())).thenReturn(List.of(retry));
		doThrow(Problem.valueOf(NOT_FOUND, "No process instance is waiting")).when(processServiceMock).correlateMessage(any(), any(), any(), any(), any());

		final var result = worker.retryDue();

		verify(repositoryMock).delete(retry);
		assertThat(retry.getStatus()).isEqualTo("PENDING");
		assertThat(result).isEqualTo(new ProcessMessageRetryWorker.Result(1, 1, 0));
	}

	@Test
	void aWithdrawalTheEngineCouldNotTakeIsRetriedLikeAnyOtherMessage() {
		final var retry = pendingWithdrawal("e1", 2, OffsetDateTime.now().minusHours(1));
		when(repositoryMock.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(eq("PENDING"), any())).thenReturn(List.of(retry));
		doThrow(Problem.valueOf(BAD_GATEWAY, "engine restarting")).when(processServiceMock).correlateMessage(any(), any(), any(), any(), any());

		final var result = worker.retryDue();

		verify(repositoryMock, never()).delete(retry);
		verify(repositoryMock, times(2)).save(retry); // the claim, then the failure
		assertThat(retry.getStatus()).isEqualTo("PENDING");
		assertThat(retry.getAttempts()).isEqualTo(3);
		assertThat(result).isEqualTo(new ProcessMessageRetryWorker.Result(1, 0, 0));
	}

	@Test
	void aNotFoundForTheDecisionMessageStillMeansTheProcessHasNotGotThereYet() {
		final var retry = pending("e1", 2, OffsetDateTime.now().minusHours(1));
		when(repositoryMock.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(eq("PENDING"), any())).thenReturn(List.of(retry));
		doThrow(Problem.valueOf(NOT_FOUND, "No process instance is waiting")).when(processServiceMock).correlateMessage(any(), any(), any(), any(), any());

		final var result = worker.retryDue();

		verify(repositoryMock, never()).delete(retry);
		assertThat(retry.getAttempts()).isEqualTo(3);
		assertThat(result).isEqualTo(new ProcessMessageRetryWorker.Result(1, 0, 0));
	}

	@Test
	void aSettledWithdrawalThatCannotBeRemovedIsLeftForTheNextRunAndDoesNotStopTheBatch() {
		final var settled = pendingWithdrawal("e1", 1, OffsetDateTime.now().minusMinutes(2));
		final var delivering = pending("e2", 1, OffsetDateTime.now().minusMinutes(2));
		when(repositoryMock.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(eq("PENDING"), any())).thenReturn(List.of(settled, delivering));
		doThrow(Problem.valueOf(NOT_FOUND, "No process instance is waiting")).when(processServiceMock).correlateMessage(any(), any(), any(), eq("e1"), any());
		doThrow(new IllegalStateException("db down")).when(repositoryMock).delete(settled);

		final var result = worker.retryDue();

		verify(repositoryMock).delete(delivering);
		assertThat(settled.getStatus()).isEqualTo("PENDING");
		assertThat(result).isEqualTo(new ProcessMessageRetryWorker.Result(2, 1, 0));
	}

	@Test
	void nothingDueDoesNothing() {
		when(repositoryMock.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(eq("PENDING"), any())).thenReturn(List.of());

		assertThat(worker.retryDue()).isEqualTo(new ProcessMessageRetryWorker.Result(0, 0, 0));
	}

	@ParameterizedTest
	@CsvSource({
		"0, 1", "1, 1", "2, 2", "3, 4", "4, 8", "6, 32", "7, 60", "40, 60"
	})
	void backoffDoublesUpToAnHour(final int attempts, final long minutes) {
		assertThat(ProcessMessageRetryWorker.backoff(attempts)).isEqualTo(Duration.ofMinutes(minutes));
	}
}
