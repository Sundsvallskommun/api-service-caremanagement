package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaPaymentRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPaymentEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FaPaymentRepository repositoryMock;

	@InjectMocks
	private PaymentService service;

	private static FaPaymentEntity entity(final String id, final OffsetDateTime created) {
		return FaPaymentEntity.create().withId(id).withErrandId(ERRAND_ID).withApplicationMonth("2026-08").withCreated(created);
	}

	private static PaymentRequest request() {
		return PaymentRequest.create().withMoneyType("FORSORJNINGSSTOD").withAmount(new BigDecimal("4500.00")).withApplicationMonth("2026-08")
			.withPayeeName("Anna Andersson");
	}

	@Test
	void listReturnsMappedSortedByCreated() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			entity("b2", OffsetDateTime.parse("2026-06-02T00:00:00Z")),
			entity("b1", OffsetDateTime.parse("2026-06-01T00:00:00Z"))));

		final var result = service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).extracting("id").containsExactly("b1", "b2"); // created asc
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void countDelegatesAfterScopeCheck() {
		when(repositoryMock.countByErrandId(ERRAND_ID)).thenReturn(2L);

		assertThat(service.count(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(2L);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(repositoryMock).countByErrandId(ERRAND_ID);
	}

	@Test
	void getReturnsPayment() {
		when(repositoryMock.findByIdAndErrandId("b1", ERRAND_ID)).thenReturn(Optional.of(entity("b1", null)));

		final var result = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "b1");

		assertThat(result.getId()).isEqualTo("b1");
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void getMissingYields404() {
		when(repositoryMock.findByIdAndErrandId("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: Payment not found on errand");
	}

	@Test
	void createPersistsAsDraftAndReturns() {
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request());

		final var captor = ArgumentCaptor.forClass(FaPaymentEntity.class);
		verify(repositoryMock).save(captor.capture());
		final var saved = captor.getValue();
		assertThat(saved.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(saved.getMoneyType()).isEqualTo("FORSORJNINGSSTOD");
		assertThat(saved.getAmount()).isEqualTo(new BigDecimal("4500.00"));
		assertThat(saved.getApplicationMonth()).isEqualTo("2026-08");
		assertThat(saved.getPayeeName()).isEqualTo("Anna Andersson");
		assertThat(saved.getStatus()).isEqualTo("DRAFT");
		assertThat(result.getStatus()).isEqualTo("DRAFT");
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void createDefaultsSourceToCaseworker() {
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request());

		final var captor = ArgumentCaptor.forClass(FaPaymentEntity.class);
		verify(repositoryMock).save(captor.capture());
		assertThat(captor.getValue().getSource()).isEqualTo("CASEWORKER");
		assertThat(captor.getValue().getLifecareId()).isNull();
		assertThat(result.getSource()).isEqualTo("CASEWORKER");
		verify(repositoryMock, never()).findByErrandIdAndLifecareId(any(), any());
	}

	@Test
	void createDoesNotEnqueueAnyRpaTask() {
		// PaymentService owns no RPA client/queue dependency at all — queuing REGISTER_PAYMENT is a separate,
		// explicit POST .../rpa-tasks call. Absence of interaction is the assertion here.
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request());

		verify(repositoryMock).save(any());
	}

	@Test
	void createLifecareSourcedInsertsWhenNoneExists() {
		when(repositoryMock.findByErrandIdAndLifecareId(ERRAND_ID, "987654")).thenReturn(Optional.empty());
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			request().withSource("LIFECARE").withLifecareId("987654"));

		final var captor = ArgumentCaptor.forClass(FaPaymentEntity.class);
		verify(repositoryMock).save(captor.capture());
		assertThat(captor.getValue().getId()).isNull(); // a fresh entity, id assigned on persist
		assertThat(captor.getValue().getSource()).isEqualTo("LIFECARE");
		assertThat(captor.getValue().getLifecareId()).isEqualTo("987654");
		assertThat(result.getLifecareId()).isEqualTo("987654");
	}

	@Test
	void createLifecareSourcedUpsertsOntoExisting() {
		final var existing = entity("b1", OffsetDateTime.parse("2026-06-01T00:00:00Z")).withSource("LIFECARE").withLifecareId("987654")
			.withMoneyType("old");
		when(repositoryMock.findByErrandIdAndLifecareId(ERRAND_ID, "987654")).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			request().withSource("LIFECARE").withLifecareId("987654"));

		final var captor = ArgumentCaptor.forClass(FaPaymentEntity.class);
		verify(repositoryMock).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo("b1"); // re-used the existing row, no duplicate
		assertThat(captor.getValue().getMoneyType()).isEqualTo("FORSORJNINGSSTOD");
		assertThat(result.getId()).isEqualTo("b1");
	}

	@Test
	void updatePreservesProvenanceWhenNotSupplied() {
		final var existing = entity("b1", OffsetDateTime.parse("2026-06-01T00:00:00Z")).withSource("LIFECARE").withLifecareId("987654");
		when(repositoryMock.findByIdAndErrandId("b1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "b1", request());

		assertThat(result.getSource()).isEqualTo("LIFECARE");
		assertThat(result.getLifecareId()).isEqualTo("987654");
	}

	@Test
	void updateStampsProvenanceWhenSupplied() {
		final var existing = entity("b1", OffsetDateTime.parse("2026-06-01T00:00:00Z")); // source/lifecareId unset
		when(repositoryMock.findByIdAndErrandId("b1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "b1",
			request().withSource("CASEWORKER").withLifecareId("987654"));

		assertThat(result.getSource()).isEqualTo("CASEWORKER");
		assertThat(result.getLifecareId()).isEqualTo("987654");
	}

	@Test
	void updateNeverChangesStatus() {
		final var existing = entity("b1", OffsetDateTime.parse("2026-06-01T00:00:00Z")).withStatus("QUEUED");
		when(repositoryMock.findByIdAndErrandId("b1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "b1", request());

		assertThat(result.getStatus()).isEqualTo("QUEUED");
	}

	@Test
	void updateReplacesFields() {
		final var existing = entity("b1", OffsetDateTime.parse("2026-06-01T00:00:00Z")).withMoneyType("old");
		when(repositoryMock.findByIdAndErrandId("b1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "b1", request());

		assertThat(result.getMoneyType()).isEqualTo("FORSORJNINGSSTOD");
		assertThat(result.getPayeeName()).isEqualTo("Anna Andersson");
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void updateMissingYields404() {
		when(repositoryMock.findByIdAndErrandId("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing", request()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: Payment not found on errand");
	}

	@Test
	void deleteRemoves() {
		final var existing = entity("b1", null);
		when(repositoryMock.findByIdAndErrandId("b1", ERRAND_ID)).thenReturn(Optional.of(existing));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "b1");

		verify(repositoryMock).delete(existing);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void deleteMissingYields404() {
		when(repositoryMock.findByIdAndErrandId("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: Payment not found on errand");

		verify(repositoryMock, never()).delete(any());
	}

	@Test
	void scopeCheckPropagatesWhenErrandMissing() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenThrow(Problem.valueOf(NOT_FOUND, "Errand not found"));

		assertThatThrownBy(() -> service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: Errand not found");

		verify(repositoryMock, never()).findByErrandId(any());
	}

	@Test
	void createForDecisionStampsPendingRegistrationAndReturnsTheId() {
		// A row the caseworker has decided on is not a draft — the status has to say what is actually waiting for the
		// robot. The errand is not scope-checked here: finalize has already read it, inside the same transaction.
		when(repositoryMock.save(any(FaPaymentEntity.class)))
			.thenAnswer(invocation -> invocation.<FaPaymentEntity>getArgument(0).withId("pay-1"));

		final var id = service.createForDecision(ERRAND_ID, PaymentRequest.create()
			.withAmount(new BigDecimal("6000.00")).withApplicationMonth("2026-06").withAccountingCode("5011").withPayeeName("Hyresvärden AB"));

		assertThat(id).isEqualTo("pay-1");
		final var captor = ArgumentCaptor.forClass(FaPaymentEntity.class);
		verify(repositoryMock).save(captor.capture());
		assertThat(captor.getValue())
			.returns("PENDING_REGISTRATION", FaPaymentEntity::getStatus)
			.returns("CASEWORKER", FaPaymentEntity::getSource)
			.returns(ERRAND_ID, FaPaymentEntity::getErrandId)
			.returns("2026-06", FaPaymentEntity::getApplicationMonth)
			.returns("5011", FaPaymentEntity::getAccountingCode);
		verifyNoInteractions(errandServiceMock);
	}

	@Test
	void createStillYieldsADraft() {
		// The caseworker's own save must keep working exactly as before — saving a draft never sets the robot off.
		when(repositoryMock.save(any(FaPaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var payment = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PaymentRequest.create().withAmount(new BigDecimal("500.00")));

		assertThat(payment.getStatus()).isEqualTo("DRAFT");
	}
}
