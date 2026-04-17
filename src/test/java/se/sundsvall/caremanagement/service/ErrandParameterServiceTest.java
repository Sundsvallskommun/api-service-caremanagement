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
import se.sundsvall.caremanagement.api.model.Parameter;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.ParameterRepository;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.ParameterEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ErrandParameterServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";
	private static final String ERRAND_ID = "eid";
	private static final String PARAMETER_ID = "pid";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private ParameterRepository parameterRepositoryMock;

	@Captor
	private ArgumentCaptor<ParameterEntity> parameterCaptor;

	@Captor
	private ArgumentCaptor<ErrandEntity> errandCaptor;

	@InjectMocks
	private ErrandParameterService service;

	@Test
	void create() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));
		when(parameterRepositoryMock.save(any(ParameterEntity.class)))
			.thenAnswer(inv -> ((ParameterEntity) inv.getArgument(0)).withId(PARAMETER_ID));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Parameter.create().withKey("k"));

		assertThat(result).isEqualTo(PARAMETER_ID);
		verify(parameterRepositoryMock).save(parameterCaptor.capture());
		assertThat(parameterCaptor.getValue().getErrandEntity()).isSameAs(errand);
		assertThat(parameterCaptor.getValue().getKey()).isEqualTo("k");
	}

	@Test
	void create_errandNotFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Parameter.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(parameterRepositoryMock, never()).save(any());
	}

	@Test
	void read() {
		final var parameter = ParameterEntity.create().withId(PARAMETER_ID).withKey("k");
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withParameters(new ArrayList<>(List.of(parameter)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PARAMETER_ID);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(PARAMETER_ID);
		assertThat(result.getKey()).isEqualTo("k");
	}

	@Test
	void read_parameterNotFound() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withParameters(new ArrayList<>());
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PARAMETER_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readAll() {
		final var errand = ErrandEntity.create().withParameters(new ArrayList<>(List.of(
			ParameterEntity.create().withId("a"),
			ParameterEntity.create().withId("b"))));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		final var result = service.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).hasSize(2);
	}

	@Test
	void update() {
		final var existing = ParameterEntity.create().withId(PARAMETER_ID).withKey("OLD");
		final var errand = ErrandEntity.create().withParameters(new ArrayList<>(List.of(existing)));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PARAMETER_ID, Parameter.create().withKey("NEW"));

		verify(parameterRepositoryMock).save(parameterCaptor.capture());
		assertThat(parameterCaptor.getValue().getKey()).isEqualTo("NEW");
	}

	@Test
	void delete() {
		final var parameter = ParameterEntity.create().withId(PARAMETER_ID);
		final var parameters = new ArrayList<>(List.of(parameter));
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withParameters(parameters);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PARAMETER_ID);

		assertThat(errand.getParameters()).doesNotContain(parameter);
		verify(errandRepositoryMock).save(errandCaptor.capture());
		assertThat(errandCaptor.getValue()).isSameAs(errand);
	}

	@Test
	void delete_parameterNotFound() {
		final var errand = ErrandEntity.create().withId(ERRAND_ID).withParameters(new ArrayList<>());
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(errand));

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PARAMETER_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(errandRepositoryMock, never()).save(any(ErrandEntity.class));
	}
}
