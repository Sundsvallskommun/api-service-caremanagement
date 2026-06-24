package se.sundsvall.caremanagement.formsnapshot.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.formsnapshot.integration.db.model.FormSnapshotEntity;

@CircuitBreaker(name = "formSnapshotRepository")
public interface FormSnapshotRepository extends JpaRepository<FormSnapshotEntity, String> {

	Optional<FormSnapshotEntity> findByErrandId(String errandId);

	boolean existsByErrandId(String errandId);

	void deleteByErrandId(String errandId);
}
