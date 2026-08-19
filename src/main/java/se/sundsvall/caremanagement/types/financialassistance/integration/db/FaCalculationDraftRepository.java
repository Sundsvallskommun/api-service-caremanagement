package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationDraftEntity;

@CircuitBreaker(name = "financialAssistanceCalculationDraftRepository")
public interface FaCalculationDraftRepository extends JpaRepository<FaCalculationDraftEntity, String> {
}
