package se.sundsvall.caremanagement.conversation.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.conversation.integration.db.MessageReadReceiptRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.UUID.randomUUID;
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

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String MESSAGES_NOT_FOUND_MESSAGE = "Message ids %s were not found on errand '%s'";

	private final ErrandRepository errandRepository;
	private final MessageRepository messageRepository;
	private final MessageReadReceiptRepository receiptRepository;

	MessageReadService(final ErrandRepository errandRepository, final MessageRepository messageRepository, final MessageReadReceiptRepository receiptRepository) {
		this.errandRepository = errandRepository;
		this.messageRepository = messageRepository;
		this.receiptRepository = receiptRepository;
	}

	@Transactional(readOnly = true)
	public long unreadCount(final String municipalityId, final String namespace, final String errandId, final ReaderSide readerSide) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return receiptRepository.countUnread(errandId, readerSide.addressedDirection().name(), readerSide.name());
	}

	private void ensureErrandExists(final String municipalityId, final String namespace, final String errandId) {
		errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	/**
	 * Marks the given messages as read for the reader side. Every id must reference a message on the errand; an id that
	 * does not is a client error. Ids that resolve to the side's own messages (it is not their recipient) and ids already
	 * marked read are silently ignored, so the call is idempotent.
	 */
	public void markRead(final String municipalityId, final String namespace, final String errandId, final ReaderSide readerSide, final String readBy, final List<String> messageIds) {
		ensureErrandExists(municipalityId, namespace, errandId);
		final var distinctIds = messageIds.stream().distinct().toList();
		final var messages = messageRepository.findByErrandIdAndIdIn(errandId, distinctIds);

		if (messages.size() != distinctIds.size()) {
			final var found = messages.stream().map(MessageEntity::getId).toList();
			final var missing = distinctIds.stream().filter(id -> !found.contains(id)).toList();
			throw Problem.valueOf(NOT_FOUND, MESSAGES_NOT_FOUND_MESSAGE.formatted(missing, errandId));
		}

		final var addressedToReader = messages.stream()
			.filter(message -> readerSide.addressedDirection().name().equals(message.getDirection()))
			.map(MessageEntity::getId)
			.toList();
		if (addressedToReader.isEmpty()) {
			return;
		}

		// readAt stored as a UTC LocalDateTime to match the entity's NORMALIZE timezone storage on the native insert.
		final var readAt = now(systemDefault()).truncatedTo(MILLIS).withOffsetSameInstant(UTC).toLocalDateTime();
		final var alreadyRead = receiptRepository.findReadMessageIds(readerSide.name(), addressedToReader);
		addressedToReader.stream()
			.filter(id -> !alreadyRead.contains(id))
			// INSERT IGNORE per row so a concurrent mark-as-read for the same message+side is a no-op rather than a
			// unique-constraint 500 — keeping the documented idempotency true under double-click / multi-tab races.
			.forEach(id -> receiptRepository.insertIgnore(randomUUID().toString(), id, readerSide.name(), readBy, readAt));
	}
}
