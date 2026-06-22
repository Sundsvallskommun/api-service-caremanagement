package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaBevakningEntity;

@CircuitBreaker(name = "financialAssistanceBevakningRepository")
public interface FaBevakningRepository extends JpaRepository<FaBevakningEntity, String> {

	List<FaBevakningEntity> findByErrandId(String errandId);

	Optional<FaBevakningEntity> findByIdAndErrandId(String id, String errandId);
}
