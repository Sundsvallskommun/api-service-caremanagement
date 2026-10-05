package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaWarningEntity;

@CircuitBreaker(name = "financialAssistanceWarningRepository")
public interface FaWarningRepository extends JpaRepository<FaWarningEntity, String> {

	List<FaWarningEntity> findByErrandId(String errandId);

	long countByErrandIdAndStatusNot(String errandId, String status);

	Optional<FaWarningEntity> findByIdAndErrandId(String id, String errandId);

	/**
	 * Idempotently records one OPEN warning. {@code INSERT IGNORE} turns the {@code (errand_id, type, source_key)} unique
	 * collision into a no-op instead of a {@code DataIntegrityViolationException}, so two reconciles of the same errand
	 * running at once (two concurrent reads of a section proposal, or a read racing the daily prepare) raise the warning
	 * once rather than twice. {@code now} is bound as a UTC {@link LocalDateTime} to match the entity's {@code NORMALIZE}
	 * timezone storage.
	 */
	@Modifying
	@Query(value = """
		insert ignore into errand_financial_assistance_warning (id, errand_id, type, source_key, message, status, auto_resolved, created, updated)
		values (:id, :errandId, :type, :sourceKey, :message, :status, false, :now, :now)
		""", nativeQuery = true)
	void insertIgnore(@Param("id") String id, @Param("errandId") String errandId, @Param("type") String type, @Param("sourceKey") String sourceKey,
		@Param("message") String message, @Param("status") String status, @Param("now") LocalDateTime now);
}
