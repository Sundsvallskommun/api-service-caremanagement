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
import se.sundsvall.caremanagement.journal.service.event.JournalEntryCreated;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
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
 * arbetsanteckning); locking it makes it a {@code LOCKED} upprättad handling, after which {@code update} and
 * {@code delete} are rejected with {@code 409 Conflict}.
 *
 * <p>
 * Every operation is scoped to its errand and tenant: each first asserts the errand exists in the
 * {@code (municipalityId, namespace)} tenant, then loads the entry by id <em>and</em> errand id, so an entry id from
 * another errand or tenant resolves to {@code 404} rather than leaking or mutating cross-tenant data.
 */
@Service
@Transactional
public class JournalEntryService {

	private final JournalEntryRepository repository;
	private final ApplicationEventPublisher events;
	private final ErrandAccessGuard errandGuard;

	JournalEntryService(final JournalEntryRepository repository, final ApplicationEventPublisher events, final ErrandAccessGuard errandGuard) {
		this.repository = repository;
		this.events = events;
		this.errandGuard = errandGuard;
	}

	public String add(final String municipalityId, final String namespace, final String errandId, final CreateJournalEntry request) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

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

		events.publishEvent(new JournalEntryCreated(saved.getId(), errandId, municipalityId, namespace, request.type(), request.createdBy(), timestamp));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public List<JournalEntry> listForErrand(final String municipalityId, final String namespace, final String errandId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		return repository.findByErrandIdOrderByEntryDateDescEntryTimeDescCreatedDesc(errandId).stream()
			.map(JournalEntryService::toJournalEntry)
			.toList();
	}

	@Transactional(readOnly = true)
	public JournalEntry read(final String municipalityId, final String namespace, final String errandId, final String journalEntryId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		return toJournalEntry(find(errandId, journalEntryId));
	}

	public JournalEntry update(final String municipalityId, final String namespace, final String errandId, final String journalEntryId, final UpdateJournalEntry request) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		final var entity = requireWorking(findForUpdate(errandId, journalEntryId), "edited");
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

	public void delete(final String municipalityId, final String namespace, final String errandId, final String journalEntryId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		repository.delete(requireWorking(findForUpdate(errandId, journalEntryId), "deleted"));
	}

	/** Lock the entry (write-protection) — it becomes an immutable finalised record. Already-locked entries 409. */
	public JournalEntry lock(final String municipalityId, final String namespace, final String errandId, final String journalEntryId, final LockJournalEntry request) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		final var entity = findForUpdate(errandId, journalEntryId);
		if (entity.getStatus() == LOCKED) {
			throw Problem.valueOf(CONFLICT, "Journal entry is already locked");
		}
		entity
			.withStatus(LOCKED)
			.withLockedBy(ofNullable(request).map(LockJournalEntry::lockedBy).orElse(null))
			.withLocked(now(systemDefault()).truncatedTo(MILLIS));

		return toJournalEntry(repository.save(entity));
	}

	private JournalEntryEntity find(final String errandId, final String journalEntryId) {
		return repository.findByIdAndErrandId(journalEntryId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No journal entry with id '" + journalEntryId + "'"));
	}

	/**
	 * Reads the entry under a pessimistic write lock, scoped to the errand, so the lock-check-then-write in
	 * update/delete/lock cannot race a concurrent lock — the second transaction blocks and re-reads the current
	 * (possibly LOCKED) status. An entry belonging to another errand resolves to {@code 404}.
	 */
	private JournalEntryEntity findForUpdate(final String errandId, final String journalEntryId) {
		return repository.findByIdAndErrandIdForUpdate(journalEntryId, errandId)
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
