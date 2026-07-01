package se.sundsvall.caremanagement.permit.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.permit.api.model.Permit;
import se.sundsvall.caremanagement.permit.integration.db.PermitRepository;
import se.sundsvall.caremanagement.permit.integration.db.model.PermitEntity;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class PermitServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandAccessGuard errandGuardMock;

	@Mock
	private PermitRepository permitRepositoryMock;

	@InjectMocks
	private PermitService service;

	private void errandMissing() {
		doThrow(Problem.valueOf(NOT_FOUND, "No errand"))
			.when(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void issueDefaultsValidFromAndStatus() {
		when(permitRepositoryMock.save(any(PermitEntity.class))).thenReturn(PermitEntity.create().withId("permit-1"));

		final var id = service.issue(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Permit.create().withPermitType("PARKING_PERMIT"));

		assertThat(id).isEqualTo("permit-1");
		final ArgumentCaptor<PermitEntity> captor = ArgumentCaptor.forClass(PermitEntity.class);
		verify(permitRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(captor.getValue().getPermitType()).isEqualTo("PARKING_PERMIT");
		assertThat(captor.getValue().getValidFrom()).isNotNull();
		assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
	}

	@Test
	void issueErrandNotFound() {
		errandMissing();

		assertThatThrownBy(() -> service.issue(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Permit.create().withPermitType("X")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
		verify(permitRepositoryMock, never()).save(any());
	}

	@Test
	void readReturnsPermit() {
		when(permitRepositoryMock.findByErrandIdAndId(ERRAND_ID, "permit-1")).thenReturn(Optional.of(PermitEntity.create().withId("permit-1").withStatus("ACTIVE")));

		final var permit = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "permit-1");

		assertThat(permit.getId()).isEqualTo("permit-1");
		assertThat(permit.getStatus()).isEqualTo("ACTIVE");
	}

	@Test
	void readPermitNotFound() {
		when(permitRepositoryMock.findByErrandIdAndId(ERRAND_ID, "missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readAllReturnsMappedList() {
		when(permitRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(
			PermitEntity.create().withId("p1"), PermitEntity.create().withId("p2")));

		final var permits = service.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(permits).extracting(Permit::getId).containsExactly("p1", "p2");
	}

	@Test
	void revokeSetsStatusRevoked() {
		when(permitRepositoryMock.findByErrandIdAndId(ERRAND_ID, "permit-1")).thenReturn(Optional.of(PermitEntity.create().withId("permit-1").withStatus("ACTIVE")));

		service.revoke(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "permit-1");

		final ArgumentCaptor<PermitEntity> captor = ArgumentCaptor.forClass(PermitEntity.class);
		verify(permitRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo("REVOKED");
	}

	@Test
	void revokeAllForErrandRevokesOnlyActivePermits() {
		when(permitRepositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(
			PermitEntity.create().withId("p1").withStatus("ACTIVE"),
			PermitEntity.create().withId("p2").withStatus("REVOKED")));

		service.revokeAllForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verify(permitRepositoryMock, times(1)).save(any(PermitEntity.class));
	}

	@Test
	void deleteRemovesPermit() {
		final var entity = PermitEntity.create().withId("permit-1");
		when(permitRepositoryMock.findByErrandIdAndId(ERRAND_ID, "permit-1")).thenReturn(Optional.of(entity));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "permit-1");

		verify(permitRepositoryMock).delete(entity);
	}
}
