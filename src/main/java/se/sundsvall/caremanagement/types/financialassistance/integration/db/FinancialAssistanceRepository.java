package se.sundsvall.caremanagement.types.financialassistance.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

@CircuitBreaker(name = "financialAssistanceRepository")
public interface FinancialAssistanceRepository extends JpaRepository<FinancialAssistanceEntity, String> {

	Optional<FinancialAssistanceEntity> findByErrandId(String errandId);

	/**
	 * Same lookup as {@link #findByErrandId(String)}, but with a pessimistic write lock on the errand's row. Used by
	 * finalizeErrand() to make its "not already finalized" check-then-act atomic: two concurrent finalizeErrand calls for
	 * the same errand must not both pass the check before either commits.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select fa from FinancialAssistanceEntity fa where fa.errandId = :errandId")
	Optional<FinancialAssistanceEntity> findByErrandIdForUpdate(@Param("errandId") String errandId);

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

	/**
	 * Which of the given Lifecare payment ids another errand already references ({@code lifecarePaymentIds}). A payment
	 * belongs to one decision, so these can never be taken as this errand's.
	 */
	@Query("""
		select distinct p from FinancialAssistanceEntity fa
		join fa.lifecarePaymentIds p
		where p in :lifecarePaymentIds and fa.errandId <> :errandId
		""")
	List<String> findLifecarePaymentIdsLinkedElsewhere(@Param("lifecarePaymentIds") Collection<String> lifecarePaymentIds, @Param("errandId") String errandId);

	/**
	 * Link a Lifecare calculation to the errand, but only when none is linked yet. The prepare step and the caseworker's
	 * first save through the errand's /lifecare calculation route can both create the calculation; whichever links first
	 * wins, and the other learns it lost from the {@code 0}.
	 *
	 * @return the number of rows updated — {@code 1} when this call linked the id, {@code 0} when one was already linked
	 */
	@Modifying
	@Transactional
	@Query("""
		update FinancialAssistanceEntity fa set fa.lifecareCalculationId = :lifecareCalculationId
		where fa.errandId = :errandId and fa.lifecareCalculationId is null
		""")
	int linkLifecareCalculationIfAbsent(@Param("errandId") String errandId, @Param("lifecareCalculationId") Integer lifecareCalculationId);

	/**
	 * Adds one Lifecare payment id to the errand's linked payments, atomically. {@code errand_fa_lifecare_payment} is
	 * keyed on {@code (errand_id, lifecare_payment_id)}, so {@code INSERT IGNORE} is a no-op — not an error — when the id
	 * is already linked. Deliberately not a read-modify-write of the whole {@code lifecarePaymentIds} list: two
	 * registrations for the same errand racing each other must not have one overwrite the other's link.
	 */
	@Modifying
	@Transactional
	@Query(value = "insert ignore into errand_fa_lifecare_payment (errand_id, lifecare_payment_id) values (:errandId, :paymentId)", nativeQuery = true)
	void linkPaymentIfAbsent(@Param("errandId") String errandId, @Param("paymentId") String paymentId);
}
