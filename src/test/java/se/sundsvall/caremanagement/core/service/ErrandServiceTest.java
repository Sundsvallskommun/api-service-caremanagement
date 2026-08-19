package se.sundsvall.caremanagement.core.service;

import java.time.OffsetDateTime;
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
import se.sundsvall.caremanagement.core.service.registry.ErrandTypeRegistry;
import se.sundsvall.caremanagement.shared.NotificationRequest;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ErrandServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";

	private static final String ERRAND_NUMBER = "EB-26060001";

	@Mock
	private ErrandRepository repositoryMock;

	@Mock
	private ErrandNumberGenerator errandNumberGeneratorMock;

	@Mock
	private ApplicationEventPublisher eventPublisherMock;

	@Mock
	private ErrandNotificationFilter errandNotificationFilterMock;

	@Mock
	private ErrandTypeRegistry errandTypeRegistryMock;

	@InjectMocks
	private ErrandService service;

	@Test
	void createPublishesErrandCreatedAndAssignmentNotificationAndAssignsGeneratedNumber() {
		when(errandNumberGeneratorMock.generate(MUNICIPALITY_ID, NAMESPACE)).thenReturn(ERRAND_NUMBER);
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));

		final var id = service.createErrand(MUNICIPALITY_ID, NAMESPACE,
			Errand.create().withTypeSlug("t").withReporterUserId("reporter").withAssignedUserId("assignee"));

		assertThat(id).isEqualTo(ERRAND_ID);
		final var entityCaptor = ArgumentCaptor.forClass(ErrandEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		verify(eventPublisherMock).publishEvent(any(ErrandCreated.class));
		verify(eventPublisherMock).publishEvent(any(NotificationRequest.class));
	}

	@Test
	void createWithoutAssigneeSkipsAssignmentNotification() {
		when(errandNumberGeneratorMock.generate(MUNICIPALITY_ID, NAMESPACE)).thenReturn(ERRAND_NUMBER);
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));

		service.createErrand(MUNICIPALITY_ID, NAMESPACE, Errand.create().withTypeSlug("t").withReporterUserId("r"));

		verify(eventPublisherMock).publishEvent(any(ErrandCreated.class));
		verify(eventPublisherMock, never()).publishEvent(any(NotificationRequest.class));
	}

	@Test
	void createWhenReporterIsAssigneeSkipsAssignmentNotification() {
		when(errandNumberGeneratorMock.generate(MUNICIPALITY_ID, NAMESPACE)).thenReturn(ERRAND_NUMBER);
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));

		service.createErrand(MUNICIPALITY_ID, NAMESPACE,
			Errand.create().withTypeSlug("t").withReporterUserId("u").withAssignedUserId("u"));

		verify(eventPublisherMock).publishEvent(any(ErrandCreated.class));
		verify(eventPublisherMock, never()).publishEvent(any(NotificationRequest.class));
	}

	@Test
	void createErrandRejectsTypedCreateOnlySlug() {
		when(errandTypeRegistryMock.requiresTypedCreate("financial-assistance-renewal")).thenReturn(true);

		assertThatThrownBy(() -> service.createErrand(MUNICIPALITY_ID, NAMESPACE,
			Errand.create().withTypeSlug("financial-assistance-renewal")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: Errands of type 'financial-assistance-renewal' must be created via the type's own endpoint (which seeds the type data and starts its process), not the generic POST /errands");

		verifyNoInteractions(repositoryMock, eventPublisherMock);
	}

	@Test
	void createTypedErrandBypassesTheGuard() {
		when(errandNumberGeneratorMock.generate(MUNICIPALITY_ID, NAMESPACE)).thenReturn(ERRAND_NUMBER);
		when(repositoryMock.save(any(ErrandEntity.class))).thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));

		final var id = service.createTypedErrand(MUNICIPALITY_ID, NAMESPACE,
			Errand.create().withTypeSlug("financial-assistance-renewal").withReporterUserId("r"));

		assertThat(id).isEqualTo(ERRAND_ID);
		verify(eventPublisherMock).publishEvent(any(ErrandCreated.class));
		verifyNoInteractions(errandTypeRegistryMock);
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
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No errand with id '11111111-1111-1111-1111-111111111111' found in namespace 'MY_NAMESPACE' for municipality id '2281'");
	}

	@Test
	void findErrandsCombinesProvidedFilter() {
		final ArgumentCaptor<Specification<ErrandEntity>> specCaptor = ArgumentCaptor.captor();
		when(repositoryMock.findAll(specCaptor.capture(), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(of(ErrandEntity.create().withId(ERRAND_ID))));

		final Specification<ErrandEntity> extra = (root, _, cb) -> cb.equal(root.get("status"), "OPEN");
		final var page = service.findErrands(MUNICIPALITY_ID, NAMESPACE, extra, false, null, PageRequest.of(0, 10));

		assertThat(page.getErrands()).hasSize(1);
		assertThat(specCaptor.getValue()).isNotNull();
		verifyNoInteractions(errandNotificationFilterMock);
	}

	@Test
	void findErrandsWithNullFilterStillRuns() {
		when(repositoryMock.findAll(any(Specification.class), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(of()));

		final var page = service.findErrands(MUNICIPALITY_ID, NAMESPACE, null, false, null, PageRequest.of(0, 10));
		assertThat(page.getErrands()).isEmpty();
	}

	@Test
	void findErrandsAppliesUnacknowledgedNotificationFilter() {
		when(repositoryMock.findAll(any(Specification.class), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(of(ErrandEntity.create().withId(ERRAND_ID))));
		when(errandNotificationFilterMock.hasUnacknowledgedNotifications(MUNICIPALITY_ID, NAMESPACE, "jane01doe"))
			.thenReturn((root, _, cb) -> cb.conjunction());

		final var page = service.findErrands(MUNICIPALITY_ID, NAMESPACE, null, true, "jane01doe", PageRequest.of(0, 10));

		assertThat(page.getErrands()).hasSize(1);
		verify(errandNotificationFilterMock).hasUnacknowledgedNotifications(MUNICIPALITY_ID, NAMESPACE, "jane01doe");
	}

	@Test
	void countErrandsCombinesProvidedFilter() {
		final ArgumentCaptor<Specification<ErrandEntity>> specCaptor = ArgumentCaptor.captor();
		when(repositoryMock.count(specCaptor.capture())).thenReturn(7L);

		final Specification<ErrandEntity> extra = (root, _, cb) -> cb.equal(root.get("status"), "OPEN");
		final var count = service.countErrands(MUNICIPALITY_ID, NAMESPACE, extra);

		assertThat(count).isEqualTo(7L);
		assertThat(specCaptor.getValue()).isNotNull();
	}

	@Test
	void countErrandsWithNullFilterStillRuns() {
		when(repositoryMock.count(any(Specification.class))).thenReturn(0L);

		final var count = service.countErrands(MUNICIPALITY_ID, NAMESPACE, null);

		assertThat(count).isZero();
	}

	@Test
	void findByStatusTouchedBeforeMapsEntities() {
		final var cutoff = OffsetDateTime.parse("2026-05-01T00:00:00Z");
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withErrandNumber(ERRAND_NUMBER).withStatus("CLOSED")
			.withMunicipalityId(MUNICIPALITY_ID).withNamespace(NAMESPACE);
		when(repositoryMock.findByMunicipalityIdAndNamespaceAndStatusAndTouchedLessThanEqual(MUNICIPALITY_ID, NAMESPACE, "CLOSED", cutoff))
			.thenReturn(of(entity));

		final var result = service.findByStatusTouchedBefore(MUNICIPALITY_ID, NAMESPACE, "CLOSED", cutoff);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo(ERRAND_ID);
		assertThat(result.getFirst().getErrandNumber()).isEqualTo(ERRAND_NUMBER);
		verify(repositoryMock).findByMunicipalityIdAndNamespaceAndStatusAndTouchedLessThanEqual(MUNICIPALITY_ID, NAMESPACE, "CLOSED", cutoff);
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
		verify(eventPublisherMock).publishEvent(any(ErrandDeleted.class));
	}

	@Test
	void deleteMissingThrows() {
		when(repositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No errand with id '11111111-1111-1111-1111-111111111111' found in namespace 'MY_NAMESPACE' for municipality id '2281'");
	}

	@Test
	void linkProcessInstanceStoresIdAndPublishesNothing() {
		when(repositoryMock.updateProcessInstanceId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "proc-1")).thenReturn(1);

		service.linkProcessInstance(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "proc-1");

		verify(repositoryMock).updateProcessInstanceId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "proc-1");
		verify(eventPublisherMock, never()).publishEvent(any());
	}

	@Test
	void linkProcessInstanceMissingThrows() {
		when(repositoryMock.updateProcessInstanceId(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "proc-1")).thenReturn(0);

		assertThatThrownBy(() -> service.linkProcessInstance(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "proc-1"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No errand with id '11111111-1111-1111-1111-111111111111' found in namespace 'MY_NAMESPACE' for municipality id '2281'");
	}

	@Test
	void updateApplicantNameDelegatesTargetedUpdateAndPublishesNothing() {
		service.updateApplicantName(ERRAND_ID, "Anna Andersson");

		verify(repositoryMock).updateApplicantName(ERRAND_ID, "Anna Andersson");
		verify(eventPublisherMock, never()).publishEvent(any());
	}

	@Test
	void verifyExistingErrandDoesNothingWhenErrandExists() {
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		service.verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verify(repositoryMock).existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
		verify(eventPublisherMock, never()).publishEvent(any());
	}

	@Test
	void verifyExistingErrandThrowsNotFoundWhenErrandMissing() {
		when(repositoryMock.existsByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No errand with id '11111111-1111-1111-1111-111111111111' found in namespace 'MY_NAMESPACE' for municipality id '2281'");
	}
}
