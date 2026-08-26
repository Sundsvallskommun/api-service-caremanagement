package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaJobStimulusPeriodEntity;

@CircuitBreaker(name = "faJobStimulusPeriodRepository")
public interface FaJobStimulusPeriodRepository extends JpaRepository<FaJobStimulusPeriodEntity, String> {

	List<FaJobStimulusPeriodEntity> findByErrandIdOrderByFromDateAsc(String errandId);

	void deleteByErrandId(String errandId);
}
