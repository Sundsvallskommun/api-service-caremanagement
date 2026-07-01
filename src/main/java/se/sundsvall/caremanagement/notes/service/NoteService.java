package se.sundsvall.caremanagement.notes.service;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.notes.api.model.CreateNote;
import se.sundsvall.caremanagement.notes.api.model.Note;
import se.sundsvall.caremanagement.notes.api.model.UpdateNote;
import se.sundsvall.caremanagement.notes.integration.db.NoteRepository;
import se.sundsvall.caremanagement.notes.integration.db.model.NoteEntity;
import se.sundsvall.caremanagement.notes.service.event.NoteAdded;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class NoteService {

	private static final String NOTE_NOT_FOUND_MESSAGE = "No note with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";

	private final ErrandAccessGuard errandGuard;
	private final NoteRepository repository;
	private final ApplicationEventPublisher events;

	NoteService(final ErrandAccessGuard errandGuard, final NoteRepository repository, final ApplicationEventPublisher events) {
		this.errandGuard = errandGuard;
		this.repository = repository;
		this.events = events;
	}

	public String add(final String municipalityId, final String namespace, final String errandId, final CreateNote request) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		final var timestamp = now(systemDefault()).truncatedTo(MILLIS);
		final var saved = repository.save(NoteEntity.create()
			.withErrandId(errandId)
			.withBody(request.body())
			.withAuthor(request.author())
			.withCreated(timestamp));

		events.publishEvent(new NoteAdded(saved.getId(), errandId, municipalityId, namespace, request.author(), timestamp));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public List<Note> listForErrand(final String municipalityId, final String namespace, final String errandId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		return repository.findByErrandIdOrderByCreatedDesc(errandId).stream()
			.map(NoteService::toNote)
			.toList();
	}

	@Transactional(readOnly = true)
	public long countForErrand(final String municipalityId, final String namespace, final String errandId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		return repository.countByErrandId(errandId);
	}

	@Transactional(readOnly = true)
	public Note read(final String municipalityId, final String namespace, final String errandId, final String noteId) {
		return toNote(findNote(municipalityId, namespace, errandId, noteId));
	}

	public Note update(final String municipalityId, final String namespace, final String errandId, final String noteId, final UpdateNote request) {
		final var entity = findNote(municipalityId, namespace, errandId, noteId)
			.withBody(request.body())
			.withModifiedBy(request.modifiedBy())
			.withModified(now(systemDefault()).truncatedTo(MILLIS));

		return toNote(repository.save(entity));
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String noteId) {
		repository.delete(findNote(municipalityId, namespace, errandId, noteId));
	}

	private NoteEntity findNote(final String municipalityId, final String namespace, final String errandId, final String noteId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		return repository.findByErrandIdAndId(errandId, noteId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOTE_NOT_FOUND_MESSAGE.formatted(noteId, errandId, namespace, municipalityId)));
	}

	private static Note toNote(final NoteEntity e) {
		return Note.create()
			.withId(e.getId())
			.withErrandId(e.getErrandId())
			.withBody(e.getBody())
			.withAuthor(e.getAuthor())
			.withCreated(e.getCreated())
			.withModifiedBy(e.getModifiedBy())
			.withModified(e.getModified());
	}
}
