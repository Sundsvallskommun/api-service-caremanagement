package se.sundsvall.caremanagement.operaton.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.operaton.integration.db.model.ProcessMessageRetryEntity;

@CircuitBreaker(name = "processMessageRetryRepository")
public interface ProcessMessageRetryRepository extends JpaRepository<ProcessMessageRetryEntity, String> {

	/**
	 * The next due messages, oldest first, at most five: each is a blocking engine call of up to 35 s (connect + read
	 * timeout), so five keep one run well inside its five-minute ShedLock. The rest wait for the next minute's run.
	 */
	List<ProcessMessageRetryEntity> findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(String status, OffsetDateTime now);
}
