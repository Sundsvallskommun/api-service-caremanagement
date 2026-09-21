package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPayeeEntity;

@CircuitBreaker(name = "financialAssistancePayeeRepository")
public interface FaPayeeRepository extends JpaRepository<FaPayeeEntity, String> {

	List<FaPayeeEntity> findByErrandId(String errandId);

	Optional<FaPayeeEntity> findByIdAndErrandId(String id, String errandId);
}
