package se.sundsvall.caremanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.integration.db.model.ContactReasonEntity;

@CircuitBreaker(name = "contactReasonRepository")
public interface ContactReasonRepository extends JpaRepository<ContactReasonEntity, Long> {

	List<ContactReasonEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	Optional<ContactReasonEntity> findByReasonAndNamespaceAndMunicipalityId(String reason, String namespace, String municipalityId);

	boolean existsByReasonAndNamespaceAndMunicipalityId(String reason, String namespace, String municipalityId);
}
