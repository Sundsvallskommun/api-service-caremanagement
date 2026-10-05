package se.sundsvall.caremanagement.operaton.service.scheduler.processmessageretry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessMessageRetrySchedulerTest {

	@Mock
	private ProcessMessageRetryWorker workerMock;

	@InjectMocks
	private ProcessMessageRetryScheduler scheduler;

	@Test
	void retryProcessMessagesDelegatesToTheWorker() {
		when(workerMock.retryDue()).thenReturn(new ProcessMessageRetryWorker.Result(2, 1, 1));

		scheduler.retryProcessMessages();

		verify(workerMock).retryDue();
		verifyNoMoreInteractions(workerMock);
	}

	@Test
	void anEmptyRunIsQuiet() {
		when(workerMock.retryDue()).thenReturn(new ProcessMessageRetryWorker.Result(0, 0, 0));

		scheduler.retryProcessMessages();

		verify(workerMock).retryDue();
	}
}
