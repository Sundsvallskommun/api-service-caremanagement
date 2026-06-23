package se.sundsvall.caremanagement.conversation.spi;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

/**
 * Read-only cross-module view of an errand's full conversation thread (both directions, oldest first), exposed via the
 * {@code spi} named interface so the archiving job can render the thread into the meddelandehistorik PDF without
 * reaching into the conversation persistence layer. Returns message bodies + metadata and attachment file names only —
 * no attachment content crosses the boundary.
 */
@Service
public class ConversationThreadQueryService {

	private final MessageRepository messageRepository;
	private final MessageAttachmentRepository attachmentRepository;

	ConversationThreadQueryService(final MessageRepository messageRepository, final MessageAttachmentRepository attachmentRepository) {
		this.messageRepository = messageRepository;
		this.attachmentRepository = attachmentRepository;
	}

	/**
	 * The errand's conversation thread, oldest first, each message carrying the file names of its attachments.
	 */
	@Transactional(readOnly = true)
	public List<ConversationMessageView> threadForErrand(final String errandId) {
		final var messages = messageRepository.findByErrandIdOrderByCreatedAsc(errandId);
		final var fileNamesByMessageId = attachmentRepository.findByMessageIdIn(messages.stream().map(MessageEntity::getId).toList()).stream()
			.collect(groupingBy(MessageAttachmentEntity::getMessageId));

		return messages.stream()
			.map(message -> new ConversationMessageView(
				message.getDirection(),
				message.getBody(),
				message.getAuthor(),
				message.getCreated(),
				fileNamesByMessageId.getOrDefault(message.getId(), emptyList()).stream()
					.map(MessageAttachmentEntity::getFileName)
					.toList()))
			.toList();
	}
}
