package se.sundsvall.caremanagement.document.service;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.document.api.model.CreateDocument;
import se.sundsvall.caremanagement.document.api.model.Document;
import se.sundsvall.caremanagement.document.api.model.LockDocument;
import se.sundsvall.caremanagement.document.api.model.UpdateDocument;
import se.sundsvall.caremanagement.document.integration.db.DocumentRepository;
import se.sundsvall.caremanagement.document.integration.db.model.DocumentEntity;
import se.sundsvall.caremanagement.document.service.event.DocumentAdded;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.document.integration.db.model.DocumentStatus.LOCKED;
import static se.sundsvall.caremanagement.document.integration.db.model.DocumentStatus.WORKING;

/**
 * Dokument (formal case documents) on an errand. A created document starts {@code WORKING} (an editable draft);
 * {@link #lock(String, LockDocument) locking} it makes it a {@code LOCKED} upprättad handling, after which
 * {@link #update update} and {@link #delete delete} are rejected with {@code 409 Conflict}.
 */
@Service
@Transactional
public class DocumentService {

	private final DocumentRepository repository;
	private final ApplicationEventPublisher events;

	DocumentService(final DocumentRepository repository, final ApplicationEventPublisher events) {
		this.repository = repository;
		this.events = events;
	}

	public String add(final String errandId, final CreateDocument request) {
		final var timestamp = now(systemDefault()).truncatedTo(MILLIS);
		final var saved = repository.save(DocumentEntity.create()
			.withErrandId(errandId)
			.withType(request.type())
			.withHeading(request.heading())
			.withText(request.text())
			.withDocumentDate(request.documentDate())
			.withDocumentTime(request.documentTime())
			.withStatus(WORKING)
			.withCreatedBy(request.createdBy())
			.withCreated(timestamp));

		events.publishEvent(new DocumentAdded(saved.getId(), errandId, request.type(), request.createdBy(), timestamp));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public List<Document> listForErrand(final String errandId) {
		return repository.findByErrandIdOrderByDocumentDateDescDocumentTimeDescCreatedDesc(errandId).stream()
			.map(DocumentService::toDocument)
			.toList();
	}

	@Transactional(readOnly = true)
	public Document read(final String documentId) {
		return toDocument(find(documentId));
	}

	public Document update(final String documentId, final UpdateDocument request) {
		final var entity = requireWorking(find(documentId), "edited");
		entity
			.withType(request.type())
			.withHeading(request.heading())
			.withText(request.text())
			.withDocumentDate(request.documentDate())
			.withDocumentTime(request.documentTime())
			.withModifiedBy(request.modifiedBy())
			.withModified(now(systemDefault()).truncatedTo(MILLIS));

		return toDocument(repository.save(entity));
	}

	public void delete(final String documentId) {
		repository.delete(requireWorking(find(documentId), "deleted"));
	}

	/** Lock the document (skrivskydd) — it becomes an immutable upprättad handling. Already-locked documents 409. */
	public Document lock(final String documentId, final LockDocument request) {
		final var entity = find(documentId);
		if (entity.getStatus() == LOCKED) {
			throw Problem.valueOf(CONFLICT, "Document is already locked");
		}
		entity
			.withStatus(LOCKED)
			.withLockedBy(ofNullable(request).map(LockDocument::lockedBy).orElse(null))
			.withLocked(now(systemDefault()).truncatedTo(MILLIS));

		return toDocument(repository.save(entity));
	}

	private DocumentEntity find(final String documentId) {
		return repository.findById(documentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No document with id '" + documentId + "'"));
	}

	private static DocumentEntity requireWorking(final DocumentEntity entity, final String action) {
		if (entity.getStatus() == LOCKED) {
			throw Problem.valueOf(CONFLICT, "Document is locked and cannot be " + action);
		}
		return entity;
	}

	private static Document toDocument(final DocumentEntity e) {
		return Document.create()
			.withId(e.getId())
			.withErrandId(e.getErrandId())
			.withType(e.getType())
			.withHeading(e.getHeading())
			.withText(e.getText())
			.withDocumentDate(e.getDocumentDate())
			.withDocumentTime(e.getDocumentTime())
			.withStatus(ofNullable(e.getStatus()).map(Enum::name).orElse(null))
			.withCreatedBy(e.getCreatedBy())
			.withCreated(e.getCreated())
			.withModifiedBy(e.getModifiedBy())
			.withModified(e.getModified())
			.withLockedBy(e.getLockedBy())
			.withLocked(e.getLocked());
	}
}
