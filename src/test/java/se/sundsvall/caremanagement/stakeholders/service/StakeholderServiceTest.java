package se.sundsvall.caremanagement.stakeholders.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.integration.db.StakeholderRepository;
import se.sundsvall.caremanagement.stakeholders.integration.db.model.StakeholderEntity;
import se.sundsvall.caremanagement.stakeholders.service.event.StakeholderMutated;
import se.sundsvall.dept44.problem.ThrowableProblem;

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
class StakeholderServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";
	private static final String STAKEHOLDER_ID = "22222222-2222-2222-2222-222222222222";
	private static final String TYPE_SLUG = "fostercare";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private StakeholderRepository stakeholderRepositoryMock;

	@Mock
	private StakeholderRoleRegistry roleRegistryMock;

	@Mock
	private ApplicationEventPublisher eventPublisherMock;

	@InjectMocks
	private StakeholderService service;

	@Test
	void createSavesAndPublishesStakeholderMutated() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(new ErrandEntity().withTypeSlug(TYPE_SLUG)));
		when(roleRegistryMock.knownTypes()).thenReturn(Set.of(TYPE_SLUG));
		when(roleRegistryMock.isValidRole(TYPE_SLUG, "APPLICANT")).thenReturn(true);
		when(stakeholderRepositoryMock.save(any(StakeholderEntity.class)))
			.thenAnswer(invocation -> ((StakeholderEntity) invocation.getArgument(0)).withId(STAKEHOLDER_ID));

		final var id = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			Stakeholder.create().withRole("APPLICANT").withFirstName("Anna").withLastName("Andersson"));

		assertThat(id).isEqualTo(STAKEHOLDER_ID);
		verify(stakeholderRepositoryMock).save(any(StakeholderEntity.class));
		verify(eventPublisherMock).publishEvent(new StakeholderMutated(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID));
	}

	@Test
	void createWithUnconstrainedTypeSkipsRoleValidation() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(new ErrandEntity().withTypeSlug("uncontributed")));
		when(roleRegistryMock.knownTypes()).thenReturn(Set.of(TYPE_SLUG));
		when(stakeholderRepositoryMock.save(any(StakeholderEntity.class)))
			.thenAnswer(invocation -> ((StakeholderEntity) invocation.getArgument(0)).withId(STAKEHOLDER_ID));

		final var id = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			Stakeholder.create().withRole("ANYTHING_GOES"));

		assertThat(id).isEqualTo(STAKEHOLDER_ID);
		verify(stakeholderRepositoryMock).save(any(StakeholderEntity.class));
		verify(eventPublisherMock).publishEvent(new StakeholderMutated(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID));
	}

	@Test
	void createWithInvalidRoleForTypeThrowsBadRequest() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(new ErrandEntity().withTypeSlug(TYPE_SLUG)));
		when(roleRegistryMock.knownTypes()).thenReturn(Set.of(TYPE_SLUG));
		when(roleRegistryMock.isValidRole(TYPE_SLUG, "BOGUS_ROLE")).thenReturn(false);

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			Stakeholder.create().withRole("BOGUS_ROLE")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessageContaining("BOGUS_ROLE")
			.hasMessageContaining(TYPE_SLUG);

		verifyNoInteractions(stakeholderRepositoryMock, eventPublisherMock);
	}

	@Test
	void createOnMissingErrandThrowsAndPublishesNothing() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Stakeholder.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(stakeholderRepositoryMock, eventPublisherMock);
	}

	@Test
	void updateSavesAndPublishesStakeholderMutated() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(new ErrandEntity().withTypeSlug(TYPE_SLUG)));
		when(roleRegistryMock.knownTypes()).thenReturn(Set.of(TYPE_SLUG));
		when(roleRegistryMock.isValidRole(TYPE_SLUG, "APPLICANT")).thenReturn(true);
		when(stakeholderRepositoryMock.findById(STAKEHOLDER_ID))
			.thenReturn(Optional.of(StakeholderEntity.create().withId(STAKEHOLDER_ID).withErrandId(ERRAND_ID)));

		service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID,
			Stakeholder.create().withRole("APPLICANT").withFirstName("Bertil"));

		verify(stakeholderRepositoryMock).save(any(StakeholderEntity.class));
		verify(eventPublisherMock).publishEvent(new StakeholderMutated(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID));
	}

	@Test
	void updateWithInvalidRoleForTypeThrowsBadRequest() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(new ErrandEntity().withTypeSlug(TYPE_SLUG)));
		when(roleRegistryMock.knownTypes()).thenReturn(Set.of(TYPE_SLUG));
		when(roleRegistryMock.isValidRole(TYPE_SLUG, "BOGUS_ROLE")).thenReturn(false);

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID,
			Stakeholder.create().withRole("BOGUS_ROLE")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessageContaining("BOGUS_ROLE")
			.hasMessageContaining(TYPE_SLUG);

		verify(stakeholderRepositoryMock, never()).save(any(StakeholderEntity.class));
		verifyNoInteractions(eventPublisherMock);
	}

	@Test
	void deleteRemovesAndPublishesStakeholderMutated() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(new ErrandEntity()));
		final var entity = StakeholderEntity.create().withId(STAKEHOLDER_ID).withErrandId(ERRAND_ID);
		when(stakeholderRepositoryMock.findById(STAKEHOLDER_ID)).thenReturn(Optional.of(entity));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID);

		verify(stakeholderRepositoryMock).delete(entity);
		verify(eventPublisherMock).publishEvent(new StakeholderMutated(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID));
	}

	@Test
	void readReturnsStakeholderWithoutPublishing() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(new ErrandEntity()));
		when(stakeholderRepositoryMock.findById(STAKEHOLDER_ID))
			.thenReturn(Optional.of(StakeholderEntity.create().withId(STAKEHOLDER_ID).withErrandId(ERRAND_ID).withRole("APPLICANT")));

		final var stakeholder = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID);

		assertThat(stakeholder.getId()).isEqualTo(STAKEHOLDER_ID);
		assertThat(stakeholder.getRole()).isEqualTo("APPLICANT");
		verifyNoInteractions(eventPublisherMock);
	}

	@Test
	void readAllReturnsStakeholdersWithoutPublishing() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(new ErrandEntity()));
		when(stakeholderRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(List.of(StakeholderEntity.create().withId(STAKEHOLDER_ID).withErrandId(ERRAND_ID).withRole("APPLICANT")));

		final var stakeholders = service.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(stakeholders).extracting(Stakeholder::getId).containsExactly(STAKEHOLDER_ID);
		verify(eventPublisherMock, never()).publishEvent(any());
	}
}
