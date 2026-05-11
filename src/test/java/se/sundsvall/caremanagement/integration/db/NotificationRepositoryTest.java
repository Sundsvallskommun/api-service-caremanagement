package se.sundsvall.caremanagement.integration.db;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.NotificationEntity;
import se.sundsvall.caremanagement.integration.db.model.NotificationSubType;
import se.sundsvall.caremanagement.integration.db.model.NotificationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
class NotificationRepositoryTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private ErrandRepository errandRepository;

	private ErrandEntity errand;

	@BeforeEach
	void setUp() {
		notificationRepository.deleteAll();
		errandRepository.deleteAll();
		errand = errandRepository.save(ErrandEntity.create().withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE).withTitle("t"));
	}

	@Test
	void persistPopulatesCreatedAndModifiedViaListener() {
		final var saved = notificationRepository.saveAndFlush(buildNotification("o1", false));

		assertThat(saved.getCreated()).isNotNull();
		assertThat(saved.getModified()).isNull();

		saved.setDescription("changed");
		final var updated = notificationRepository.saveAndFlush(saved);
		assertThat(updated.getModified()).isNotNull();
	}

	@Test
	void findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId() {
		final var saved = notificationRepository.save(buildNotification("o1", false));

		final var result = notificationRepository.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(saved.getId(), NAMESPACE, MUNICIPALITY_ID, errand.getId());

		assertThat(result).isPresent();
		assertThat(result.get().getOwnerId()).isEqualTo("o1");
	}

	@Test
	void findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId_wrongNamespaceReturnsEmpty() {
		final var saved = notificationRepository.save(buildNotification("o1", false));

		assertThat(notificationRepository.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(saved.getId(), "other", MUNICIPALITY_ID, errand.getId())).isEmpty();
	}

	@Test
	void findAllByNamespaceAndMunicipalityIdAndErrandEntityId() {
		notificationRepository.save(buildNotification("o1", false));
		notificationRepository.save(buildNotification("o2", true));

		final var result = notificationRepository.findAllByNamespaceAndMunicipalityIdAndErrandEntityId(NAMESPACE, MUNICIPALITY_ID, errand.getId(), Sort.unsorted());

		assertThat(result).hasSize(2);
	}

	@Test
	void findAllByNamespaceAndMunicipalityIdAndOwnerId() {
		notificationRepository.save(buildNotification("o1", false));
		notificationRepository.save(buildNotification("o1", true));
		notificationRepository.save(buildNotification("o2", false));

		final var result = notificationRepository.findAllByNamespaceAndMunicipalityIdAndOwnerId(NAMESPACE, MUNICIPALITY_ID, "o1", Sort.unsorted());

		assertThat(result).hasSize(2);
		assertThat(result).allMatch(n -> "o1".equals(n.getOwnerId()));
	}

	@Test
	void acknowledgeAllByErrand_flipsUnacknowledgedOnly() {
		notificationRepository.save(buildNotification("o1", false));
		notificationRepository.save(buildNotification("o2", false));
		notificationRepository.save(buildNotification("o3", true));

		final var flipped = notificationRepository.acknowledgeAllByErrand(NAMESPACE, MUNICIPALITY_ID, errand.getId());

		assertThat(flipped).isEqualTo(2);
		assertThat(notificationRepository.findAll()).allMatch(NotificationEntity::isAcknowledged);
	}

	@Test
	void deleteByExpiresBefore_removesOnlyExpired() {
		final var pastExpire = OffsetDateTime.now(ZoneId.systemDefault()).minusDays(1);
		final var futureExpire = OffsetDateTime.now(ZoneId.systemDefault()).plusDays(30);

		notificationRepository.save(buildNotification("o1", false).withExpires(pastExpire));
		notificationRepository.save(buildNotification("o2", false).withExpires(futureExpire));

		final var deleted = notificationRepository.deleteByExpiresBefore(OffsetDateTime.now(ZoneId.systemDefault()));

		assertThat(deleted).isEqualTo(1);
		assertThat(notificationRepository.findAll()).hasSize(1).first().extracting(NotificationEntity::getOwnerId).isEqualTo("o2");
	}

	private NotificationEntity buildNotification(final String ownerId, final boolean acknowledged) {
		return NotificationEntity.create()
			.withErrandEntity(errand)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withOwnerId(ownerId)
			.withType(NotificationType.CREATE)
			.withSubType(NotificationSubType.ERRAND)
			.withDescription("desc")
			.withAcknowledged(acknowledged)
			.withExpires(OffsetDateTime.now(ZoneId.systemDefault()).plusDays(30));
	}
}
