package se.sundsvall.caremanagement.core.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
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
	 * Errands of the given types that are in {@code status} and have no process instance linked, created within
	 * {@code createdFrom}..{@code createdTo} (both inclusive), oldest first. Backs the recovery of a process start that
	 * failed: an errand still in its first status with nothing linked, some time after it was created. Narrowed by the
	 * {@code (municipality_id, namespace, status)} index.
	 */
	@Query("""
		select e from ErrandEntity e
		where e.municipalityId = :municipalityId and e.namespace = :namespace and e.status = :status
			and e.typeSlug in :typeSlugs and e.processInstanceId is null
			and e.created between :createdFrom and :createdTo
		order by e.created
		""")
	List<ErrandEntity> findWithoutProcessInstance(@Param("municipalityId") String municipalityId, @Param("namespace") String namespace,
		@Param("typeSlugs") Collection<String> typeSlugs, @Param("status") String status,
		@Param("createdFrom") OffsetDateTime createdFrom, @Param("createdTo") OffsetDateTime createdTo);

	/**
	 * Sets the denormalized {@code applicant_name} read-model field. A targeted bulk update on purpose: it bypasses the
	 * {@code @PreUpdate} lifecycle callback so refreshing the applicant name does <b>not</b> bump {@code touched} (the
	 * default errand-list sort) — a stakeholder edit must not resurface an errand as recently touched.
	 */
	@Modifying(clearAutomatically = true)
	@Query("update ErrandEntity e set e.applicantName = :applicantName where e.id = :errandId")
	int updateApplicantName(@Param("errandId") String errandId, @Param("applicantName") String applicantName);

	/**
	 * Links the started Operaton process instance to the errand. A targeted bulk update on purpose: it avoids the
	 * load-then-save read-modify-write that races the async applicant-name / classification writers on the same errand
	 * row (MariaDB snapshot isolation, error 1020), the same reason {@link #updateApplicantName} is a targeted update.
	 * Tenant-scoped; returns the number of rows updated (0 = no such errand in the given namespace/municipality).
	 */
	@Modifying(clearAutomatically = true)
	@Query("update ErrandEntity e set e.processInstanceId = :processInstanceId where e.id = :errandId and e.namespace = :namespace and e.municipalityId = :municipalityId")
	int updateProcessInstanceId(@Param("municipalityId") String municipalityId, @Param("namespace") String namespace, @Param("errandId") String errandId, @Param("processInstanceId") String processInstanceId);
}
