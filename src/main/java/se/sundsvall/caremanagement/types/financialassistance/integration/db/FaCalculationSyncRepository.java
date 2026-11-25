package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationSyncEntity;

@CircuitBreaker(name = "financialAssistanceCalculationSyncRepository")
public interface FaCalculationSyncRepository extends JpaRepository<FaCalculationSyncEntity, String> {

	List<FaCalculationSyncEntity> findByErrandId(String errandId);
}
