package se.sundsvall.caremanagement.attachments.service.event;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.attachments.integration.db.AttachmentRepository;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentDataEntity;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentErrandDeletedListenerTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private AttachmentRepository repositoryMock;

	@InjectMocks
	private AttachmentErrandDeletedListener listener;

	@Test
	void deletesAllAttachmentsForErrandIncludingAttachmentData() {
		// Each attachment carries its own attachment_data blob — deleteAll(entities) removes via the persistence
		// context (not a bulk SQL delete), which is what lets JPA's cascade=ALL remove the child attachment_data rows
		// too, so no longblob is orphaned on errand deletion.
		final var first = AttachmentEntity.create()
			.withId("a1")
			.withErrandId("e1")
			.withAttachmentData(AttachmentDataEntity.create().withId(1));
		final var second = AttachmentEntity.create()
			.withId("a2")
			.withErrandId("e1")
			.withAttachmentData(AttachmentDataEntity.create().withId(2));
		when(repositoryMock.findByErrandId("e1")).thenReturn(List.of(first, second));

		listener.on(new ErrandDeleted("e1", "type", "2281", "MY_NAMESPACE", "user", FIXED_TIMESTAMP));

		verify(repositoryMock).findByErrandId("e1");
		verify(repositoryMock).deleteAll(List.of(first, second));
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void deletesNothingWhenErrandHasNoAttachments() {
		when(repositoryMock.findByErrandId("e1")).thenReturn(List.of());

		listener.on(new ErrandDeleted("e1", "type", "2281", "MY_NAMESPACE", "user", FIXED_TIMESTAMP));

		verify(repositoryMock).findByErrandId("e1");
		verify(repositoryMock).deleteAll(List.of());
		verifyNoMoreInteractions(repositoryMock);
	}
}
