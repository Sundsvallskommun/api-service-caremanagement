package se.sundsvall.caremanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.integration.db.model.TypeEntity;

@Transactional
@CircuitBreaker(name = "typeRepository")
public interface TypeRepository extends JpaRepository<TypeEntity, Long> {

	List<TypeEntity> findAllByNamespaceAndMunicipalityId(String namespace, String municipalityId);

	boolean existsByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	TypeEntity getByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);

	void deleteByNamespaceAndMunicipalityIdAndName(String namespace, String municipalityId, String name);
}
