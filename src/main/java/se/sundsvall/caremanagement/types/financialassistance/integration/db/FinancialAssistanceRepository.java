package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
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
	 * Errand ids of financial-assistance applications created on or after {@code createdAfter} where the given person
	 * appears in any role (applicant or co-applicant). Backs the "already applied within the window" duplicate guard —
	 * results are still scoped to the namespace/municipality by loading each errand envelope afterwards.
	 */
	@Query("""
		select distinct fa.errandId from FinancialAssistanceEntity fa
		join fa.persons p
		where p.personalNumber = :personalNumber and fa.created >= :createdAfter
		""")
	List<String> findRecentErrandIdsByPerson(@Param("personalNumber") String personalNumber, @Param("createdAfter") OffsetDateTime createdAfter);
}
