package se.sundsvall.caremanagement.operaton.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.operaton.integration.db.model.ProcessMessageRetryEntity;

@CircuitBreaker(name = "processMessageRetryRepository")
public interface ProcessMessageRetryRepository extends JpaRepository<ProcessMessageRetryEntity, String> {

	List<ProcessMessageRetryEntity> findByStatusAndNextAttemptBeforeOrderByNextAttempt(String status, OffsetDateTime now);
}
