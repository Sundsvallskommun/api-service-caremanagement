package se.sundsvall.caremanagement.journal.service;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.journal.api.model.CreateJournalEntry;
import se.sundsvall.caremanagement.journal.api.model.JournalEntry;
import se.sundsvall.caremanagement.journal.api.model.LockJournalEntry;
import se.sundsvall.caremanagement.journal.api.model.UpdateJournalEntry;
import se.sundsvall.caremanagement.journal.integration.db.JournalEntryRepository;
import se.sundsvall.caremanagement.journal.integration.db.model.JournalEntryEntity;
import se.sundsvall.caremanagement.journal.service.event.JournalEntryAdded;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.journal.integration.db.model.JournalEntryStatus.LOCKED;
import static se.sundsvall.caremanagement.journal.integration.db.model.JournalEntryStatus.WORKING;

/**
 * Journalanteckningar (case-journal entries) on an errand. A created entry starts {@code WORKING} (an editable
 * arbetsanteckning); {@link #lock(String, LockJournalEntry) locking} it makes it a {@code LOCKED} upprättad handling,
 * after which {@link #update update} and {@link #delete delete} are rejected with {@code 409 Conflict}.
 */
@Service
@Transactional
public class JournalEntryService {

	private final JournalEntryRepository repository;
	private final ApplicationEventPublisher events;

	JournalEntryService(final JournalEntryRepository repository, final ApplicationEventPublisher events) {
		this.repository = repository;
		this.events = events;
	}

	public String add(final String errandId, final CreateJournalEntry request) {
		final var timestamp = now(systemDefault()).truncatedTo(MILLIS);
		final var saved = repository.save(JournalEntryEntity.create()
			.withErrandId(errandId)
			.withType(request.type())
			.withHeading(request.heading())
			.withText(request.text())
			.withEntryDate(request.entryDate())
			.withEntryTime(request.entryTime())
			.withStatus(WORKING)
			.withCreatedBy(request.createdBy())
			.withCreated(timestamp));

		events.publishEvent(new JournalEntryAdded(saved.getId(), errandId, request.type(), request.createdBy(), timestamp));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public List<JournalEntry> listForErrand(final String errandId) {
		return repository.findByErrandIdOrderByEntryDateDescEntryTimeDescCreatedDesc(errandId).stream()
			.map(JournalEntryService::toJournalEntry)
			.toList();
	}

	@Transactional(readOnly = true)
	public JournalEntry read(final String journalEntryId) {
		return toJournalEntry(find(journalEntryId));
	}

	public JournalEntry update(final String journalEntryId, final UpdateJournalEntry request) {
		final var entity = requireWorking(find(journalEntryId), "edited");
		entity
			.withType(request.type())
			.withHeading(request.heading())
			.withText(request.text())
			.withEntryDate(request.entryDate())
			.withEntryTime(request.entryTime())
			.withModifiedBy(request.modifiedBy())
			.withModified(now(systemDefault()).truncatedTo(MILLIS));

		return toJournalEntry(repository.save(entity));
	}

	public void delete(final String journalEntryId) {
		repository.delete(requireWorking(find(journalEntryId), "deleted"));
	}

	/** Lock the entry (skrivskydd) — it becomes an immutable upprättad handling. Already-locked entries 409. */
	public JournalEntry lock(final String journalEntryId, final LockJournalEntry request) {
		final var entity = find(journalEntryId);
		if (entity.getStatus() == LOCKED) {
			throw Problem.valueOf(CONFLICT, "Journal entry is already locked");
		}
		entity
			.withStatus(LOCKED)
			.withLockedBy(ofNullable(request).map(LockJournalEntry::lockedBy).orElse(null))
			.withLocked(now(systemDefault()).truncatedTo(MILLIS));

		return toJournalEntry(repository.save(entity));
	}

	private JournalEntryEntity find(final String journalEntryId) {
		return repository.findById(journalEntryId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No journal entry with id '" + journalEntryId + "'"));
	}

	private static JournalEntryEntity requireWorking(final JournalEntryEntity entity, final String action) {
		if (entity.getStatus() == LOCKED) {
			throw Problem.valueOf(CONFLICT, "Journal entry is locked and cannot be " + action);
		}
		return entity;
	}

	private static JournalEntry toJournalEntry(final JournalEntryEntity e) {
		return JournalEntry.create()
			.withId(e.getId())
			.withErrandId(e.getErrandId())
			.withType(e.getType())
			.withHeading(e.getHeading())
			.withText(e.getText())
			.withEntryDate(e.getEntryDate())
			.withEntryTime(e.getEntryTime())
			.withStatus(ofNullable(e.getStatus()).map(Enum::name).orElse(null))
			.withCreatedBy(e.getCreatedBy())
			.withCreated(e.getCreated())
			.withModifiedBy(e.getModifiedBy())
			.withModified(e.getModified())
			.withLockedBy(e.getLockedBy())
			.withLocked(e.getLocked());
	}
}
