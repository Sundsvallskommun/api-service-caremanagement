package se.sundsvall.caremanagement.decisions.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.decisions.integration.db.model.DecisionEntity;

@CircuitBreaker(name = "decisionRepository")
public interface DecisionRepository extends JpaRepository<DecisionEntity, String> {

	List<DecisionEntity> findByErrandIdOrderByCreatedDesc(String errandId);

	Optional<DecisionEntity> findByErrandIdAndId(String errandId, String id);

	long deleteByErrandId(String errandId);
}
