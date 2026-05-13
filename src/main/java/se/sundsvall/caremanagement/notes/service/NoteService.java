package se.sundsvall.caremanagement.notes.service;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.notes.api.model.CreateNote;
import se.sundsvall.caremanagement.notes.api.model.Note;
import se.sundsvall.caremanagement.notes.integration.db.NoteRepository;
import se.sundsvall.caremanagement.notes.integration.db.model.NoteEntity;
import se.sundsvall.caremanagement.notes.service.event.NoteAdded;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class NoteService {

	private final NoteRepository repository;
	private final ApplicationEventPublisher events;

	NoteService(final NoteRepository repository, final ApplicationEventPublisher events) {
		this.repository = repository;
		this.events = events;
	}

	public String add(final String errandId, final CreateNote request) {
		final var timestamp = now(systemDefault()).truncatedTo(MILLIS);
		final var saved = repository.save(NoteEntity.create()
			.withErrandId(errandId)
			.withBody(request.body())
			.withAuthor(request.author())
			.withCreated(timestamp));

		events.publishEvent(new NoteAdded(saved.getId(), errandId, request.author(), timestamp));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public List<Note> listForErrand(final String errandId) {
		return repository.findByErrandIdOrderByCreatedDesc(errandId).stream()
			.map(NoteService::toNote)
			.toList();
	}

	@Transactional(readOnly = true)
	public Note read(final String noteId) {
		return toNote(repository.findById(noteId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No note with id '" + noteId + "'")));
	}

	public void delete(final String noteId) {
		repository.deleteById(noteId);
	}

	private static Note toNote(final NoteEntity e) {
		return Note.create()
			.withId(e.getId())
			.withErrandId(e.getErrandId())
			.withBody(e.getBody())
			.withAuthor(e.getAuthor())
			.withCreated(e.getCreated());
	}
}
