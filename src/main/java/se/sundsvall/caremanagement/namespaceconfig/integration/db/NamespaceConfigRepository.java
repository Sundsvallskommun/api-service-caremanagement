package se.sundsvall.caremanagement.namespaceconfig.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.namespaceconfig.integration.db.model.NamespaceConfigEntity;

@Transactional
@CircuitBreaker(name = "namespaceConfigRepository")
public interface NamespaceConfigRepository extends JpaRepository<NamespaceConfigEntity, Long> {

	Optional<NamespaceConfigEntity> findByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	Optional<NamespaceConfigEntity> findByMunicipalityIdAndShortCode(String municipalityId, String shortCode);

	boolean existsByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	void deleteByNamespaceAndMunicipalityId(String namespace, String municipalityId);
}
