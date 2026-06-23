package se.sundsvall.caremanagement.conversation.spi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentDataRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.stream.Collectors.groupingBy;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Read-only cross-module view of an errand's full conversation thread (both directions, oldest first) with attachment
 * content materialised, exposed via the {@code spi} named interface so the archiving job can render the thread into the
 * meddelandehistorik PDF and merge the attachments without reaching into the conversation persistence layer. Attachment
 * blobs are read fully into {@code byte[]} inside this module's own transaction; attachments whose blob row is missing
 * are dropped rather than failing the whole thread.
 */
@Service
public class ConversationThreadQueryService {

	private static final String READ_ERROR_MESSAGE = "Could not read conversation attachment content for attachment id '%s': %s";

	private final MessageRepository messageRepository;
	private final MessageAttachmentRepository attachmentRepository;
	private final MessageAttachmentDataRepository attachmentDataRepository;

	ConversationThreadQueryService(final MessageRepository messageRepository, final MessageAttachmentRepository attachmentRepository,
		final MessageAttachmentDataRepository attachmentDataRepository) {
		this.messageRepository = messageRepository;
		this.attachmentRepository = attachmentRepository;
		this.attachmentDataRepository = attachmentDataRepository;
	}

	/**
	 * The errand's conversation thread, oldest first, each message carrying its attachments (with content). Attachments
	 * whose blob row is missing are omitted.
	 */
	@Transactional(readOnly = true)
	public List<ConversationMessageView> threadForErrand(final String errandId) {
		final var messages = messageRepository.findByErrandIdOrderByCreatedAsc(errandId);
		final var attachmentsByMessageId = attachmentRepository.findByMessageIdIn(messages.stream().map(MessageEntity::getId).toList()).stream()
			.collect(groupingBy(MessageAttachmentEntity::getMessageId));

		return messages.stream()
			.map(message -> new ConversationMessageView(
				message.getDirection(),
				message.getBody(),
				message.getAuthor(),
				message.getCreated(),
				attachmentsByMessageId.getOrDefault(message.getId(), List.of()).stream()
					.map(this::toView)
					.flatMap(Optional::stream)
					.toList()))
			.toList();
	}

	private Optional<ConversationAttachmentView> toView(final MessageAttachmentEntity attachment) {
		return attachmentDataRepository.findByMessageAttachmentId(attachment.getId())
			.map(data -> {
				try {
					final var content = data.getFile().getBinaryStream().readAllBytes();
					return new ConversationAttachmentView(attachment.getFileName(), attachment.getMimeType(), content);
				} catch (final SQLException | IOException exception) {
					throw Problem.valueOf(INTERNAL_SERVER_ERROR, READ_ERROR_MESSAGE.formatted(attachment.getId(), exception.getMessage()));
				}
			});
	}
}
