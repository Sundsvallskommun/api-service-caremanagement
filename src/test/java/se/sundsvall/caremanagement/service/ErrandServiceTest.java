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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.caremanagement.api.model.Errand;
import se.sundsvall.caremanagement.api.model.PatchErrand;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.LookupRepository;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.CONTACT_REASON;

@ExtendWith(MockitoExtension.class)
class ErrandServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "ns";
	private static final String ERRAND_ID = "eid";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private LookupRepository lookupRepositoryMock;

	@Mock
	private ProcessService processServiceMock;

	@Captor
	private ArgumentCaptor<ErrandEntity> entityCaptor;

	@InjectMocks
	private ErrandService service;

	@Test
	void createErrand() {
		final var errand = Errand.create().withTitle("title");
		when(errandRepositoryMock.save(any(ErrandEntity.class)))
			.thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));
		when(processServiceMock.startProcess(any(), any(), any(), any())).thenReturn(Optional.empty());

		final var result = service.createErrand(MUNICIPALITY_ID, NAMESPACE, errand);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(errandRepositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getTitle()).isEqualTo("title");
		assertThat(entityCaptor.getValue().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(entityCaptor.getValue().getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(entityCaptor.getValue().getContactReason()).isNull();
		verify(processServiceMock).startProcess(MUNICIPALITY_ID, null, ERRAND_ID, null);
		verifyNoMoreInteractions(lookupRepositoryMock, processServiceMock);
	}

	@Test
	void createErrand_withProcessDefinitionName_startsProcessAndStoresInstanceId() {
		final var errand = Errand.create().withTitle("title").withProcessDefinitionName("Handläggning");
		when(errandRepositoryMock.save(any(ErrandEntity.class)))
			.thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));
		when(processServiceMock.startProcess(eq(MUNICIPALITY_ID), eq("Handläggning"), eq(ERRAND_ID), any()))
			.thenReturn(Optional.of("pi-1"));

		final var result = service.createErrand(MUNICIPALITY_ID, NAMESPACE, errand);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(errandRepositoryMock, org.mockito.Mockito.times(2)).save(entityCaptor.capture());
		final var allValues = entityCaptor.getAllValues();
		assertThat(allValues).hasSize(2);
		assertThat(allValues.getLast().getProcessInstanceId()).isEqualTo("pi-1");
	}

	@Test
	void createErrand_processServiceRejectsUnknownName_propagatesProblem() {
		final var errand = Errand.create().withTitle("title").withProcessDefinitionName("Unknown");
		when(errandRepositoryMock.save(any(ErrandEntity.class)))
			.thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));
		when(processServiceMock.startProcess(eq(MUNICIPALITY_ID), eq("Unknown"), eq(ERRAND_ID), any()))
			.thenThrow(se.sundsvall.dept44.problem.Problem.valueOf(BAD_REQUEST, "No Operaton process definition found with name 'Unknown'"));

		assertThatThrownBy(() -> service.createErrand(MUNICIPALITY_ID, NAMESPACE, errand))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void createErrand_withContactReason_resolved() {
		final var errand = Errand.create().withTitle("title").withContactReason("PHONE");
		final var lookup = LookupEntity.create().withKind(CONTACT_REASON).withName("PHONE");
		when(lookupRepositoryMock.findByKindAndNamespaceAndMunicipalityIdAndName(CONTACT_REASON, NAMESPACE, MUNICIPALITY_ID, "PHONE"))
			.thenReturn(Optional.of(lookup));
		when(errandRepositoryMock.save(any(ErrandEntity.class)))
			.thenAnswer(inv -> ((ErrandEntity) inv.getArgument(0)).withId(ERRAND_ID));

		final var result = service.createErrand(MUNICIPALITY_ID, NAMESPACE, errand);

		assertThat(result).isEqualTo(ERRAND_ID);
		verify(errandRepositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getContactReason()).isSameAs(lookup);
	}

	@Test
	void createErrand_contactReasonNotFound_badRequest() {
		final var errand = Errand.create().withTitle("title").withContactReason("UNKNOWN");
		when(lookupRepositoryMock.findByKindAndNamespaceAndMunicipalityIdAndName(CONTACT_REASON, NAMESPACE, MUNICIPALITY_ID, "UNKNOWN"))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.createErrand(MUNICIPALITY_ID, NAMESPACE, errand))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(errandRepositoryMock, never()).save(any());
	}

	@Test
	void readErrand() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withTitle("title")
			.withNamespace(NAMESPACE).withMunicipalityId(MUNICIPALITY_ID);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));

		final var result = service.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(ERRAND_ID);
		assertThat(result.getTitle()).isEqualTo("title");
	}

	@Test
	void readErrand_notFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void findErrands_noFilter() {
		final var page = new PageImpl<>(List.of(ErrandEntity.create().withId("a")));
		when(errandRepositoryMock.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

		final var result = service.findErrands(MUNICIPALITY_ID, NAMESPACE, null, PageRequest.of(0, 20));

		assertThat(result).isNotNull();
		assertThat(result.getErrands()).hasSize(1);
		assertThat(result.getMetaData()).isNotNull();
	}

	@Test
	void findErrands_withFilter() {
		final var page = new PageImpl<ErrandEntity>(List.of());
		final Specification<ErrandEntity> filter = (root, q, cb) -> cb.conjunction();
		when(errandRepositoryMock.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

		final var result = service.findErrands(MUNICIPALITY_ID, NAMESPACE, filter, PageRequest.of(0, 20));

		assertThat(result).isNotNull();
		assertThat(result.getErrands()).isEmpty();
	}

	@Test
	void updateErrand() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withTitle("old");
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));

		service.updateErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PatchErrand.create().withTitle("new"));

		verify(errandRepositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getTitle()).isEqualTo("new");
		verifyNoMoreInteractions(lookupRepositoryMock);
	}

	@Test
	void updateErrand_withContactReason_resolved() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID);
		final var lookup = LookupEntity.create().withKind(CONTACT_REASON).withName("PHONE");
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));
		when(lookupRepositoryMock.findByKindAndNamespaceAndMunicipalityIdAndName(CONTACT_REASON, NAMESPACE, MUNICIPALITY_ID, "PHONE"))
			.thenReturn(Optional.of(lookup));

		service.updateErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PatchErrand.create().withContactReason("PHONE"));

		verify(errandRepositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getContactReason()).isSameAs(lookup);
	}

	@Test
	void updateErrand_notFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PatchErrand.create()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(errandRepositoryMock, never()).save(any());
	}

	@Test
	void deleteErrand() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID);
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));

		service.deleteErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verify(errandRepositoryMock).delete(entity);
	}

	@Test
	void deleteErrand_notFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(errandRepositoryMock, never()).delete(any(ErrandEntity.class));
	}
}
