package se.sundsvall.caremanagement.permit.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.permit.integration.db.model.PermitEntity;

@CircuitBreaker(name = "permitRepository")
public interface PermitRepository extends JpaRepository<PermitEntity, String> {

	List<PermitEntity> findByErrandIdOrderByCreatedDesc(String errandId);

	Optional<PermitEntity> findByErrandIdAndId(String errandId, String id);

	long deleteByErrandId(String errandId);
}
