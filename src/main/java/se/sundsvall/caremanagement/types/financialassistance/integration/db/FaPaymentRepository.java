package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPaymentEntity;

@CircuitBreaker(name = "financialAssistancePaymentRepository")
public interface FaPaymentRepository extends JpaRepository<FaPaymentEntity, String> {

	List<FaPaymentEntity> findByErrandId(String errandId);

	long countByErrandId(String errandId);

	Optional<FaPaymentEntity> findByIdAndErrandId(String id, String errandId);

	Optional<FaPaymentEntity> findByErrandIdAndLifecareId(String errandId, String lifecareId);
}
