package se.sundsvall.caremanagement.notifications.integration.db;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.caremanagement.cocaseworkers.integration.db.model.CoCaseworkerEntity;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationEntity;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Proves the cross-module widening in {@link NotificationRepository#findAllVisibleToUser} actually resolves at
 * runtime: {@code CoCaseworkerEntity} is referenced only by entity name in the JPQL string (no Java import), so
 * this is the test that would catch a rename/removal breaking that string reference silently.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
class NotificationRepositoryTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String OTHER_ERRAND_ID = "22222222-2222-2222-2222-222222222222";
	private static final String OWNER_ID = "assignee1";
	private static final String CO_CASEWORKER_ID = "coworker1";
	private static final String STRANGER_ID = "stranger1";

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		final var notification = NotificationEntity.create()
			.withErrandId(ERRAND_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withOwnerId(OWNER_ID)
			.withType(NotificationType.CREATE)
			.withDescription("New errand assigned to you")
			.withAcknowledged(false)
			.withHandled(false)
			.withExpires(OffsetDateTime.now().plusDays(1));
		notificationRepository.save(notification);

		final var otherErrandNotification = NotificationEntity.create()
			.withErrandId(OTHER_ERRAND_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withOwnerId(STRANGER_ID)
			.withType(NotificationType.CREATE)
			.withDescription("Not visible to the co-caseworker")
			.withAcknowledged(false)
			.withHandled(false)
			.withExpires(OffsetDateTime.now().plusDays(1));
		notificationRepository.save(otherErrandNotification);

		final var coCaseworker = CoCaseworkerEntity.create()
			.withErrandId(ERRAND_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withUserId(CO_CASEWORKER_ID)
			.withCreated(OffsetDateTime.now());
		entityManager.persist(coCaseworker);
		entityManager.flush();
	}

	@Test
	void findAllVisibleToUserReturnsDirectlyOwnedNotifications() {
		final var result = notificationRepository.findAllVisibleToUser(NAMESPACE, MUNICIPALITY_ID, OWNER_ID, Sort.unsorted());

		assertThat(result).extracting(NotificationEntity::getErrandId).containsExactly(ERRAND_ID);
	}

	@Test
	void findAllVisibleToUserWidensToCoCaseworkerErrands() {
		final var result = notificationRepository.findAllVisibleToUser(NAMESPACE, MUNICIPALITY_ID, CO_CASEWORKER_ID, Sort.unsorted());

		assertThat(result).extracting(NotificationEntity::getErrandId).containsExactly(ERRAND_ID);
	}

	@Test
	void findAllVisibleToUserExcludesUnrelatedUsers() {
		final var result = notificationRepository.findAllVisibleToUser(NAMESPACE, MUNICIPALITY_ID, "nobody", Sort.unsorted());

		assertThat(result).isEmpty();
	}
}
