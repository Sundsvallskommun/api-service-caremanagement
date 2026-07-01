package se.sundsvall.caremanagement.conversation.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.conversation.api.model.CreateMessage;
import se.sundsvall.caremanagement.conversation.api.model.Message;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentDataRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageAttachmentRepository;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.caremanagement.conversation.service.event.MessagePosted;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.conversation.service.mapper.MessageMapper.toMessage;
import static se.sundsvall.caremanagement.conversation.service.mapper.MessageMapper.toMessageAttachmentDataEntity;
import static se.sundsvall.caremanagement.conversation.service.mapper.MessageMapper.toMessageAttachmentEntity;

/**
 * Per-errand message thread between caseworker and applicant. Universal across all errand types — a message is
 * {@code (errandId, direction, body, author, created)} plus any number of file attachments. Persists the message (and
 * its attachments) and publishes a {@link MessagePosted} event so other modules can notify (typically a content-free
 * notification on OUTBOUND — the caseworker's message to the applicant); the body and attachments never leave this
 * in-app thread.
 */
@Service
@Transactional
public class MessageService {

	private static final String MESSAGE_NOT_FOUND_MESSAGE = "No message with id '%s'";
	private static final String IN_REPLY_TO_NOT_FOUND_MESSAGE = "Cannot reply to message '%s' - no such message on errand '%s'";
	private static final String ATTACHMENT_NOT_FOUND_MESSAGE = "No attachment with id '%s' found on message '%s'";
	private static final String DATA_NOT_FOUND_MESSAGE = "No file content found for attachment with id '%s'";
	private static final String STREAM_ERROR_MESSAGE = "%s occurred when copying file with attachment id '%s' to response: %s";

	private final ErrandAccessGuard errandGuard;
	private final MessageRepository repository;
	private final MessageAttachmentRepository attachmentRepository;
	private final MessageAttachmentDataRepository attachmentDataRepository;
	private final ApplicationEventPublisher events;

	MessageService(final ErrandAccessGuard errandGuard, final MessageRepository repository, final MessageAttachmentRepository attachmentRepository,
		final MessageAttachmentDataRepository attachmentDataRepository, final ApplicationEventPublisher events) {
		this.errandGuard = errandGuard;
		this.repository = repository;
		this.attachmentRepository = attachmentRepository;
		this.attachmentDataRepository = attachmentDataRepository;
		this.events = events;
	}

	public String post(final String municipalityId, final String namespace, final String errandId, final CreateMessage request, final List<MultipartFile> attachments) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		validateInReplyTo(errandId, request.inReplyToId());

		final var saved = repository.save(MessageEntity.create()
			.withErrandId(errandId)
			.withDirection(request.direction())
			.withBody(request.body())
			.withAuthor(request.author())
			.withInReplyToId(request.inReplyToId())
			.withCreated(now(systemDefault()).truncatedTo(MILLIS)));

		final var files = ofNullable(attachments).orElse(emptyList());
		files.forEach(file -> store(saved.getId(), saved.getDirection(), file));

		events.publishEvent(new MessagePosted(saved.getId(), municipalityId, namespace, errandId,
			saved.getDirection(), saved.getAuthor(), !files.isEmpty(), saved.getCreated()));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public List<Message> listForErrand(final String municipalityId, final String namespace, final String errandId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		final var messages = repository.findByErrandIdOrderByCreatedAsc(errandId);
		final var attachmentsByMessageId = attachmentRepository
			.findByMessageIdIn(messages.stream().map(MessageEntity::getId).toList()).stream()
			.collect(Collectors.groupingBy(MessageAttachmentEntity::getMessageId));

		return messages.stream()
			.map(message -> toMessage(message, attachmentsByMessageId.getOrDefault(message.getId(), emptyList())))
			.toList();
	}

	@Transactional(readOnly = true)
	public Message read(final String municipalityId, final String namespace, final String errandId, final String messageId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		final var message = repository.findByIdAndErrandId(messageId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, MESSAGE_NOT_FOUND_MESSAGE.formatted(messageId)));
		return toMessage(message, attachmentRepository.findByMessageId(messageId));
	}

	public void streamAttachmentFile(final String municipalityId, final String namespace, final String errandId, final String messageId, final String attachmentId, final HttpServletResponse response) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		ensureMessageBelongsToErrand(errandId, messageId);
		final var attachment = attachmentRepository.findByMessageIdAndId(messageId, attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ATTACHMENT_NOT_FOUND_MESSAGE.formatted(attachmentId, messageId)));
		final var data = attachmentDataRepository.findByMessageAttachmentId(attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, DATA_NOT_FOUND_MESSAGE.formatted(attachmentId)));

		try {
			response.addHeader(CONTENT_TYPE, attachment.getMimeType());
			response.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"");
			ofNullable(attachment.getFileSize()).ifPresent(response::setContentLength);
			StreamUtils.copy(data.getFile().getBinaryStream(), response.getOutputStream());
		} catch (final IOException | SQLException exception) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, STREAM_ERROR_MESSAGE.formatted(exception.getClass().getSimpleName(), attachmentId, exception.getMessage()));
		}
	}

	private void store(final String messageId, final String direction, final MultipartFile file) {
		final var attachment = attachmentRepository.save(toMessageAttachmentEntity(messageId, direction, file));
		attachmentDataRepository.save(toMessageAttachmentDataEntity(attachment.getId(), file));
	}

	private void ensureMessageBelongsToErrand(final String errandId, final String messageId) {
		repository.findByIdAndErrandId(messageId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, MESSAGE_NOT_FOUND_MESSAGE.formatted(messageId)));
	}

	/**
	 * A reply may only reference a message on the same errand. A null reference (the common case) is allowed; a
	 * non-null one that does not resolve to a message on this errand is a client error.
	 */
	private void validateInReplyTo(final String errandId, final String inReplyToId) {
		if (inReplyToId == null) {
			return;
		}
		repository.findByIdAndErrandId(inReplyToId, errandId)
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, IN_REPLY_TO_NOT_FOUND_MESSAGE.formatted(inReplyToId, errandId)));
	}
}
