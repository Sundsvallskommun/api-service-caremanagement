package se.sundsvall.caremanagement.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.api.model.Lookup;
import se.sundsvall.caremanagement.integration.db.LookupRepository;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.CATEGORY;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.CONTACT_REASON;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.STATUS;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";
	private static final String NAME = "NEW";

	@Mock
	private LookupRepository repositoryMock;

	@Captor
	private ArgumentCaptor<LookupEntity> entityCaptor;

	@InjectMocks
	private MetadataService service;

	@Test
	void create() {
		final var lookup = Lookup.create().withName(NAME).withDisplayName("New case");
		when(repositoryMock.existsByKindAndNamespaceAndMunicipalityIdAndName(STATUS, NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(false);
		when(repositoryMock.save(any(LookupEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, STATUS, lookup);

		assertThat(result).isEqualTo(NAME);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getKind()).isEqualTo(STATUS);
		assertThat(entityCaptor.getValue().getName()).isEqualTo(NAME);
		assertThat(entityCaptor.getValue().getDisplayName()).isEqualTo("New case");
	}

	@Test
	void createConflict() {
		when(repositoryMock.existsByKindAndNamespaceAndMunicipalityIdAndName(STATUS, NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(true);

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, STATUS, Lookup.create().withName(NAME)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void createConflict_contactReasonKindLabel() {
		when(repositoryMock.existsByKindAndNamespaceAndMunicipalityIdAndName(CONTACT_REASON, NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(true);

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, CONTACT_REASON, Lookup.create().withName(NAME)))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("contact reason");
	}

	@Test
	void read() {
		final var entity = LookupEntity.create().withKind(CATEGORY).withName(NAME).withDisplayName("d");
		when(repositoryMock.findByKindAndNamespaceAndMunicipalityIdAndName(CATEGORY, NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(Optional.of(entity));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, CATEGORY, NAME);

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo(NAME);
		assertThat(result.getDisplayName()).isEqualTo("d");
	}

	@Test
	void readNotFound() {
		when(repositoryMock.findByKindAndNamespaceAndMunicipalityIdAndName(CATEGORY, NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, CATEGORY, NAME))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readAll() {
		when(repositoryMock.findAllByKindAndNamespaceAndMunicipalityId(STATUS, NAMESPACE, MUNICIPALITY_ID)).thenReturn(List.of(
			LookupEntity.create().withName("A"),
			LookupEntity.create().withName("B")));

		final var result = service.readAll(MUNICIPALITY_ID, NAMESPACE, STATUS);

		assertThat(result).hasSize(2);
	}

	@Test
	void update() {
		final var entity = LookupEntity.create().withKind(STATUS).withName(NAME).withDisplayName("old");
		when(repositoryMock.findByKindAndNamespaceAndMunicipalityIdAndName(STATUS, NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(Optional.of(entity));

		service.update(MUNICIPALITY_ID, NAMESPACE, STATUS, NAME, Lookup.create().withDisplayName("new"));

		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getDisplayName()).isEqualTo("new");
	}

	@Test
	void updateNotFound() {
		when(repositoryMock.findByKindAndNamespaceAndMunicipalityIdAndName(STATUS, NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, STATUS, NAME, Lookup.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void delete() {
		when(repositoryMock.existsByKindAndNamespaceAndMunicipalityIdAndName(STATUS, NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(true);

		service.delete(MUNICIPALITY_ID, NAMESPACE, STATUS, NAME);

		verify(repositoryMock).deleteByKindAndNamespaceAndMunicipalityIdAndName(STATUS, NAMESPACE, MUNICIPALITY_ID, NAME);
	}

	@Test
	void deleteNotFound() {
		when(repositoryMock.existsByKindAndNamespaceAndMunicipalityIdAndName(STATUS, NAMESPACE, MUNICIPALITY_ID, NAME)).thenReturn(false);

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, STATUS, NAME))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).deleteByKindAndNamespaceAndMunicipalityIdAndName(any(LookupKind.class), any(), any(), any());
	}
}
