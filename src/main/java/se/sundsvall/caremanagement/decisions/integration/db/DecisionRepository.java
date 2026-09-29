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

	/** Whether a decision of the given type and value is recorded on an errand other than the given one. */
	boolean existsByDecisionTypeAndValueAndErrandIdNot(String decisionType, String value, String errandId);

	long deleteByErrandId(String errandId);
}
