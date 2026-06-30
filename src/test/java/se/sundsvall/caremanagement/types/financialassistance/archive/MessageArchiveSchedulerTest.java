package se.sundsvall.caremanagement.types.financialassistance.archive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class MessageArchiveSchedulerTest {

	@Mock
	private MessageArchiveService messageArchiveServiceMock;

	@InjectMocks
	private MessageArchiveScheduler scheduler;

	@Test
	void delegatesToService() {
		scheduler.archiveClosedErrandConversations();

		verify(messageArchiveServiceMock).archiveClosedErrands();
		verifyNoMoreInteractions(messageArchiveServiceMock);
	}
}
