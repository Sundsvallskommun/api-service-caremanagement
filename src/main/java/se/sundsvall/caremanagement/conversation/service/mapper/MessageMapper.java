package se.sundsvall.caremanagement.conversation.service.mapper;

import java.io.IOException;
import java.util.List;
import org.hibernate.Hibernate;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.conversation.api.model.Message;
import se.sundsvall.caremanagement.conversation.api.model.MessageAttachment;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentDataEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public final class MessageMapper {

	private MessageMapper() {}

	public static Message toMessage(final MessageEntity entity, final List<MessageAttachmentEntity> attachments) {
		return ofNullable(entity)
			.map(e -> Message.create()
				.withId(e.getId())
				.withErrandId(e.getErrandId())
				.withDirection(e.getDirection())
				.withBody(e.getBody())
				.withAuthor(e.getAuthor())
				.withInReplyToId(e.getInReplyToId())
				.withCreated(e.getCreated())
				.withAttachments(toMessageAttachmentList(attachments)))
			.orElse(null);
	}

	public static List<MessageAttachment> toMessageAttachmentList(final List<MessageAttachmentEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(MessageMapper::toMessageAttachment)
			.toList();
	}

	public static MessageAttachment toMessageAttachment(final MessageAttachmentEntity entity) {
		return ofNullable(entity)
			.map(e -> MessageAttachment.create()
				.withId(e.getId())
				.withFileName(e.getFileName())
				.withMimeType(e.getMimeType())
				.withFileSize(e.getFileSize())
				.withSenderRole(e.getSenderRole())
				.withCreated(e.getCreated()))
			.orElse(null);
	}

	/**
	 * Build the attachment metadata row for an uploaded file. The binary content is stored separately via
	 * {@link #toMessageAttachmentDataEntity(String, MultipartFile)} once this row's id is known. {@code senderRole} is
	 * denormalised from the message {@code direction} (INBOUND = CLIENT, OUTBOUND = CASEWORKER) so the unified
	 * attachment list and the client-attachment consolidation never have to re-join to the message.
	 */
	public static MessageAttachmentEntity toMessageAttachmentEntity(final String messageId, final String direction, final MultipartFile file) {
		if (messageId == null || file == null) {
			return null;
		}
		return MessageAttachmentEntity.create()
			.withMessageId(messageId)
			.withFileName(file.getOriginalFilename())
			.withMimeType(file.getContentType())
			.withFileSize(Math.toIntExact(file.getSize()))
			.withSenderRole(senderRoleFromDirection(direction))
			.withCreated(now(systemDefault()).truncatedTo(MILLIS));
	}

	/** INBOUND messages come from the applicant (CLIENT); everything else is the caseworker (CASEWORKER). */
	private static String senderRoleFromDirection(final String direction) {
		return "INBOUND".equals(direction) ? "CLIENT" : "CASEWORKER";
	}

	/**
	 * Build the blob row holding an uploaded file's content, keyed to its already-persisted attachment metadata row.
	 */
	public static MessageAttachmentDataEntity toMessageAttachmentDataEntity(final String messageAttachmentId, final MultipartFile file) {
		if (messageAttachmentId == null || file == null) {
			return null;
		}
		try {
			return MessageAttachmentDataEntity.create()
				.withMessageAttachmentId(messageAttachmentId)
				.withFile(Hibernate.getLobHelper().createBlob(file.getInputStream(), file.getSize()));
		} catch (final IOException ioException) {
			throw Problem.valueOf(BAD_REQUEST, "Could not read input stream: %s".formatted(ioException.getMessage()));
		}
	}
}
