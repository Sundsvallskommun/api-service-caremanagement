package se.sundsvall.caremanagement.core.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandNumberSequenceEntity;

@CircuitBreaker(name = "errandNumberSequenceRepository")
public interface ErrandNumberSequenceRepository extends JpaRepository<ErrandNumberSequenceEntity, Long> {

	/**
	 * Idempotently creates the counter row (at {@code current_value = 0}) for the given scope if it does not already
	 * exist. {@code INSERT IGNORE} serializes concurrent first-creations on the {@code uq_errand_number_sequence}
	 * unique index and downgrades the duplicate-key collision to a no-op warning, so two transactions racing to create
	 * the very first errand of a month never propagate a {@code DataIntegrityViolationException}. Callers must run this
	 * before {@link #findByMunicipalityIdAndNamespaceAndSequenceYearAndSequenceMonth} so the locked read always finds a
	 * row to lock.
	 */
	@Modifying
	@Query(value = """
		insert ignore into errand_number_sequence
		    (municipality_id, namespace, sequence_year, sequence_month, current_value)
		values (:municipalityId, :namespace, :sequenceYear, :sequenceMonth, 0)
		""", nativeQuery = true)
	void ensureSequenceRow(@Param("municipalityId") String municipalityId, @Param("namespace") String namespace, @Param("sequenceYear") int sequenceYear, @Param("sequenceMonth") int sequenceMonth);

	/**
	 * Loads the counter row for the given scope under a pessimistic write lock so concurrent errand creations within
	 * the same {@code (municipality, namespace, year, month)} are serialized and never hand out the same number. The
	 * row is guaranteed to exist by a preceding {@link #ensureSequenceRow} call.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ErrandNumberSequenceEntity> findByMunicipalityIdAndNamespaceAndSequenceYearAndSequenceMonth(String municipalityId, String namespace, Integer sequenceYear, Integer sequenceMonth);
}
