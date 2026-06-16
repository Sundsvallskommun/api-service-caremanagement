package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

@CircuitBreaker(name = "financialAssistanceRepository")
public interface FinancialAssistanceRepository extends JpaRepository<FinancialAssistanceEntity, String> {

	Optional<FinancialAssistanceEntity> findByErrandId(String errandId);

	/**
	 * Errand ids of every financial-assistance application where the given party appears in any role (applicant or
	 * co-applicant), regardless of period or age. Backs the eligibility check's "finns i CM?" existence gate and the
	 * per-month application lookup; results are scoped to the namespace/municipality and windowed by loading each errand
	 * envelope afterwards.
	 */
	@Query("""
		select distinct fa.errandId from FinancialAssistanceEntity fa
		join fa.persons p
		where p.partyId = :partyId
		""")
	List<String> findErrandIdsByPartyId(@Param("partyId") String partyId);
}
