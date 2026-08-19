package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaSectionApprovalEntity;

@CircuitBreaker(name = "financialAssistanceSectionApprovalRepository")
public interface FaSectionApprovalRepository extends JpaRepository<FaSectionApprovalEntity, String> {

	List<FaSectionApprovalEntity> findByErrandId(String errandId);

	Optional<FaSectionApprovalEntity> findByErrandIdAndSection(String errandId, String section);
}
