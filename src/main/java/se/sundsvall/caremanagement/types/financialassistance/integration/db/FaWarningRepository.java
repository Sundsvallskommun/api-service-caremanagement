package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaWarningEntity;

@CircuitBreaker(name = "financialAssistanceWarningRepository")
public interface FaWarningRepository extends JpaRepository<FaWarningEntity, String> {

	List<FaWarningEntity> findByErrandId(String errandId);

	Optional<FaWarningEntity> findByIdAndErrandId(String id, String errandId);
}
