package se.sundsvall.caremanagement.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import se.sundsvall.caremanagement.api.model.Notification;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.NotificationRepository;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.NotificationEntity;
import se.sundsvall.caremanagement.integration.db.model.NotificationSubType;
import se.sundsvall.caremanagement.integration.db.model.NotificationType;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";
	private static final String ERRAND_ID = "eid";
	private static final String NOTIFICATION_ID = "nid";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private NotificationRepository notificationRepositoryMock;

	@Captor
	private ArgumentCaptor<NotificationEntity> entityCaptor;

	private NotificationService service;

	@BeforeEach
	void setUp() {
		service = new NotificationService(errandRepositoryMock, notificationRepositoryMock, new NotificationProperties(Duration.ofDays(30)));
	}

	@Test
	void create_savesAndReturnsId() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(notificationRepositoryMock.save(any(NotificationEntity.class)))
			.thenAnswer(inv -> ((NotificationEntity) inv.getArgument(0)).withId(NOTIFICATION_ID));

		final var notification = Notification.create()
			.withOwnerId("jane01doe")
			.withCreatedBy("john02doe")
			.withType("CREATE")
			.withSubType("ERRAND")
			.withDescription("hello");

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, notification);

		assertThat(result).isEqualTo(NOTIFICATION_ID);
		verify(notificationRepositoryMock).save(entityCaptor.capture());
		final var saved = entityCaptor.getValue();
		assertThat(saved.getOwnerId()).isEqualTo("jane01doe");
		assertThat(saved.getType()).isEqualTo(NotificationType.CREATE);
		assertThat(saved.getSubType()).isEqualTo(NotificationSubType.ERRAND);
		assertThat(saved.getExpires()).isAfter(OffsetDateTime.now().plusDays(29));
		assertThat(saved.isAcknowledged()).isFalse();
	}

	@Test
	void create_selfCreated_autoAcknowledges() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(notificationRepositoryMock.save(any(NotificationEntity.class)))
			.thenAnswer(inv -> ((NotificationEntity) inv.getArgument(0)).withId(NOTIFICATION_ID));

		service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Notification.create()
			.withOwnerId("jane01doe").withCreatedBy("jane01doe").withType("CREATE").withDescription("hi"));

		verify(notificationRepositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().isAcknowledged()).isTrue();
	}

	@Test
	void create_createdByDiffersInCase_doesNotAutoAcknowledge() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(notificationRepositoryMock.save(any(NotificationEntity.class)))
			.thenAnswer(inv -> ((NotificationEntity) inv.getArgument(0)).withId(NOTIFICATION_ID));

		service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Notification.create()
			.withOwnerId("jane01doe").withCreatedBy("Jane01Doe").withType("CREATE").withDescription("hi"));

		verify(notificationRepositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().isAcknowledged()).isFalse();
	}

	@Test
	void create_errandNotFound_throws404() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			Notification.create().withOwnerId("o").withType("CREATE").withDescription("d")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(notificationRepositoryMock);
	}

	@Test
	void read_returnsDto() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID);
		final var entity = NotificationEntity.create().withId(NOTIFICATION_ID).withErrandEntity(errand)
			.withType(NotificationType.CREATE).withDescription("d");
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(notificationRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(NOTIFICATION_ID, NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(Optional.of(entity));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTIFICATION_ID);

		assertThat(result.getId()).isEqualTo(NOTIFICATION_ID);
		assertThat(result.getType()).isEqualTo("CREATE");
	}

	@Test
	void read_notificationNotFound_throws404() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID)));
		when(notificationRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(NOTIFICATION_ID, NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTIFICATION_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readAllByErrand_listsAndMaps() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID)));
		when(notificationRepositoryMock.findAllByNamespaceAndMunicipalityIdAndErrandEntityId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, Sort.unsorted()))
			.thenReturn(List.of(NotificationEntity.create().withId("a"), NotificationEntity.create().withId("b")));

		final var result = service.readAllByErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Sort.unsorted());

		assertThat(result).extracting(Notification::getId).containsExactly("a", "b");
	}

	@Test
	void readAllByErrand_errandNotFound_throws404() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.readAllByErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Sort.unsorted()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readAllByOwner_doesNotRequireErrandLookup() {
		when(notificationRepositoryMock.findAllByNamespaceAndMunicipalityIdAndOwnerId(NAMESPACE, MUNICIPALITY_ID, "jane01doe", Sort.unsorted()))
			.thenReturn(List.of(NotificationEntity.create().withId("a")));

		final var result = service.readAllByOwner(MUNICIPALITY_ID, NAMESPACE, "jane01doe", Sort.unsorted());

		assertThat(result).hasSize(1);
		verifyNoInteractions(errandRepositoryMock);
	}

	@Test
	void update_appliesPatch() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID);
		final var entity = NotificationEntity.create().withId(NOTIFICATION_ID).withErrandEntity(errand).withAcknowledged(false);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(notificationRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(NOTIFICATION_ID, NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(Optional.of(entity));

		service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTIFICATION_ID, Notification.create().withAcknowledged(true));

		verify(notificationRepositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().isAcknowledged()).isTrue();
	}

	@Test
	void update_notificationNotFound_throws404() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID)));
		when(notificationRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(NOTIFICATION_ID, NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTIFICATION_ID, Notification.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(notificationRepositoryMock, never()).save(any());
	}

	@Test
	void delete_removesEntity() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID);
		final var entity = NotificationEntity.create().withId(NOTIFICATION_ID).withErrandEntity(errand);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(notificationRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(NOTIFICATION_ID, NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(Optional.of(entity));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTIFICATION_ID);

		verify(notificationRepositoryMock).delete(entity);
	}

	@Test
	void delete_notFound_throws404() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID)));
		when(notificationRepositoryMock.findByIdAndNamespaceAndMunicipalityIdAndErrandEntityId(NOTIFICATION_ID, NAMESPACE, MUNICIPALITY_ID, ERRAND_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTIFICATION_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(notificationRepositoryMock, never()).delete(any(NotificationEntity.class));
	}

	@Test
	void acknowledgeAll_returnsAffectedCount() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID)));
		when(notificationRepositoryMock.acknowledgeAllByErrand(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID)).thenReturn(7);

		final var result = service.acknowledgeAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).isEqualTo(7);
	}

	@Test
	void acknowledgeAll_errandNotFound_throws404() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.acknowledgeAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}
}
