package se.sundsvall.caremanagement.cocaseworkers.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import se.sundsvall.caremanagement.cocaseworkers.api.model.AddCoCaseworker;
import se.sundsvall.caremanagement.cocaseworkers.integration.db.CoCaseworkerRepository;
import se.sundsvall.caremanagement.cocaseworkers.integration.db.model.CoCaseworkerEntity;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class CoCaseworkerServiceTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String USER_ID = "jane01doe";
	private static final String CO_CASEWORKER_ID = "c1";

	@Mock
	private ErrandAccessGuard errandGuardMock;

	@Mock
	private CoCaseworkerRepository repositoryMock;

	@InjectMocks
	private CoCaseworkerService service;

	@Test
	void addSavesEntityAndReturnsId() {
		when(repositoryMock.existsByNamespaceAndMunicipalityIdAndErrandIdAndUserId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, USER_ID)).thenReturn(false);
		when(repositoryMock.save(any(CoCaseworkerEntity.class))).thenReturn(CoCaseworkerEntity.create().withId(CO_CASEWORKER_ID));

		final var id = service.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new AddCoCaseworker(USER_ID));

		assertThat(id).isEqualTo(CO_CASEWORKER_ID);
		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		final ArgumentCaptor<CoCaseworkerEntity> captor = ArgumentCaptor.forClass(CoCaseworkerEntity.class);
		verify(repositoryMock).save(captor.capture());
		assertThat(captor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(captor.getValue().getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(captor.getValue().getNamespace()).isEqualTo(NAMESPACE);
		assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
		assertThat(captor.getValue().getCreated()).isNotNull();
	}

	@Test
	void addUnknownErrandNotFound() {
		doThrow(Problem.valueOf(NOT_FOUND, "No errand"))
			.when(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThatThrownBy(() -> service.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new AddCoCaseworker(USER_ID)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void addAlreadyExistingConflicts() {
		when(repositoryMock.existsByNamespaceAndMunicipalityIdAndErrandIdAndUserId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, USER_ID)).thenReturn(true);

		assertThatThrownBy(() -> service.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new AddCoCaseworker(USER_ID)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessage("Conflict: User 'jane01doe' is already a co-caseworker on errand 'errand-1'");

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void listForErrandReturnsMappedCoCaseworkers() {
		when(repositoryMock.findAllByNamespaceAndMunicipalityIdAndErrandId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, Sort.unsorted())).thenReturn(java.util.List.of(
			CoCaseworkerEntity.create().withId(CO_CASEWORKER_ID).withErrandId(ERRAND_ID).withUserId(USER_ID).withCreated(FIXED_TIMESTAMP)));

		final var result = service.listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Sort.unsorted());

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo(CO_CASEWORKER_ID);
		assertThat(result.getFirst().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(result.getFirst().getUserId()).isEqualTo(USER_ID);
		assertThat(result.getFirst().getCreated()).isEqualTo(FIXED_TIMESTAMP);
		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void removeDeletesExistingCoCaseworker() {
		final var existing = CoCaseworkerEntity.create().withId(CO_CASEWORKER_ID).withErrandId(ERRAND_ID).withUserId(USER_ID);
		when(repositoryMock.findByNamespaceAndMunicipalityIdAndErrandIdAndUserId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, USER_ID)).thenReturn(Optional.of(existing));

		service.remove(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, USER_ID);

		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(repositoryMock).delete(existing);
	}

	@Test
	void removeNotFound() {
		when(repositoryMock.findByNamespaceAndMunicipalityIdAndErrandIdAndUserId(NAMESPACE, MUNICIPALITY_ID, ERRAND_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.remove(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, USER_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No co-caseworker 'jane01doe' found on errand 'errand-1' in namespace 'my-namespace' for municipality id '2281'");

		verify(repositoryMock, never()).delete(any());
	}
}
