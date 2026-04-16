package se.sundsvall.caremanagement.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.api.model.StakeholderParameter;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.StakeholderParameterRepository;
import se.sundsvall.caremanagement.integration.db.StakeholderRepository;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderParameterEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class StakeholderParameterServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";
	private static final String ERRAND_ID = "eid";
	private static final String STAKEHOLDER_ID = "sid";
	private static final Long PARAMETER_ID = 42L;

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private StakeholderRepository stakeholderRepositoryMock;

	@Mock
	private StakeholderParameterRepository parameterRepositoryMock;

	@Captor
	private ArgumentCaptor<StakeholderParameterEntity> parameterCaptor;

	@Captor
	private ArgumentCaptor<StakeholderEntity> stakeholderCaptor;

	@InjectMocks
	private StakeholderParameterService service;

	@Test
	void create() {
		final var stakeholder = StakeholderEntity.create().withId(STAKEHOLDER_ID);
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>(List.of(stakeholder)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(parameterRepositoryMock.save(any(StakeholderParameterEntity.class)))
			.thenAnswer(inv -> ((StakeholderParameterEntity) inv.getArgument(0)).withId(PARAMETER_ID));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, StakeholderParameter.create().withKey("k"));

		assertThat(result).isEqualTo(PARAMETER_ID);
		verify(parameterRepositoryMock).save(parameterCaptor.capture());
		assertThat(parameterCaptor.getValue().getStakeholderEntity()).isSameAs(stakeholder);
		assertThat(parameterCaptor.getValue().getKey()).isEqualTo("k");
	}

	@Test
	void create_errandNotFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, StakeholderParameter.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(parameterRepositoryMock, never()).save(any());
	}

	@Test
	void create_stakeholderNotFound() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>());
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, StakeholderParameter.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(parameterRepositoryMock, never()).save(any());
	}

	@Test
	void read() {
		final var parameter = StakeholderParameterEntity.create().withId(PARAMETER_ID).withKey("k");
		final var stakeholder = StakeholderEntity.create().withId(STAKEHOLDER_ID).withParameters(new ArrayList<>(List.of(parameter)));
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>(List.of(stakeholder)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, PARAMETER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(PARAMETER_ID);
		assertThat(result.getKey()).isEqualTo("k");
	}

	@Test
	void read_parameterNotFound() {
		final var stakeholder = StakeholderEntity.create().withId(STAKEHOLDER_ID).withParameters(new ArrayList<>());
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>(List.of(stakeholder)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, PARAMETER_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readAll() {
		final var stakeholder = StakeholderEntity.create().withId(STAKEHOLDER_ID).withParameters(new ArrayList<>(List.of(
			StakeholderParameterEntity.create().withId(1L),
			StakeholderParameterEntity.create().withId(2L))));
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>(List.of(stakeholder)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		final var result = service.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID);

		assertThat(result).hasSize(2);
	}

	@Test
	void update() {
		final var existing = StakeholderParameterEntity.create().withId(PARAMETER_ID).withKey("OLD");
		final var stakeholder = StakeholderEntity.create().withId(STAKEHOLDER_ID).withParameters(new ArrayList<>(List.of(existing)));
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>(List.of(stakeholder)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, PARAMETER_ID, StakeholderParameter.create().withKey("NEW"));

		verify(parameterRepositoryMock).save(parameterCaptor.capture());
		assertThat(parameterCaptor.getValue().getKey()).isEqualTo("NEW");
	}

	@Test
	void delete() {
		final var parameter = StakeholderParameterEntity.create().withId(PARAMETER_ID);
		final var parameters = new ArrayList<>(List.of(parameter));
		final var stakeholder = StakeholderEntity.create().withId(STAKEHOLDER_ID).withParameters(parameters);
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>(List.of(stakeholder)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, PARAMETER_ID);

		assertThat(stakeholder.getParameters()).doesNotContain(parameter);
		verify(stakeholderRepositoryMock).save(stakeholderCaptor.capture());
		assertThat(stakeholderCaptor.getValue()).isSameAs(stakeholder);
	}

	@Test
	void delete_parameterNotFound() {
		final var stakeholder = StakeholderEntity.create().withId(STAKEHOLDER_ID).withParameters(new ArrayList<>());
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>(List.of(stakeholder)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, PARAMETER_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(stakeholderRepositoryMock, never()).save(any(StakeholderEntity.class));
	}
}
