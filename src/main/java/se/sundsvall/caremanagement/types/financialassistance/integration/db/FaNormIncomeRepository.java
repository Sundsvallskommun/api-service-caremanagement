package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;

@CircuitBreaker(name = "financialAssistanceNormIncomeRepository")
public interface FaNormIncomeRepository extends JpaRepository<FaNormIncomeEntity, String> {

	List<FaNormIncomeEntity> findByErrandId(String errandId);

	Optional<FaNormIncomeEntity> findByIdAndErrandId(String id, String errandId);

	@Query("select coalesce(max(e.position), -1) + 1 from FaNormIncomeEntity e where e.errandId = :errandId")
	int nextPositionForErrand(@Param("errandId") String errandId);
}
