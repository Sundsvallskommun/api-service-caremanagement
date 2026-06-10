package se.sundsvall.caremanagement.attachments.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.attachments.integration.db.AttachmentRepository;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AttachmentErrandDeletedListenerTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private AttachmentRepository repositoryMock;

	@InjectMocks
	private AttachmentErrandDeletedListener listener;

	@Test
	void deletesAllAttachmentsForErrand() {
		listener.on(new ErrandDeleted("e1", "type", "2281", "MY_NAMESPACE", "user", FIXED_TIMESTAMP));

		verify(repositoryMock).deleteByErrandId("e1");
		verifyNoMoreInteractions(repositoryMock);
	}
}
