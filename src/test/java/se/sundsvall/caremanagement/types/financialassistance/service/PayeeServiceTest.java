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
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.PaymentView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeLifecareResult;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaPayeeRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPayeeEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.PayeeService.LIFECARE_STATUS_FAILED;
import static se.sundsvall.caremanagement.types.financialassistance.service.PayeeService.LIFECARE_STATUS_PENDING;
import static se.sundsvall.caremanagement.types.financialassistance.service.PayeeService.LIFECARE_STATUS_SYNCED;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.PayeeMapper.SOURCE_LIFECARE;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.PayeeMapper.SOURCE_MANUAL;

@ExtendWith(MockitoExtension.class)
class PayeeServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PAYEE_ID = randomUUID().toString();
	private static final String PERSONAL_NUMBER = "19800101T001";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private HouseholdPartyService householdPartyServiceMock;

	@Mock
	private LifecareCaseHistoryService lifecareCaseHistoryServiceMock;

	@Mock
	private FaPayeeRepository payeeRepositoryMock;

	@InjectMocks
	private PayeeService service;

	private static PaymentView payment(final String payDate, final String name, final String account) {
		return new PaymentView(1, new BigDecimal("7900.00"), "Personkonto", payDate, "6000", account,
			name, null, null, null, null, null, "2026-08");
	}

	private static FaPayeeEntity manual(final String id, final String name, final String account, final String status) {
		return FaPayeeEntity.create()
			.withId(id)
			.withErrandId(ERRAND_ID)
			.withName(name)
			.withPaymentMethod("Personkonto")
			.withClearing("6000")
			.withAccountNumber(account)
			.withLifecareStatus(status)
			.withCreated(OffsetDateTime.parse("2026-09-21T12:00:00Z"));
	}

	private void householdWithApplicant() {
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new HouseholdPartyService.Household(Optional.of(PERSONAL_NUMBER), false, Optional.empty(), Optional.empty()));
	}

	@Test
	void listPutsLifecarePayeesFirstMostRecentlyPaidAndAppendsManualOnes() {
		householdWithApplicant();
		when(lifecareCaseHistoryServiceMock.listPayments(eq(MUNICIPALITY_ID), eq(PERSONAL_NUMBER), any(), any())).thenReturn(List.of(
			payment("2026-06-27", "Anna Andersson", "111"),
			payment("2026-08-27", "Hyresvärden AB", "222")));
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(manual(PAYEE_ID, "Ny Mottagare", "333", LIFECARE_STATUS_PENDING)));

		final var result = service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).extracting("name").containsExactly("Hyresvärden AB", "Anna Andersson", "Ny Mottagare");
		assertThat(result).extracting("source").containsExactly(SOURCE_LIFECARE, SOURCE_LIFECARE, SOURCE_MANUAL);
		assertThat(result.getFirst().getLastPaidOn()).isEqualTo("2026-08-27");
		assertThat(result.getFirst().getId()).isNull();
		assertThat(result.getLast().getId()).isEqualTo(PAYEE_ID);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void listCollapsesAManualPayeeIntoItsLifecareTwin() {
		householdWithApplicant();
		when(lifecareCaseHistoryServiceMock.listPayments(eq(MUNICIPALITY_ID), eq(PERSONAL_NUMBER), any(), any()))
			.thenReturn(List.of(payment("2026-08-27", "Hyresvärden AB", "222")));
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(List.of(manual(PAYEE_ID, " hyresvärden ab ", "222", LIFECARE_STATUS_SYNCED)));

		final var result = service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getSource()).isEqualTo(SOURCE_LIFECARE);
	}

	@Test
	void listDedupesRepeatedLifecarePayees() {
		householdWithApplicant();
		when(lifecareCaseHistoryServiceMock.listPayments(eq(MUNICIPALITY_ID), eq(PERSONAL_NUMBER), any(), any())).thenReturn(List.of(
			payment("2026-08-27", "Anna Andersson", "111"),
			payment("2026-07-27", "Anna Andersson", "111")));
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		final var result = service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getLastPaidOn()).isEqualTo("2026-08-27");
	}

	@Test
	void listSkipsLifecarePaymentsWithNeitherNameNorAccount() {
		householdWithApplicant();
		when(lifecareCaseHistoryServiceMock.listPayments(eq(MUNICIPALITY_ID), eq(PERSONAL_NUMBER), any(), any()))
			.thenReturn(List.of(payment("2026-08-27", null, null)));
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		assertThat(service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
	}

	@Test
	void listFallsBackToManualPayeesWhenLifecareIsDown() {
		householdWithApplicant();
		when(lifecareCaseHistoryServiceMock.listPayments(eq(MUNICIPALITY_ID), eq(PERSONAL_NUMBER), any(), any())).thenThrow(new IllegalStateException("Lifecare down"));
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(manual(PAYEE_ID, "Ny Mottagare", "333", LIFECARE_STATUS_PENDING)));

		final var result = service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getSource()).isEqualTo(SOURCE_MANUAL);
	}

	@Test
	void listSkipsLifecareEntirelyWhenTheApplicantCannotBeResolved() {
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new HouseholdPartyService.Household(Optional.empty(), false, Optional.empty(), Optional.empty()));
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		assertThat(service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
		verifyNoInteractions(lifecareCaseHistoryServiceMock);
	}

	@Test
	void getReturnsTheManualPayee() {
		when(payeeRepositoryMock.findByIdAndErrandId(PAYEE_ID, ERRAND_ID))
			.thenReturn(Optional.of(manual(PAYEE_ID, "Ny Mottagare", "333", LIFECARE_STATUS_PENDING)));

		final var result = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID);

		assertThat(result.getId()).isEqualTo(PAYEE_ID);
		assertThat(result.getSource()).isEqualTo(SOURCE_MANUAL);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void getThrowsNotFoundForAnUnknownPayee() {
		when(payeeRepositoryMock.findByIdAndErrandId(PAYEE_ID, ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Payee not found on errand");
	}

	@Test
	void createStoresThePayeePending() {
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(payeeRepositoryMock.save(any(FaPayeeEntity.class))).thenAnswer(invocation -> ((FaPayeeEntity) invocation.getArgument(0)).withId(PAYEE_ID));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PayeeRequest.create()
			.withName("Ny Mottagare")
			.withPaymentMethod("Plusgiro")
			.withAccountNumber("333"));

		assertThat(result.getId()).isEqualTo(PAYEE_ID);
		assertThat(result.getLifecareStatus()).isEqualTo(LIFECARE_STATUS_PENDING);
		assertThat(result.getSource()).isEqualTo(SOURCE_MANUAL);

		final var entity = ArgumentCaptor.forClass(FaPayeeEntity.class);
		verify(payeeRepositoryMock).save(entity.capture());
		assertThat(entity.getValue().getErrandId()).isEqualTo(ERRAND_ID);
	}

	@Test
	void createReusesAnIdenticalPayeeInsteadOfDuplicatingIt() {
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(List.of(manual(PAYEE_ID, "Ny Mottagare", "333", LIFECARE_STATUS_SYNCED)));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PayeeRequest.create()
			.withName("ny mottagare")
			.withPaymentMethod("Personkonto")
			.withClearing("6000")
			.withAccountNumber("333"));

		assertThat(result.getId()).isEqualTo(PAYEE_ID);
		assertThat(result.getLifecareStatus()).isEqualTo(LIFECARE_STATUS_SYNCED);
		verify(payeeRepositoryMock, never()).save(any());
	}

	@Test
	void addedMarksThePayeeSynced() {
		when(payeeRepositoryMock.findByIdAndErrandId(PAYEE_ID, ERRAND_ID))
			.thenReturn(Optional.of(manual(PAYEE_ID, "Ny Mottagare", "333", LIFECARE_STATUS_PENDING)));
		when(payeeRepositoryMock.save(any(FaPayeeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.recordLifecareResult(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID,
			PayeeLifecareResult.create().withOutcome("ADDED").withLifecarePayeeId("44213"));

		assertThat(result.getLifecareStatus()).isEqualTo(LIFECARE_STATUS_SYNCED);
		assertThat(result.getLifecarePayeeId()).isEqualTo("44213");
		assertThat(result.getLifecareDetail()).isNull();
	}

	@Test
	void alreadyExistsCountsAsSuccess() {
		when(payeeRepositoryMock.findByIdAndErrandId(PAYEE_ID, ERRAND_ID))
			.thenReturn(Optional.of(manual(PAYEE_ID, "Ny Mottagare", "333", LIFECARE_STATUS_PENDING)));
		when(payeeRepositoryMock.save(any(FaPayeeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.recordLifecareResult(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID,
			PayeeLifecareResult.create().withOutcome("ALREADY_EXISTS"));

		assertThat(result.getLifecareStatus()).isEqualTo(LIFECARE_STATUS_SYNCED);
	}

	@Test
	void failedStoresLifecaresOwnMessage() {
		when(payeeRepositoryMock.findByIdAndErrandId(PAYEE_ID, ERRAND_ID))
			.thenReturn(Optional.of(manual(PAYEE_ID, "Ny Mottagare", "333", LIFECARE_STATUS_PENDING)));
		when(payeeRepositoryMock.save(any(FaPayeeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.recordLifecareResult(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID,
			PayeeLifecareResult.create().withOutcome("FAILED").withDetail("Kontonummer har fel format"));

		assertThat(result.getLifecareStatus()).isEqualTo(LIFECARE_STATUS_FAILED);
		assertThat(result.getLifecareDetail()).isEqualTo("Kontonummer har fel format");
	}

	@Test
	void failedWithoutDetailIsRejected() {
		when(payeeRepositoryMock.findByIdAndErrandId(PAYEE_ID, ERRAND_ID))
			.thenReturn(Optional.of(manual(PAYEE_ID, "Ny Mottagare", "333", LIFECARE_STATUS_PENDING)));
		final var result = PayeeLifecareResult.create().withOutcome("FAILED");

		assertThatThrownBy(() -> service.recordLifecareResult(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID, result))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("detail is required");
		verify(payeeRepositoryMock, never()).save(any());
	}

	@Test
	void failedOnAnAlreadySyncedPayeeIsAConflict() {
		when(payeeRepositoryMock.findByIdAndErrandId(PAYEE_ID, ERRAND_ID))
			.thenReturn(Optional.of(manual(PAYEE_ID, "Ny Mottagare", "333", LIFECARE_STATUS_SYNCED)));
		final var result = PayeeLifecareResult.create().withOutcome("FAILED").withDetail("Kontonummer har fel format");

		assertThatThrownBy(() -> service.recordLifecareResult(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID, result))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("already SYNCED");
		verify(payeeRepositoryMock, never()).save(any());
	}

	@Test
	void deleteRemovesTheManualPayee() {
		final var entity = manual(PAYEE_ID, "Ny Mottagare", "333", LIFECARE_STATUS_PENDING);
		when(payeeRepositoryMock.findByIdAndErrandId(PAYEE_ID, ERRAND_ID)).thenReturn(Optional.of(entity));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID);

		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(payeeRepositoryMock).delete(entity);
	}

	private static Payee decided(final String name, final String account) {
		return Payee.create().withName(name).withPaymentMethod("Personkonto").withClearing("6000").withAccountNumber(account);
	}

	@Test
	void warnsForADecidedPayeeTheRobotHasNotReportedYet() {
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(List.of(manual(PAYEE_ID, "Väntar AB", "222", LIFECARE_STATUS_PENDING)));

		final var result = service.unsyncedPayeeWarnings(ERRAND_ID, List.of(decided("Väntar AB", "222")));

		assertThat(result).singleElement().asString()
			.contains("Väntar AB")
			.contains("inte upplagd i Lifecare ännu");
	}

	@Test
	void warnsWithLifecaresOwnMessageWhenTheRobotFailed() {
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(List.of(manual(PAYEE_ID, "Trasig AB", "222", LIFECARE_STATUS_FAILED)
				.withLifecareDetail("Kontonummer har fel format")));

		final var result = service.unsyncedPayeeWarnings(ERRAND_ID, List.of(decided("Trasig AB", "222")));

		assertThat(result).singleElement().asString()
			.contains("Trasig AB")
			.contains("Kontonummer har fel format");
	}

	@Test
	void doesNotWarnWhenTheDecidedPayeeIsInLifecare() {
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(List.of(manual(PAYEE_ID, "Synkad AB", "111", LIFECARE_STATUS_SYNCED)));

		assertThat(service.unsyncedPayeeWarnings(ERRAND_ID, List.of(decided("Synkad AB", "111")))).isEmpty();
	}

	@Test
	void doesNotWarnAboutAnUnsyncedPayeeTheDecisionDoesNotPayTo() {
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			manual(PAYEE_ID, "Oanvänd AB", "999", LIFECARE_STATUS_PENDING),
			manual(randomUUID().toString(), "Synkad AB", "111", LIFECARE_STATUS_SYNCED)));

		assertThat(service.unsyncedPayeeWarnings(ERRAND_ID, List.of(decided("Synkad AB", "111")))).isEmpty();
	}

	@Test
	void doesNotWarnForAPayeeThatCameFromTheLifecareHistory() {
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		assertThat(service.unsyncedPayeeWarnings(ERRAND_ID, List.of(decided("Ur historiken AB", "111")))).isEmpty();
	}

	@Test
	void warnsOncePerPayeeEvenWhenSeveralPaymentsGoToIt() {
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(List.of(manual(PAYEE_ID, "Väntar AB", "222", LIFECARE_STATUS_PENDING)));

		final var result = service.unsyncedPayeeWarnings(ERRAND_ID,
			List.of(decided("Väntar AB", "222"), decided("väntar ab ", "222")));

		assertThat(result).hasSize(1);
	}

	@Test
	void toleratesAPaymentWithoutAPayee() {
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(List.of(manual(PAYEE_ID, "Väntar AB", "222", LIFECARE_STATUS_PENDING)));

		final var withNull = new java.util.ArrayList<Payee>();
		withNull.add(null);

		assertThat(service.unsyncedPayeeWarnings(ERRAND_ID, withNull)).isEmpty();
		assertThat(service.unsyncedPayeeWarnings(ERRAND_ID, null)).isEmpty();
	}

	@Test
	void listIsScopedToTheErrand() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenThrow(new IllegalStateException("404"));

		assertThatThrownBy(() -> service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(payeeRepositoryMock, lifecareCaseHistoryServiceMock);
	}

	@Test
	void errandLookupIsUsedAsTheScopeCheck() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create());
		when(payeeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new HouseholdPartyService.Household(Optional.empty(), false, Optional.empty(), Optional.empty()));

		assertThat(service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
	}
}
