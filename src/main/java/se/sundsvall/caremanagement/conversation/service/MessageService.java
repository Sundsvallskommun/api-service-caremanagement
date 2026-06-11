package se.sundsvall.caremanagement.conversation.service;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.conversation.api.model.CreateMessage;
import se.sundsvall.caremanagement.conversation.api.model.Message;
import se.sundsvall.caremanagement.conversation.integration.db.MessageRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;
import se.sundsvall.caremanagement.conversation.service.event.MessagePosted;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Per-errand message thread between caseworker and applicant. Universal across all errand types — a message is just
 * {@code (errandId, direction, body, author, created)}. Persists the message and publishes a {@link MessagePosted}
 * event so other modules can notify (typically a content-free notification on OUTBOUND); the body never leaves this
 * in-app thread.
 */
@Service
@Transactional
public class MessageService {

	private final MessageRepository repository;
	private final ApplicationEventPublisher events;

	MessageService(final MessageRepository repository, final ApplicationEventPublisher events) {
		this.repository = repository;
		this.events = events;
	}

	public String post(final String errandId, final CreateMessage request) {
		final var saved = repository.save(MessageEntity.create()
			.withErrandId(errandId)
			.withDirection(request.direction())
			.withBody(request.body())
			.withAuthor(request.author())
			.withCreated(now(systemDefault()).truncatedTo(MILLIS)));

		events.publishEvent(new MessagePosted(saved.getId(), errandId, saved.getDirection(), saved.getAuthor(), saved.getCreated()));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public List<Message> listForErrand(final String errandId) {
		return repository.findByErrandIdOrderByCreatedAsc(errandId).stream()
			.map(MessageService::toMessage)
			.toList();
	}

	@Transactional(readOnly = true)
	public Message read(final String messageId) {
		return toMessage(repository.findById(messageId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No message with id '" + messageId + "'")));
	}

	private static Message toMessage(final MessageEntity e) {
		return Message.create()
			.withId(e.getId())
			.withErrandId(e.getErrandId())
			.withDirection(e.getDirection())
			.withBody(e.getBody())
			.withAuthor(e.getAuthor())
			.withCreated(e.getCreated());
	}
}
