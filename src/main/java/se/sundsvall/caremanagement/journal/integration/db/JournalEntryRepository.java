package se.sundsvall.caremanagement.journal.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.journal.integration.db.model.JournalEntryEntity;

@CircuitBreaker(name = "journalEntryRepository")
public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, String> {

	List<JournalEntryEntity> findByErrandIdOrderByEntryDateDescEntryTimeDescCreatedDesc(String errandId);

	/**
	 * Loads the entry only if it belongs to the given errand. Scopes reads to the errand (and, via the
	 * errand-existence guard in the service, the tenant) so an entry id from another errand/tenant cannot be read —
	 * a miss returns empty and is mapped to {@code 404} by the service.
	 */
	Optional<JournalEntryEntity> findByIdAndErrandId(String id, String errandId);

	/**
	 * Loads the entry under a pessimistic write lock, scoped to the given errand. Mutating paths (update/delete/lock)
	 * use this so a concurrent lock and edit are serialized: the second transaction blocks, then re-reads the
	 * now-{@code LOCKED} status and is rejected — closing the read-check-write race that would otherwise let an edit
	 * slip past skrivskydd. The errand scope also prevents mutating an entry belonging to another errand/tenant.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from JournalEntryEntity e where e.id = :id and e.errandId = :errandId")
	Optional<JournalEntryEntity> findByIdAndErrandIdForUpdate(@Param("id") String id, @Param("errandId") String errandId);
}
