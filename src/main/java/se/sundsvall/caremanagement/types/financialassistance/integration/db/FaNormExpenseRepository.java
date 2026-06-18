package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;

@CircuitBreaker(name = "financialAssistanceNormExpenseRepository")
public interface FaNormExpenseRepository extends JpaRepository<FaNormExpenseEntity, String> {

	List<FaNormExpenseEntity> findByErrandId(String errandId);

	Optional<FaNormExpenseEntity> findByIdAndErrandId(String id, String errandId);
}
