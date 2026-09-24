package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPaymentEntity;

@CircuitBreaker(name = "financialAssistancePaymentRepository")
public interface FaPaymentRepository extends JpaRepository<FaPaymentEntity, String> {

	List<FaPaymentEntity> findByErrandId(String errandId);

	long countByErrandId(String errandId);

	Optional<FaPaymentEntity> findByIdAndErrandId(String id, String errandId);

	Optional<FaPaymentEntity> findByErrandIdAndLifecareId(String errandId, String lifecareId);

	/**
	 * Which of the given Lifecare payment ids another errand's (pre-reference) payment rows already carry — those were
	 * registered for that errand's decision and can never be taken as this errand's.
	 */
	@Query("""
		select distinct p.lifecareId from FaPaymentEntity p
		where p.lifecareId in :lifecareIds and p.errandId <> :errandId
		""")
	List<String> findLifecareIdsOnOtherErrands(@Param("lifecareIds") Collection<String> lifecareIds, @Param("errandId") String errandId);
}
