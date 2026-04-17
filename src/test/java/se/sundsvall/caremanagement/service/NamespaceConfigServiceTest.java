package se.sundsvall.caremanagement.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.api.model.NamespaceConfig;
import se.sundsvall.caremanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.caremanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class NamespaceConfigServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";

	@Mock
	private NamespaceConfigRepository repositoryMock;

	@Captor
	private ArgumentCaptor<NamespaceConfigEntity> entityCaptor;

	@InjectMocks
	private NamespaceConfigService service;

	@Test
	void create() {
		final var config = NamespaceConfig.create().withDisplayName("display").withShortCode("sc");
		when(repositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);
		when(repositoryMock.save(any(NamespaceConfigEntity.class)))
			.thenAnswer(inv -> ((NamespaceConfigEntity) inv.getArgument(0)).withId(42L));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, config);

		assertThat(result).isEqualTo(42L);
		verify(repositoryMock).existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(entityCaptor.getValue().getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(entityCaptor.getValue().getDisplayName()).isEqualTo("display");
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void createConflict() {
		when(repositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, NamespaceConfig.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(repositoryMock).existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void read() {
		final var entity = NamespaceConfigEntity.create().withId(1L).withDisplayName("d");
		when(repositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getDisplayName()).isEqualTo("d");
	}

	@Test
	void readNotFound() {
		when(repositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void update() {
		final var entity = NamespaceConfigEntity.create().withId(1L).withDisplayName("old");
		when(repositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.of(entity));

		service.update(MUNICIPALITY_ID, NAMESPACE, NamespaceConfig.create().withDisplayName("new").withShortCode("sc"));

		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getDisplayName()).isEqualTo("new");
		assertThat(entityCaptor.getValue().getShortCode()).isEqualTo("sc");
	}

	@Test
	void updateNotFound() {
		when(repositoryMock.findByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, NamespaceConfig.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void delete() {
		when(repositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		service.delete(MUNICIPALITY_ID, NAMESPACE);

		verify(repositoryMock).deleteByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void deleteNotFound() {
		when(repositoryMock.existsByNamespaceAndMunicipalityId(NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).deleteByNamespaceAndMunicipalityId(any(), any());
	}
}
