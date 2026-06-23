package se.sundsvall.caremanagement.conversation.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.conversation.integration.db.MessageReadReceiptRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageReadReceiptEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Read-state for the conversation thread, kept separate from the immutable {@link MessageEntity}. A {@link ReaderSide}
 * reads the messages addressed to it (the opposite {@code direction}); the unread count is those messages that have no
 * receipt for the side yet. Marking is per-message and idempotent. None of this is recorded in the händelselogg — the
 * unread-count poll and the mark-read call are excluded from the access log.
 */
@Service
@Transactional
public class MessageReadService {

	private static final String MESSAGES_NOT_FOUND_MESSAGE = "No messages with ids %s on errand '%s'";

	private final MessageRepository messageRepository;
	private final MessageReadReceiptRepository receiptRepository;

	MessageReadService(final MessageRepository messageRepository, final MessageReadReceiptRepository receiptRepository) {
		this.messageRepository = messageRepository;
		this.receiptRepository = receiptRepository;
	}

	@Transactional(readOnly = true)
	public long unreadCount(final String errandId, final ReaderSide readerSide) {
		return receiptRepository.countUnread(errandId, readerSide.addressedDirection(), readerSide.name());
	}

	/**
	 * Marks the given messages as read for the reader side. Every id must reference a message on the errand; an id that
	 * does not is a client error. Ids that resolve to the side's own messages (it is not their recipient) and ids already
	 * marked read are silently ignored, so the call is idempotent.
	 */
	public void markRead(final String errandId, final ReaderSide readerSide, final String readBy, final List<String> messageIds) {
		final var distinctIds = messageIds.stream().distinct().toList();
		final var messages = messageRepository.findByErrandIdAndIdIn(errandId, distinctIds);

		if (messages.size() != distinctIds.size()) {
			final var found = messages.stream().map(MessageEntity::getId).toList();
			final var missing = distinctIds.stream().filter(id -> !found.contains(id)).toList();
			throw Problem.valueOf(NOT_FOUND, MESSAGES_NOT_FOUND_MESSAGE.formatted(missing, errandId));
		}

		final var addressedToReader = messages.stream()
			.filter(message -> readerSide.addressedDirection().equals(message.getDirection()))
			.map(MessageEntity::getId)
			.toList();
		if (addressedToReader.isEmpty()) {
			return;
		}

		final var alreadyRead = receiptRepository.findReadMessageIds(readerSide.name(), addressedToReader);
		final var receipts = addressedToReader.stream()
			.filter(id -> !alreadyRead.contains(id))
			.map(id -> MessageReadReceiptEntity.create()
				.withMessageId(id)
				.withReaderSide(readerSide.name())
				.withReadBy(readBy)
				.withReadAt(now(systemDefault()).truncatedTo(MILLIS)))
			.toList();

		receiptRepository.saveAll(receipts);
	}
}
