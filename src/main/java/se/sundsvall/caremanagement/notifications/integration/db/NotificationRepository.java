package se.sundsvall.caremanagement.notifications.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationEntity;

@CircuitBreaker(name = "notificationRepository")
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

	Optional<NotificationEntity> findByIdAndNamespaceAndMunicipalityIdAndErrandId(String id, String namespace, String municipalityId, String errandId);

	List<NotificationEntity> findAllByNamespaceAndMunicipalityIdAndErrandId(String namespace, String municipalityId, String errandId, Sort sort);

	/**
	 * Widens "notifications for this recipient" beyond direct ownership: also matches notifications on any errand
	 * where {@code userId} is a co-caseworker (see the {@code cocaseworkers} module). Referenced by entity name only
	 * (no Java import of {@code CoCaseworkerEntity}) to keep the module dependency out of the compiled graph — the two
	 * modules are coupled only through this JPQL string, resolved against the shared persistence unit at runtime.
	 * Notifications stay single rows: this only widens who can see one, never duplicates it.
	 */
	@Query("""
		select n from NotificationEntity n
		where n.namespace = ?1
		  and n.municipalityId = ?2
		  and (n.ownerId = ?3
		    or n.errandId in (
		      select c.errandId from CoCaseworkerEntity c
		      where c.namespace = ?1
		        and c.municipalityId = ?2
		        and c.userId = ?3
		    ))
		""")
	List<NotificationEntity> findAllVisibleToUser(String namespace, String municipalityId, String userId, Sort sort);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update NotificationEntity n set n.acknowledged = true where n.namespace = ?1 and n.municipalityId = ?2 and n.errandId = ?3 and n.acknowledged = false")
	int acknowledgeAllByErrand(String namespace, String municipalityId, String errandId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update NotificationEntity n set n.handled = true, n.acknowledged = true where n.namespace = ?1 and n.municipalityId = ?2 and n.errandId = ?3 and n.handled = false")
	int handleAllByErrand(String namespace, String municipalityId, String errandId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update NotificationEntity n set n.ownerId = ?4 where n.namespace = ?1 and n.municipalityId = ?2 and n.errandId = ?3 and (n.ownerId is null or n.ownerId = '')")
	int assignOwnerToUnowned(String namespace, String municipalityId, String errandId, String ownerId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	long deleteByExpiresBefore(OffsetDateTime cutoff);
}
