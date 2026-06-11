package se.sundsvall.caremanagement.referral.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.referral.integration.db.model.ReferralEntity;

@CircuitBreaker(name = "referralRepository")
public interface ReferralRepository extends JpaRepository<ReferralEntity, String> {

	List<ReferralEntity> findByErrandIdOrderByCreatedDesc(String errandId);

	Optional<ReferralEntity> findByErrandIdAndId(String errandId, String id);

	long deleteByErrandId(String errandId);
}
