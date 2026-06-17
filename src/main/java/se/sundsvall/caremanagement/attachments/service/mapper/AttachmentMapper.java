package se.sundsvall.caremanagement.attachments.service.mapper;

import java.io.IOException;
import java.util.List;
import org.hibernate.Hibernate;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.api.model.Attachment;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentDataEntity;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.conversation.spi.ConversationAttachment;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public final class AttachmentMapper {

	private AttachmentMapper() {}

	public static Attachment toAttachment(final AttachmentEntity entity) {
		return ofNullable(entity)
			.map(e -> Attachment.create()
				.withId(e.getId())
				.withFileName(e.getFileName())
				.withMimeType(e.getMimeType())
				.withFileSize(e.getFileSize())
				.withOrigin(e.getOrigin())
				.withSenderRole(e.getSenderRole())
				.withCreated(e.getCreated())
				.withModified(e.getModified()))
			.orElse(null);
	}

	public static AttachmentEntity toAttachmentEntity(final String errandId, final String namespace,
		final String municipalityId, final String origin, final String senderRole, final MultipartFile file) {

		if (errandId == null || file == null) {
			return null;
		}
		try {
			return AttachmentEntity.create()
				.withErrandId(errandId)
				.withNamespace(namespace)
				.withMunicipalityId(municipalityId)
				.withFileName(file.getOriginalFilename())
				.withMimeType(file.getContentType())
				.withFileSize(Math.toIntExact(file.getSize()))
				.withOrigin(origin)
				.withSenderRole(senderRole)
				.withAttachmentData(AttachmentDataEntity.create()
					.withFile(Hibernate.getLobHelper().createBlob(file.getInputStream(), file.getSize())));
		} catch (final IOException ioException) {
			throw Problem.valueOf(BAD_REQUEST, "Could not read input stream: %s".formatted(ioException.getMessage()));
		}
	}

	public static AttachmentEntity toAttachmentEntity(final String errandId, final String namespace,
		final String municipalityId, final String origin, final String senderRole, final String fileName, final String mimeType, final byte[] content) {

		if (errandId == null || content == null) {
			return null;
		}
		return AttachmentEntity.create()
			.withErrandId(errandId)
			.withNamespace(namespace)
			.withMunicipalityId(municipalityId)
			.withFileName(fileName)
			.withMimeType(mimeType)
			.withFileSize(content.length)
			.withOrigin(origin)
			.withSenderRole(senderRole)
			.withAttachmentData(AttachmentDataEntity.create()
				.withFile(Hibernate.getLobHelper().createBlob(content)));
	}

	/** Project a conversation attachment into the unified errand attachment list, tagged with origin CONVERSATION. */
	public static Attachment toAttachment(final ConversationAttachment attachment) {
		return ofNullable(attachment)
			.map(a -> Attachment.create()
				.withId(a.id())
				.withMessageId(a.messageId())
				.withFileName(a.fileName())
				.withMimeType(a.mimeType())
				.withFileSize(a.fileSize())
				.withOrigin("CONVERSATION")
				.withSenderRole(a.senderRole())
				.withCreated(a.created()))
			.orElse(null);
	}

	public static List<Attachment> toAttachmentList(final List<AttachmentEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(AttachmentMapper::toAttachment)
			.toList();
	}
}
