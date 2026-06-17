package se.sundsvall.caremanagement.core.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandNumberSequenceEntity;

@CircuitBreaker(name = "errandNumberSequenceRepository")
public interface ErrandNumberSequenceRepository extends JpaRepository<ErrandNumberSequenceEntity, Long> {

	/**
	 * Loads the counter row for the given scope under a pessimistic write lock so concurrent errand creations within
	 * the same {@code (municipality, namespace, year)} are serialized and never hand out the same number.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ErrandNumberSequenceEntity> findByMunicipalityIdAndNamespaceAndSequenceYear(String municipalityId, String namespace, Integer sequenceYear);
}
