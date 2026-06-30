package se.sundsvall.caremanagement.document.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.document.integration.db.model.DocumentEntity;

@CircuitBreaker(name = "documentRepository")
public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {

	List<DocumentEntity> findByErrandIdOrderByDocumentDateDescDocumentTimeDescCreatedDesc(String errandId);

	/**
	 * Loads the document only if it belongs to the given errand. Scopes reads to the errand (and, via the
	 * errand-existence guard in the service, the tenant) so a document id from another errand/tenant cannot be read —
	 * a miss returns empty and is mapped to {@code 404} by the service.
	 */
	Optional<DocumentEntity> findByIdAndErrandId(String id, String errandId);

	/**
	 * Loads the document under a pessimistic write lock, scoped to the given errand. Mutating paths (update/delete/lock)
	 * use this so a concurrent lock and edit are serialized: the second transaction blocks, then re-reads the
	 * now-{@code LOCKED} status and is rejected — closing the read-check-write race that would otherwise let an edit
	 * slip past skrivskydd. The errand scope also prevents mutating a document belonging to another errand/tenant.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from DocumentEntity e where e.id = :id and e.errandId = :errandId")
	Optional<DocumentEntity> findByIdAndErrandIdForUpdate(@Param("id") String id, @Param("errandId") String errandId);
}
