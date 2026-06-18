package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;

@CircuitBreaker(name = "financialAssistanceNormPersonRepository")
public interface FaNormPersonRepository extends JpaRepository<FaNormPersonEntity, String> {

	List<FaNormPersonEntity> findByErrandId(String errandId);

	Optional<FaNormPersonEntity> findByIdAndErrandId(String id, String errandId);
}
