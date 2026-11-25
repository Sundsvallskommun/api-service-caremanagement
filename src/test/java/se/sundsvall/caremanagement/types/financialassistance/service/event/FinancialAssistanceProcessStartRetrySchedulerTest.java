package se.sundsvall.caremanagement.types.financialassistance.service.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceProcessStartRetrySchedulerTest {

	@Mock
	private FinancialAssistanceProcessStartRetryWorker workerMock;

	@InjectMocks
	private FinancialAssistanceProcessStartRetryScheduler scheduler;

	@Test
	void retryProcessStartsDelegatesToTheWorker() {
		when(workerMock.retryUnstarted()).thenReturn(new FinancialAssistanceProcessStartRetryWorker.Result(3, 1, 1, 0, 1, 0));

		scheduler.retryProcessStarts();

		verify(workerMock).retryUnstarted();
		verifyNoMoreInteractions(workerMock);
	}

	@Test
	void aRunThatFoundNothingIsQuiet() {
		when(workerMock.retryUnstarted()).thenReturn(new FinancialAssistanceProcessStartRetryWorker.Result(0, 0, 0, 0, 0, 0));

		scheduler.retryProcessStarts();

		verify(workerMock).retryUnstarted();
		verifyNoMoreInteractions(workerMock);
	}

	@Test
	void aRunThatOnlyFoundAgedOutErrandsIsStillReported() {
		when(workerMock.retryUnstarted()).thenReturn(new FinancialAssistanceProcessStartRetryWorker.Result(0, 0, 0, 0, 0, 2));

		scheduler.retryProcessStarts();

		verify(workerMock).retryUnstarted();
	}
}
