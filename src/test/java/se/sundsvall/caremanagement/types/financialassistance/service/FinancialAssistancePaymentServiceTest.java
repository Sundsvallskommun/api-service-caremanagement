package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.LifecarePayment;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatus;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatusService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class FinancialAssistancePaymentServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "a3c1f4de-2b6a-4c1e-9d3f-7e8a9b0c1d2e";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

	@Mock
	private PaymentStatusService paymentStatusServiceMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FinancialAssistanceRepository financialAssistanceRepositoryMock;

	@Mock
	private DecisionService decisionServiceMock;

	@Mock
	private LifecareServiceIdService lifecareServiceIdServiceMock;

	// "Today" is Wednesday 2026-09-23, Swedish time.
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneId.of("Europe/Stockholm"));

	private FinancialAssistancePaymentService service;

	@BeforeEach
	void setUp() {
		service = new FinancialAssistancePaymentService(paymentStatusServiceMock, errandServiceMock,
			financialAssistanceRepositoryMock, decisionServiceMock, lifecareServiceIdServiceMock, CLOCK);
	}

	private static PaymentStatusRequest errandRequest() {
		return PaymentStatusRequest.create().withErrandId(ERRAND_ID).withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");
	}

	/** The errand is linked to these Lifecare payments and was decided on the given day. */
	private void linkedErrand(final LocalDate decisionDate, final String... lifecarePaymentIds) {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withLifecarePaymentIds(List.of(lifecarePaymentIds))));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Decision.create().withDecisionType("RECOMMENDATION").withDecisionDate(LocalDate.of(2026, 9, 1)),
			Decision.create().withDecisionType("PAYMENT").withValue("BIFALL").withDecisionDate(decisionDate)));
	}

	@Test
	void linkedPaymentsAreEffectuatedWhenLifecareReportsEveryOnePaid() {
		linkedErrand(LocalDate.of(2026, 9, 21), "101", "102");
		// The window runs from the month before the application month to a month past today, for a payment dated ahead.
		when(paymentStatusServiceMock.paidPaymentDates(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 10, 23)))
			.thenReturn(Map.of("101", "2026-09-22", "102", "2026-09-23", "999", "2026-09-24"));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-09-23");
		assertThat(response.getDetail()).isNull();
		assertThat(response.getDeadline()).isEqualTo("2026-09-24"); // Monday + 3 working days
		assertThat(response.getOverdue()).isFalse();
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void linkedPaymentsAreNotEffectuatedByAnotherPaymentLifecareHasPaid() {
		linkedErrand(LocalDate.of(2026, 9, 21), "101", "102");
		when(paymentStatusServiceMock.paidPaymentDates(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(Map.of("101", "2026-09-22", "555", "2026-09-22"));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getPaymentDate()).isNull();
		assertThat(response.getDetail()).isEqualTo("1 av 2 kopplade utbetalningar är inte utbetalda i Lifecare ännu");
		assertThat(response.getDeadline()).isEqualTo("2026-09-24");
		assertThat(response.getOverdue()).isFalse();
	}

	@Test
	void linkedPaymentsAreOverdueTheDayAfterTheThirdWorkingDayAfterTheDecision() {
		// Decided Thursday 2026-09-17: Friday, Monday, Tuesday - the deadline is Tuesday 2026-09-22, overdue on the 23rd.
		linkedErrand(LocalDate.of(2026, 9, 17), "101");
		when(paymentStatusServiceMock.paidPaymentDates(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(Map.of());

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getDeadline()).isEqualTo("2026-09-22");
		assertThat(response.getOverdue()).isTrue();
	}

	@Test
	void linkedPaymentsAreNotOverdueOnTheDeadlineItself() {
		// Decided Friday 2026-09-18: Monday, Tuesday, Wednesday - the deadline is today, the 23rd, so not overdue yet.
		linkedErrand(LocalDate.of(2026, 9, 18), "101");
		when(paymentStatusServiceMock.paidPaymentDates(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(Map.of());

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getDeadline()).isEqualTo("2026-09-23");
		assertThat(response.getOverdue()).isFalse();
	}

	@Test
	void linkedPaymentsCountTheDeadlineFromTheDayTheDecisionWasRecordedWhenItHasNoDate() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withLifecarePaymentIds(List.of("101"))));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Decision.create().withDecisionType("PAYMENT").withCreated(OffsetDateTime.parse("2026-09-16T23:30:00Z")))); // the 17th in Sweden
		when(paymentStatusServiceMock.paidPaymentDates(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(Map.of());

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getDeadline()).isEqualTo("2026-09-22");
		assertThat(response.getOverdue()).isTrue();
	}

	@Test
	void linkedPaymentsWithoutADecisionCountFromTodaySoTheyCannotEscalate() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withLifecarePaymentIds(List.of("101"))));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Decision.create().withDecisionType("PAYMENT")));
		when(paymentStatusServiceMock.paidPaymentDates(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(Map.of());

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getDeadline()).isEqualTo("2026-09-28"); // Wednesday + 3 working days
		assertThat(response.getOverdue()).isFalse();
	}

	@Test
	void linkedPaymentsArePaidOnlyWhenEveryLinkedIdIsReportedPaid() {
		linkedErrand(LocalDate.of(2026, 9, 21), "101", "102", "103");
		when(paymentStatusServiceMock.paidPaymentDates(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(Map.of("102", "2026-09-22"));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getDetail()).isEqualTo("2 av 3 kopplade utbetalningar är inte utbetalda i Lifecare ännu");
		// The linked ids are the whole truth: no insats lookup, no search for other payments, nothing re-linked.
		verify(paymentStatusServiceMock, never()).registeredPayments(any(), any(), any(), any());
		verifyNoInteractions(lifecareServiceIdServiceMock);
		verify(financialAssistanceRepositoryMock, never()).save(any());
	}

	@Test
	void linkedPaymentsPropagateALifecareIntegratorFailureInsteadOfAnswering() {
		linkedErrand(LocalDate.of(2026, 9, 17), "101");
		when(paymentStatusServiceMock.paidPaymentDates(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "Error fetching payments in Lifecare FamilyCare"));
		final var request = errandRequest();

		// A failed read is not "not paid yet": the call fails, so the process retries instead of counting towards overdue.
		assertThatThrownBy(() -> service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
		verify(financialAssistanceRepositoryMock, never()).save(any());
	}

	/**
	 * An errand Draken did not link payments to, decided on the given day, whose insats in Lifecare is 7700 — the ids
	 * the Lifecare payments on it carry are not referenced by another errand unless a test says so.
	 */
	private FinancialAssistanceEntity unlinkedErrand(final LocalDate decisionDate) {
		final var entity = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID);
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Decision.create().withDecisionType("PAYMENT").withValue("BIFALL").withDecisionDate(decisionDate)));
		when(lifecareServiceIdServiceMock.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(7700);
		return entity;
	}

	@Test
	void paymentsOnTheErrandsInsatsAreFoundInLifecareAndLinkedOncePaid() {
		final var entity = unlinkedErrand(LocalDate.of(2026, 9, 21));
		when(paymentStatusServiceMock.registeredPayments(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 10, 23))).thenReturn(List.of(
			new LifecarePayment("101", 7700, "2026-06", "2026-06-25"),
			new LifecarePayment("102", 7700, "2026-06, 2026-07", "2026-06-26"),
			new LifecarePayment("200", 9999, "2026-06", "2026-06-25"), // another insats
			new LifecarePayment("300", 7700, "2026-05", "2026-05-25"), // another month
			new LifecarePayment("400", 7700, "2026-06", "2026-06-25"))); // another errand's linked payment
		when(financialAssistanceRepositoryMock.findLifecarePaymentIdsLinkedElsewhere(Set.of("101", "102", "400"), ERRAND_ID)).thenReturn(List.of("400"));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-06-26");
		assertThat(response.getDeadline()).isEqualTo("2026-09-24");
		assertThat(response.getOverdue()).isFalse();
		// From now on they are this decision's: linked, so the next read verifies exactly them.
		verify(financialAssistanceRepositoryMock).save(entity);
		assertThat(entity.getLifecarePaymentIds()).containsExactly("101", "102");
		assertThat(entity.getLifecareServiceId()).isEqualTo(7700);
	}

	@Test
	void paymentsOnTheInsatsWithoutAPayDateAreNotEffectuatedAndNotLinked() {
		unlinkedErrand(LocalDate.of(2026, 9, 21));
		when(paymentStatusServiceMock.registeredPayments(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(List.of(
			new LifecarePayment("101", 7700, "2026-06", "2026-06-25"),
			new LifecarePayment("102", 7700, "2026-06", null)));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getDetail()).isEqualTo("1 av 2 utbetalningar på ärendets insats är inte utbetalda i Lifecare ännu");
		assertThat(response.getOverdue()).isFalse();
		verify(financialAssistanceRepositoryMock, never()).save(any());
	}

	@Test
	void aBifallWhosePaymentCareMHasNotSeenYetWaitsForTheDeadlineInsteadOfEscalatingAtOnce() {
		unlinkedErrand(LocalDate.of(2026, 9, 21));
		when(paymentStatusServiceMock.registeredPayments(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(List.of());

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getDetail()).isEqualTo("Ingen utbetalning för 2026-06 hittas på ärendets insats i Lifecare ännu");
		assertThat(response.getDeadline()).isEqualTo("2026-09-24");
		assertThat(response.getOverdue()).isFalse();
		verify(financialAssistanceRepositoryMock, never()).findLifecarePaymentIdsLinkedElsewhere(any(), any());
	}

	@Test
	void onlyAnotherErrandsPaymentOnTheInsatsIsNeverTakenAndIsOverdueAfterTheDeadline() {
		unlinkedErrand(LocalDate.of(2026, 9, 17));
		when(paymentStatusServiceMock.registeredPayments(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(List.of(new LifecarePayment("400", 7700, "2026-06", "2026-06-25")));
		when(financialAssistanceRepositoryMock.findLifecarePaymentIdsLinkedElsewhere(Set.of("400"), ERRAND_ID)).thenReturn(List.of("400"));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getDetail()).isEqualTo("Ingen utbetalning för 2026-06 hittas på ärendets insats i Lifecare ännu");
		assertThat(response.getDeadline()).isEqualTo("2026-09-22");
		assertThat(response.getOverdue()).isTrue();
		verify(financialAssistanceRepositoryMock, never()).save(any());
	}

	@Test
	void paymentsOnTheInsatsWithoutAConcernedMonthAreNotTheErrands() {
		unlinkedErrand(LocalDate.of(2026, 9, 21));
		when(paymentStatusServiceMock.registeredPayments(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(List.of(
			new LifecarePayment("101", 7700, null, "2026-06-25"),
			new LifecarePayment("102", 7700, " ", "2026-06-25"),
			new LifecarePayment("103", null, "2026-06", "2026-06-25")));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getDetail()).isEqualTo("Ingen utbetalning för 2026-06 hittas på ärendets insats i Lifecare ännu");
		verify(financialAssistanceRepositoryMock, never()).save(any());
	}

	@Test
	void paymentsOnTheInsatsAreReadUpToTheEndOfAnApplicationMonthLaterThanTheWindow() {
		final var entity = unlinkedErrand(LocalDate.of(2026, 9, 21));
		final var request = errandRequest().withApplicationMonth("2026-12");
		when(paymentStatusServiceMock.registeredPayments(MUNICIPALITY_ID, APPLICANT_PARTY_ID, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31)))
			.thenReturn(List.of(new LifecarePayment("101", 7700, "2026-12", "2026-11-25")));
		when(financialAssistanceRepositoryMock.findLifecarePaymentIdsLinkedElsewhere(Set.of("101"), ERRAND_ID)).thenReturn(List.of());

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getEffectuated()).isTrue();
		assertThat(entity.getLifecarePaymentIds()).containsExactly("101");
	}

	@Test
	void paymentsOnTheInsatsPropagateALifecareIntegratorFailureAndLinkNothing() {
		final var entity = unlinkedErrand(LocalDate.of(2026, 9, 17));
		when(paymentStatusServiceMock.registeredPayments(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "Error fetching payments in Lifecare FamilyCare"));
		final var request = errandRequest();

		assertThatThrownBy(() -> service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
		verify(financialAssistanceRepositoryMock, never()).save(any());
		assertThat(entity.getLifecarePaymentIds()).isNullOrEmpty();
	}

	@Test
	void anUnknownInsatsCannotBeCheckedAndSaysSo() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Decision.create().withDecisionType("PAYMENT").withDecisionDate(LocalDate.of(2026, 9, 17))));
		when(lifecareServiceIdServiceMock.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(null);

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getDetail()).isEqualTo("Ärendets insats i Lifecare är okänd – utbetalningen kan inte kontrolleras");
		assertThat(response.getOverdue()).isTrue();
		verifyNoInteractions(paymentStatusServiceMock);
	}

	@Test
	void anErrandWithoutTypedDataCannotBeCheckedAndSaysSo() {
		when(lifecareServiceIdServiceMock.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(7700);

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, errandRequest());

		assertThat(response.getDetail()).isEqualTo("Ärendets insats i Lifecare är okänd – utbetalningen kan inte kontrolleras");
		assertThat(response.getOverdue()).isFalse(); // no decision: counted from today
	}

	@Test
	void anErrandMissingInTheNamespaceYields404BeforeLifecareIsRead() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "No errand"));
		final var request = errandRequest();

		assertThatThrownBy(() -> service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(financialAssistanceRepositoryMock, paymentStatusServiceMock);
	}

	@Test
	void checkPaymentStatusWithoutErrandReadsPersonAndMonth() {
		when(paymentStatusServiceMock.read(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE))).thenReturn(new PaymentStatus(true, "2026-05-27"));

		final var request = PaymentStatusRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getEffectuated()).isTrue();
		assertThat(response.getPaymentDate()).isEqualTo("2026-05-27");
		assertThat(response.getDetail()).isNull();
	}

	@Test
	void checkPaymentStatusWithoutErrandNotEffectuated() {
		when(paymentStatusServiceMock.read(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE))).thenReturn(new PaymentStatus(false, null));

		final var request = PaymentStatusRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06");

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getEffectuated()).isFalse();
		assertThat(response.getPaymentDate()).isNull();
	}

	@Test
	void checkPaymentStatusWithoutErrandHasNoDeadline() {
		when(paymentStatusServiceMock.read(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE))).thenReturn(new PaymentStatus(false, null));

		final var response = service.checkPaymentStatus(MUNICIPALITY_ID, NAMESPACE,
			PaymentStatusRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06"));

		assertThat(response.getDeadline()).isNull();
		assertThat(response.getOverdue()).isNull();
	}
}
