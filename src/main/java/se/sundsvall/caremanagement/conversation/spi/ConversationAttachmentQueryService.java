package se.sundsvall.caremanagement.conversation.spi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentDataRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Read-only cross-module view of an errand's conversation attachments. Exposed (via the {@code spi} named interface) to
 * the attachments module, which folds the metadata into the unified errand attachment list and combines the client
 * (INBOUND) files into the consolidated client-attachment PDF. Binary content is fully materialised to {@code byte[]}
 * inside this service's own transaction so no {@code Blob} ever escapes the module boundary.
 */
@Service
public class ConversationAttachmentQueryService {

	private static final String READ_ERROR_MESSAGE = "Could not read conversation attachment content for attachment id '%s': %s";

	private final MessageAttachmentRepository attachmentRepository;
	private final MessageAttachmentDataRepository attachmentDataRepository;

	ConversationAttachmentQueryService(final MessageAttachmentRepository attachmentRepository,
		final MessageAttachmentDataRepository attachmentDataRepository) {
		this.attachmentRepository = attachmentRepository;
		this.attachmentDataRepository = attachmentDataRepository;
	}

	/**
	 * All conversation attachments on the errand (both directions), oldest first, as metadata only.
	 */
	@Transactional(readOnly = true)
	public List<ConversationAttachment> listForErrand(final String errandId) {
		return attachmentRepository.findByErrandIdOrderByCreatedAsc(errandId).stream()
			.map(ConversationAttachmentQueryService::toConversationAttachment)
			.toList();
	}

	/**
	 * The client-sent (INBOUND) conversation attachments on the errand, oldest first, with their binary content read
	 * fully into memory. Attachments whose blob row is missing are skipped rather than failing the whole consolidation.
	 */
	@Transactional(readOnly = true)
	public List<ConversationAttachmentContent> clientAttachmentContentsForErrand(final String errandId) {
		return attachmentRepository.findByErrandIdOrderByCreatedAsc(errandId).stream()
			.filter(attachment -> Direction.INBOUND.role().equals(attachment.getSenderRole()))
			.map(this::toContent)
			.filter(Objects::nonNull)
			.toList();
	}

	private ConversationAttachmentContent toContent(final MessageAttachmentEntity attachment) {
		return attachmentDataRepository.findByMessageAttachmentId(attachment.getId())
			.map(data -> {
				try (final var in = data.getFile().getBinaryStream()) {
					final var content = in.readAllBytes();
					return new ConversationAttachmentContent(attachment.getFileName(), attachment.getMimeType(), content);
				} catch (final SQLException | IOException exception) {
					throw Problem.valueOf(INTERNAL_SERVER_ERROR, READ_ERROR_MESSAGE.formatted(attachment.getId(), exception.getMessage()));
				}
			})
			.orElse(null);
	}

	private static ConversationAttachment toConversationAttachment(final MessageAttachmentEntity entity) {
		return new ConversationAttachment(
			entity.getId(),
			entity.getMessageId(),
			entity.getFileName(),
			entity.getMimeType(),
			entity.getFileSize(),
			entity.getCreated(),
			entity.getSenderRole());
	}
}
