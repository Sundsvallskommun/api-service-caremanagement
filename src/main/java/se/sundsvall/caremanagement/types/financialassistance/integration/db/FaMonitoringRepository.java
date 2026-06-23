package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaMonitoringEntity;

@CircuitBreaker(name = "financialAssistanceMonitoringRepository")
public interface FaMonitoringRepository extends JpaRepository<FaMonitoringEntity, String> {

	List<FaMonitoringEntity> findByErrandId(String errandId);

	Optional<FaMonitoringEntity> findByIdAndErrandId(String id, String errandId);

	Optional<FaMonitoringEntity> findByErrandIdAndLifecareId(String errandId, String lifecareId);
}
