package se.sundsvall.caremanagement.conversation.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.conversation.integration.db.MessageReadReceiptRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.caremanagement.conversation.spi.Direction;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.support.Identifier;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.UUID.randomUUID;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.dept44.support.Identifier.Type.AD_ACCOUNT;
import static se.sundsvall.dept44.support.Identifier.Type.PARTY_ID;

/**
 * Read-state for the conversation thread, kept separate from the immutable {@link MessageEntity}. A caller's side is
 * identified by the {@link Direction} they author ({@code OUTBOUND} = caseworker, {@code INBOUND} = applicant); they
 * read the messages addressed to them (the {@link Direction#opposite()} direction), and the {@code reader_side} receipt
 * stores their {@link Direction#role()}. The unread count is those addressed messages that have no receipt for the side
 * yet. Marking is per-message and idempotent. None of this is recorded in the händelselogg — the unread-count poll and
 * the mark-read call are excluded from the access log.
 */
@Service
@Transactional
public class MessageReadService {

	private static final String MESSAGES_NOT_FOUND_MESSAGE = "Message ids %s were not found on errand '%s'";

	private final ErrandAccessGuard errandGuard;
	private final MessageRepository messageRepository;
	private final MessageReadReceiptRepository receiptRepository;

	MessageReadService(final ErrandAccessGuard errandGuard, final MessageRepository messageRepository, final MessageReadReceiptRepository receiptRepository) {
		this.errandGuard = errandGuard;
		this.messageRepository = messageRepository;
		this.receiptRepository = receiptRepository;
	}

	@Transactional(readOnly = true)
	public long unreadCount(final String municipalityId, final String namespace, final String errandId, final Identifier caller) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		final var side = resolveCallerSide(caller);
		return receiptRepository.countUnread(errandId, side.opposite().name(), side.role());
	}

	/**
	 * Marks the given messages as read for the caller's side (derived from the {@code X-Sent-By} {@link Identifier}).
	 * Every id must reference a message on the errand; an id that does not is a client error. Ids that resolve to the
	 * side's own messages (it is not their recipient) and ids already marked read are silently ignored, so the call is
	 * idempotent.
	 */
	public void markRead(final String municipalityId, final String namespace, final String errandId, final Identifier caller, final List<String> messageIds) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		final var side = resolveCallerSide(caller);
		final var readBy = caller.getValue();
		final var distinctIds = messageIds.stream().distinct().toList();
		final var messages = messageRepository.findByErrandIdAndIdIn(errandId, distinctIds);

		if (messages.size() != distinctIds.size()) {
			final var found = messages.stream().map(MessageEntity::getId).toList();
			final var missing = distinctIds.stream().filter(id -> !found.contains(id)).toList();
			throw Problem.valueOf(NOT_FOUND, MESSAGES_NOT_FOUND_MESSAGE.formatted(missing, errandId));
		}

		final var addressedToReader = messages.stream()
			.filter(message -> side.opposite().name().equals(message.getDirection()))
			.map(MessageEntity::getId)
			.toList();
		if (addressedToReader.isEmpty()) {
			return;
		}

		// readAt stored as a UTC LocalDateTime to match the entity's NORMALIZE timezone storage on the native insert.
		final var readAt = now(systemDefault()).truncatedTo(MILLIS).withOffsetSameInstant(UTC).toLocalDateTime();
		final var alreadyRead = receiptRepository.findReadMessageIds(side.role(), addressedToReader);
		addressedToReader.stream()
			.filter(id -> !alreadyRead.contains(id))
			// INSERT IGNORE per row so a concurrent mark-as-read for the same message+side is a no-op rather than a
			// unique-constraint 500 — keeping the documented idempotency true under double-click / multi-tab races.
			.forEach(id -> receiptRepository.insertIgnore(randomUUID().toString(), id, side.role(), readBy, readAt));
	}

	/**
	 * Maps the caller identity to the {@link Direction} it authors — an adAccount is the caseworker ({@code OUTBOUND}), a
	 * partyId is the applicant ({@code INBOUND}). The caller reads the opposite direction. Any other identity type is a
	 * bad request.
	 */
	private static Direction resolveCallerSide(final Identifier caller) {
		final var type = caller.getType();
		if (type == AD_ACCOUNT) {
			return Direction.OUTBOUND;
		}
		if (type == PARTY_ID) {
			return Direction.INBOUND;
		}
		throw Problem.valueOf(BAD_REQUEST, "Cannot determine conversation side from header '" + Identifier.HEADER_NAME
			+ "' — expected type=adAccount (caseworker) or type=partyId (applicant)");
	}
}
