package se.sundsvall.caremanagement.cocaseworkers.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.cocaseworkers.integration.db.model.CoCaseworkerEntity;

@CircuitBreaker(name = "coCaseworkerRepository")
public interface CoCaseworkerRepository extends JpaRepository<CoCaseworkerEntity, String> {

	boolean existsByNamespaceAndMunicipalityIdAndErrandIdAndUserId(String namespace, String municipalityId, String errandId, String userId);

	List<CoCaseworkerEntity> findAllByNamespaceAndMunicipalityIdAndErrandId(String namespace, String municipalityId, String errandId, Sort sort);

	Optional<CoCaseworkerEntity> findByNamespaceAndMunicipalityIdAndErrandIdAndUserId(String namespace, String municipalityId, String errandId, String userId);
}
