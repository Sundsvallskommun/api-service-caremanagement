package se.sundsvall.caremanagement.stakeholders.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.stakeholders.integration.db.model.StakeholderEntity;

@CircuitBreaker(name = "stakeholderRepository")
public interface StakeholderRepository extends JpaRepository<StakeholderEntity, String> {

	List<StakeholderEntity> findByErrandId(String errandId);

	long deleteByErrandId(String errandId);
}
