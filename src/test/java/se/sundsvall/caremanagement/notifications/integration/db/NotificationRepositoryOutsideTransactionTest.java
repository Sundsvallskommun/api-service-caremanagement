package se.sundsvall.caremanagement.notifications.integration.db;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationEntity;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

/**
 * The nightly cleanup calls {@link NotificationRepository#deleteByExpiresBefore} from a scheduler, with no transaction
 * of its own. Spring Data gives a derived delete no transaction either, so the repository method has to carry one —
 * which a {@code @DataJpaTest} cannot show while it wraps every test in a transaction. This class runs without one,
 * commits its setup and removes it afterwards.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Transactional(propagation = NOT_SUPPORTED)
class NotificationRepositoryOutsideTransactionTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "33333333-3333-3333-3333-333333333333";

	@Autowired
	private NotificationRepository notificationRepository;

	@AfterEach
	void tearDown() {
		notificationRepository.deleteAll(notificationRepository.findAllByNamespaceAndMunicipalityIdAndErrandId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, Sort.unsorted()));
	}

	@Test
	void deleteByExpiresBeforeRemovesExpiredNotificationsWithoutACallerTransaction() {
		final var cutoff = OffsetDateTime.now();
		final var expired = notificationRepository.save(notification("Expired yesterday", cutoff.minusDays(1)));
		final var current = notificationRepository.save(notification("Expires tomorrow", cutoff.plusDays(1)));

		final var deleted = notificationRepository.deleteByExpiresBefore(cutoff);

		assertThat(deleted).isPositive();
		assertThat(notificationRepository.existsById(expired.getId())).isFalse();
		assertThat(notificationRepository.existsById(current.getId())).isTrue();
	}

	private static NotificationEntity notification(final String description, final OffsetDateTime expires) {
		return NotificationEntity.create()
			.withErrandId(ERRAND_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withNamespace(NAMESPACE)
			.withOwnerId("assignee1")
			.withType(NotificationType.CREATE)
			.withDescription(description)
			.withAcknowledged(false)
			.withHandled(false)
			.withExpires(expires);
	}
}
