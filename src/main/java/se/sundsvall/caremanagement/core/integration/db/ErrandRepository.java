package se.sundsvall.caremanagement.core.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

@CircuitBreaker(name = "errandRepository")
public interface ErrandRepository extends JpaRepository<ErrandEntity, String>, JpaSpecificationExecutor<ErrandEntity> {

	boolean existsByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	boolean existsWithLockingByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ErrandEntity> findWithLockingById(String id);

	Optional<ErrandEntity> findByIdAndNamespaceAndMunicipalityId(String id, String namespace, String municipalityId);

	/**
	 * Errands in the given status whose last envelope change ({@code touched}) is at or before the cutoff — i.e. that
	 * have been in that status, untouched, since before the cutoff. Backed by the
	 * {@code (municipality_id, namespace, status, touched)} index.
	 */
	List<ErrandEntity> findByMunicipalityIdAndNamespaceAndStatusAndTouchedLessThanEqual(String municipalityId, String namespace, String status, OffsetDateTime cutoff);

	/**
	 * Sets the denormalized {@code applicant_name} read-model field. A targeted bulk update on purpose: it bypasses the
	 * {@code @PreUpdate} lifecycle callback so refreshing the applicant name does <b>not</b> bump {@code touched} (the
	 * default errand-list sort) — a stakeholder edit must not resurface an errand as recently touched.
	 */
	@Modifying(clearAutomatically = true)
	@Query("update ErrandEntity e set e.applicantName = :applicantName where e.id = :errandId")
	int updateApplicantName(@Param("errandId") String errandId, @Param("applicantName") String applicantName);
}
