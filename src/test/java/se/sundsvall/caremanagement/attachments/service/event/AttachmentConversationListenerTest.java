package se.sundsvall.caremanagement.attachments.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.conversation.service.event.MessageCreated;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AttachmentConversationListenerTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private AttachmentService attachmentServiceMock;

	@InjectMocks
	private AttachmentConversationListener listener;

	@Test
	void inboundWithAttachmentsRegeneratesClientPdf() {
		listener.on(new MessageCreated("m1", "2281", "MY_NAMESPACE", "e1", "INBOUND", "applicant", true, FIXED_TIMESTAMP));

		verify(attachmentServiceMock).regenerateClientAttachmentPdf("2281", "MY_NAMESPACE", "e1");
		verifyNoMoreInteractions(attachmentServiceMock);
	}

	@Test
	void outboundIsIgnored() {
		listener.on(new MessageCreated("m1", "2281", "MY_NAMESPACE", "e1", "OUTBOUND", "caseworker", true, FIXED_TIMESTAMP));

		verifyNoInteractions(attachmentServiceMock);
	}

	@Test
	void inboundWithoutAttachmentsIsIgnored() {
		listener.on(new MessageCreated("m1", "2281", "MY_NAMESPACE", "e1", "INBOUND", "applicant", false, FIXED_TIMESTAMP));

		verifyNoInteractions(attachmentServiceMock);
	}
}
