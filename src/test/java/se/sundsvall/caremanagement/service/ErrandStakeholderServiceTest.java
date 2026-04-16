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
import se.sundsvall.caremanagement.api.model.Stakeholder;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.StakeholderRepository;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ErrandStakeholderServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";
	private static final String ERRAND_ID = "eid";
	private static final String STAKEHOLDER_ID = "sid";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private StakeholderRepository stakeholderRepositoryMock;

	@Captor
	private ArgumentCaptor<StakeholderEntity> stakeholderCaptor;

	@Captor
	private ArgumentCaptor<ErrandEntity> errandCaptor;

	@InjectMocks
	private ErrandStakeholderService service;

	@Test
	void create() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(stakeholderRepositoryMock.save(any(StakeholderEntity.class)))
			.thenAnswer(inv -> ((StakeholderEntity) inv.getArgument(0)).withId(STAKEHOLDER_ID));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Stakeholder.create().withRole("APPLICANT"));

		assertThat(result).isEqualTo(STAKEHOLDER_ID);
		verify(stakeholderRepositoryMock).save(stakeholderCaptor.capture());
		assertThat(stakeholderCaptor.getValue().getErrandEntity()).isSameAs(errand);
		assertThat(stakeholderCaptor.getValue().getRole()).isEqualTo("APPLICANT");
	}

	@Test
	void create_errandNotFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Stakeholder.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(stakeholderRepositoryMock, never()).save(any());
	}

	@Test
	void read() {
		final var stakeholder = StakeholderEntity.create().withId(STAKEHOLDER_ID).withRole("APPLICANT");
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>(List.of(stakeholder)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(STAKEHOLDER_ID);
		assertThat(result.getRole()).isEqualTo("APPLICANT");
	}

	@Test
	void read_stakeholderNotFound() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>());
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readAll() {
		final var errand = ErrandEntity.create().withStakeholders(new ArrayList<>(List.of(
			StakeholderEntity.create().withId("a"),
			StakeholderEntity.create().withId("b"))));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		final var result = service.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).hasSize(2);
	}

	@Test
	void update() {
		final var existing = StakeholderEntity.create().withId(STAKEHOLDER_ID).withRole("OLD");
		final var errand = ErrandEntity.create().withStakeholders(new ArrayList<>(List.of(existing)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID, Stakeholder.create().withRole("NEW"));

		verify(stakeholderRepositoryMock).save(stakeholderCaptor.capture());
		assertThat(stakeholderCaptor.getValue().getRole()).isEqualTo("NEW");
	}

	@Test
	void delete() {
		final var stakeholder = StakeholderEntity.create().withId(STAKEHOLDER_ID);
		final var stakeholders = new ArrayList<>(List.of(stakeholder));
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(stakeholders);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID);

		assertThat(errand.getStakeholders()).doesNotContain(stakeholder);
		verify(errandRepositoryMock).save(errandCaptor.capture());
		assertThat(errandCaptor.getValue()).isSameAs(errand);
	}

	@Test
	void delete_stakeholderNotFound() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withStakeholders(new ArrayList<>());
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, STAKEHOLDER_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(errandRepositoryMock, never()).save(any(ErrandEntity.class));
	}
}
