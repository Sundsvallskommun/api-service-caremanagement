package se.sundsvall.caremanagement.statushistory.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.statushistory.integration.db.model.StatusHistoryEntity;

@CircuitBreaker(name = "statusHistoryRepository")
public interface StatusHistoryRepository extends JpaRepository<StatusHistoryEntity, String> {

	List<StatusHistoryEntity> findByErrandIdOrderByChangedAtDesc(String errandId);
}
