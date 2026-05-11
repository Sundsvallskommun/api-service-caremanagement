package se.sundsvall.caremanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import se.sundsvall.caremanagement.integration.db.model.NotificationEntity;

@CircuitBreaker(name = "notificationRepository")
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

	Optional<NotificationEntity> findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(String id, String namespace, String municipalityId, String errandId);

	List<NotificationEntity> findAllByNamespaceAndMunicipalityIdAndErrandEntityId(String namespace, String municipalityId, String errandId, Sort sort);

	List<NotificationEntity> findAllByNamespaceAndMunicipalityIdAndOwnerId(String namespace, String municipalityId, String ownerId, Sort sort);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update NotificationEntity n set n.acknowledged = true where n.namespace = ?1 and n.municipalityId = ?2 and n.errandEntity.id = ?3 and n.acknowledged = false")
	int acknowledgeAllByErrand(String namespace, String municipalityId, String errandId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	long deleteByExpiresBefore(OffsetDateTime cutoff);
}
