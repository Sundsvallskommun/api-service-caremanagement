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
import se.sundsvall.caremanagement.document.service.event.DocumentCreated;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
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
 * Formal case documents on an errand. A created document starts {@code WORKING} (an editable draft);
 * locking it makes it a {@code LOCKED} upprättad handling, after which {@code update} and {@code delete} are rejected
 * with {@code 409 Conflict}.
 *
 * <p>
 * Every operation is scoped to its errand and tenant: each first asserts the errand exists in the
 * {@code (municipalityId, namespace)} tenant, then loads the document by id <em>and</em> errand id, so a document id
 * from another errand or tenant resolves to {@code 404} rather than leaking or mutating cross-tenant data.
 */
@Service
@Transactional
public class DocumentService {

	private final DocumentRepository repository;
	private final ApplicationEventPublisher publisher;
	private final ErrandAccessGuard errandGuard;

	DocumentService(final DocumentRepository repository, final ApplicationEventPublisher publisher, final ErrandAccessGuard errandGuard) {
		this.repository = repository;
		this.publisher = publisher;
		this.errandGuard = errandGuard;
	}

	public String add(final String municipalityId, final String namespace, final String errandId, final CreateDocument request) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

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

		publisher.publishEvent(new DocumentCreated(saved.getId(), errandId, municipalityId, namespace, request.type(), request.createdBy(), timestamp));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public List<Document> listForErrand(final String municipalityId, final String namespace, final String errandId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		return repository.findByErrandIdOrderByDocumentDateDescDocumentTimeDescCreatedDesc(errandId).stream()
			.map(DocumentService::toDocument)
			.toList();
	}

	@Transactional(readOnly = true)
	public Document read(final String municipalityId, final String namespace, final String errandId, final String documentId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		return toDocument(find(errandId, documentId));
	}

	public Document update(final String municipalityId, final String namespace, final String errandId, final String documentId, final UpdateDocument request) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		final var entity = requireWorking(findForUpdate(errandId, documentId), "edited")
			.withType(request.type())
			.withHeading(request.heading())
			.withText(request.text())
			.withDocumentDate(request.documentDate())
			.withDocumentTime(request.documentTime())
			.withModifiedBy(request.modifiedBy())
			.withModified(now(systemDefault()).truncatedTo(MILLIS));

		return toDocument(repository.save(entity));
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String documentId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		repository.delete(requireWorking(findForUpdate(errandId, documentId), "deleted"));
	}

	/** Lock the document (write-protection) — it becomes an immutable finalised record. Already-locked documents 409. */
	public Document lock(final String municipalityId, final String namespace, final String errandId, final String documentId, final LockDocument request) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);

		final var entity = findForUpdate(errandId, documentId);
		if (entity.getStatus() == LOCKED) {
			throw Problem.valueOf(CONFLICT, "Document is already locked");
		}
		entity
			.withStatus(LOCKED)
			.withLockedBy(ofNullable(request).map(LockDocument::lockedBy).orElse(null))
			.withLocked(now(systemDefault()).truncatedTo(MILLIS));

		return toDocument(repository.save(entity));
	}

	private DocumentEntity find(final String errandId, final String documentId) {
		return repository.findByIdAndErrandId(documentId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No document with id '" + documentId + "'"));
	}

	/**
	 * Reads the document under a pessimistic write lock, scoped to the errand, so the lock-check-then-write in
	 * update/delete/lock cannot race a concurrent lock — the second transaction blocks and re-reads the current
	 * (possibly LOCKED) status. A document belonging to another errand resolves to {@code 404}.
	 */
	private DocumentEntity findForUpdate(final String errandId, final String documentId) {
		return repository.findByIdAndErrandIdForUpdate(documentId, errandId)
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
