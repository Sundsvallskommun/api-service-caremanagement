package se.sundsvall.caremanagement.core.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.core.service.event.ErrandAssigned;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.shared.NotificationRequest;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ErrandServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private ErrandRepository repositoryMock;

	@Mock
	private ApplicationEventPublisher eventPublisherMock;

	@InjectMocks
	private ErrandService service;

	@Test
	void createPublishesErrandCreatedAndAssignmentNotification() {
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));

		final var id = service.createErrand(MUNICIPALITY_ID, NAMESPACE,
			Errand.create().withTypeSlug("t").withReporterUserId("reporter").withAssignedUserId("assignee"));

		assertThat(id).isEqualTo(ERRAND_ID);
		verify(eventPublisherMock).publishEvent(any(ErrandCreated.class));
		verify(eventPublisherMock).publishEvent(any(NotificationRequest.class));
	}

	@Test
	void createWithoutAssigneeSkipsAssignmentNotification() {
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));

		service.createErrand(MUNICIPALITY_ID, NAMESPACE, Errand.create().withTypeSlug("t").withReporterUserId("r"));

		verify(eventPublisherMock).publishEvent(any(ErrandCreated.class));
		verify(eventPublisherMock, never()).publishEvent(any(NotificationRequest.class));
	}

	@Test
	void createWhenReporterIsAssigneeSkipsAssignmentNotification() {
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));

		service.createErrand(MUNICIPALITY_ID, NAMESPACE,
			Errand.create().withTypeSlug("t").withReporterUserId("u").withAssignedUserId("u"));

		verify(eventPublisherMock).publishEvent(any(ErrandCreated.class));
		verify(eventPublisherMock, never()).publishEvent(any(NotificationRequest.class));
	}

	@Test
	void readReturnsMappedErrand() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withTypeSlug("t").withTitle("T");
		when(repositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));

		final var errand = service.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(errand.getId()).isEqualTo(ERRAND_ID);
		assertThat(errand.getTypeSlug()).isEqualTo("t");
		assertThat(errand.getTitle()).isEqualTo("T");
	}

	@Test
	void readMissingThrowsNotFound() {
		when(repositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void findErrandsCombinesProvidedFilter() {
		final ArgumentCaptor<Specification<ErrandEntity>> specCaptor = ArgumentCaptor.captor();
		when(repositoryMock.findAll(specCaptor.capture(), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(of(ErrandEntity.create().withId(ERRAND_ID))));

		final Specification<ErrandEntity> extra = (root, _, cb) -> cb.equal(root.get("status"), "OPEN");
		final var page = service.findErrands(MUNICIPALITY_ID, NAMESPACE, extra, PageRequest.of(0, 10));

		assertThat(page.getErrands()).hasSize(1);
		assertThat(specCaptor.getValue()).isNotNull();
	}

	@Test
	void findErrandsWithNullFilterStillRuns() {
		when(repositoryMock.findAll(any(Specification.class), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(of()));

		final var page = service.findErrands(MUNICIPALITY_ID, NAMESPACE, null, PageRequest.of(0, 10));
		assertThat(page.getErrands()).isEmpty();
	}

	@Test
	void updateEmitsStatusChangedAndAssignedAndNotificationWhenChanged() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withTypeSlug("t").withStatus("OPEN").withAssignedUserId("old").withReporterUserId("reporter");
		when(repositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		service.updateErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			PatchErrand.create().withStatus("CLOSED").withAssignedUserId("new"));

		verify(eventPublisherMock).publishEvent(any(ErrandStatusChanged.class));
		verify(eventPublisherMock).publishEvent(any(ErrandAssigned.class));
		verify(eventPublisherMock).publishEvent(any(NotificationRequest.class));
	}

	@Test
	void updateOnlyStatusEmitsNoAssignmentEvents() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withTypeSlug("t").withStatus("OPEN").withAssignedUserId("same");
		when(repositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		service.updateErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			PatchErrand.create().withStatus("CLOSED"));

		verify(eventPublisherMock).publishEvent(any(ErrandStatusChanged.class));
		verify(eventPublisherMock, never()).publishEvent(any(ErrandAssigned.class));
		verify(eventPublisherMock, never()).publishEvent(any(NotificationRequest.class));
	}

	@Test
	void updateWithoutChangesEmitsNothing() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withTypeSlug("t").withStatus("OPEN").withAssignedUserId("same");
		when(repositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		service.updateErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PatchErrand.create());

		verify(eventPublisherMock, never()).publishEvent(any());
	}

	@Test
	void updateAssignmentSameAsReporterDoesNotPublishNotification() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withTypeSlug("t").withStatus("OPEN").withReporterUserId("u");
		when(repositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		service.updateErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PatchErrand.create().withAssignedUserId("u"));

		verify(eventPublisherMock).publishEvent(any(ErrandAssigned.class));
		verify(eventPublisherMock, never()).publishEvent(any(NotificationRequest.class));
	}

	@Test
	void deletePublishesErrandDeleted() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withTypeSlug("t");
		when(repositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));

		service.deleteErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verify(repositoryMock).delete(entity);
		verify(eventPublisherMock, times(1)).publishEvent(any(ErrandDeleted.class));
	}

	@Test
	void deleteMissingThrows() {
		when(repositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}
}
