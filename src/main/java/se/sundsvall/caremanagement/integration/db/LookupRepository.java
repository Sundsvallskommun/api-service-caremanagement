package se.sundsvall.caremanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;

@Transactional
@CircuitBreaker(name = "lookupRepository")
public interface LookupRepository extends JpaRepository<LookupEntity, Long> {

	List<LookupEntity> findAllByKindAndNamespaceAndMunicipalityId(LookupKind kind, String namespace, String municipalityId);

	Optional<LookupEntity> findByKindAndNamespaceAndMunicipalityIdAndName(LookupKind kind, String namespace, String municipalityId, String name);

	boolean existsByKindAndNamespaceAndMunicipalityIdAndName(LookupKind kind, String namespace, String municipalityId, String name);

	void deleteByKindAndNamespaceAndMunicipalityIdAndName(LookupKind kind, String namespace, String municipalityId, String name);
}
