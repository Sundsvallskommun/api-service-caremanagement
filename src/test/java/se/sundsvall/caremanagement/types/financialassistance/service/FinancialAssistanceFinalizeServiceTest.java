package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CommunicationChannels;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizePayment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceFinalizeServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String DECIDED_BY = "jane02doe";
	private static final String DECISION_ID = "decision-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private SectionApprovalService sectionApprovalServiceMock;

	@Mock
	private DecisionService decisionServiceMock;

	@Mock
	private ProcessService processServiceMock;

	@Captor
	private ArgumentCaptor<Decision> decisionCaptor;

	@Captor
	private ArgumentCaptor<FinancialAssistanceEntity> entityCaptor;

	@Captor
	private ArgumentCaptor<Map<String, Object>> variablesCaptor;

	@Mock
	private PaymentService paymentServiceMock;

	@Mock
	private PayeeService payeeServiceMock;

	@InjectMocks
	private FinancialAssistanceFinalizeService service;

	private static FinalizeRequest grantingRequest() {
		return FinalizeRequest.create()
			.withDecision(FinalizeDecision.create()
				.withOutcome("BIFALL")
				.withReason("Inkomster enligt SSBTEK")
				.withPeriodFrom(LocalDate.of(2026, 6, 1))
				.withPeriodTo(LocalDate.of(2026, 6, 30))
				.withAmount(new BigDecimal("7900.00"))
				.withDecisionMessage("Du beviljas ekonomiskt bistånd"))
			.withCommunication(CommunicationChannels.create().withMinaSidor(true).withDigitalMailbox(false).withLetter(true))
			.withPayments(List.of(
				FinalizePayment.create().withPaymentDate(LocalDate.of(2026, 6, 25)).withAmount(new BigDecimal("6000.00")).withConcernedMonth("2026-06")
					.withPayee(Payee.create().withName("Hyresvärden AB").withPaymentMethod("BANKGIRO").withAccountNumber("123-4567")).withAccountingCode("5011"),
				FinalizePayment.create().withPaymentDate(LocalDate.of(2026, 6, 25)).withAmount(new BigDecimal("1900.00")).withConcernedMonth("2026-06")
					.withPayee(Payee.create().withName("Anna Andersson").withPaymentMethod("BANKKONTO").withClearing("6000").withAccountNumber("123456789"))))
			.withHouseholdSizeChanged(true);
	}

	private static FinalizeRequest rejectingRequest() {
		return FinalizeRequest.create()
			.withDecision(FinalizeDecision.create().withOutcome("AVSLAG").withReason("Tillgångar överstiger normen").withAmount(new BigDecimal("500")))
			.withCommunication(CommunicationChannels.create().withMinaSidor(false).withDigitalMailbox(true).withLetter(false))
			.withPayments(List.of());
	}

	private static SectionApprovals approvals(final boolean calculation, final boolean payment, final boolean decision) {
		return SectionApprovals.create()
			.withCalculation(SectionApproval.create().withSection("CALCULATION").withApproved(calculation))
			.withPayment(SectionApproval.create().withSection("PAYMENT").withApproved(payment))
			.withDecision(SectionApproval.create().withSection("DECISION").withApproved(decision));
	}

	/** Errand in AWAITING_DECISION, all sections approved, nothing decided yet, typed row present. */
	private void readyErrand() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("AWAITING_DECISION"));
		when(sectionApprovalServiceMock.approvals(ERRAND_ID)).thenReturn(approvals(true, true, true));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Decision.create().withDecisionType("RECOMMENDATION").withValue("OK")));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(decisionServiceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Decision.class))).thenReturn(DECISION_ID);
		// The payment rows finalize creates; their ids are returned on the receipt.
		final var ids = new java.util.concurrent.atomic.AtomicInteger();
		lenient().when(paymentServiceMock.createForDecision(eq(ERRAND_ID), any(PaymentRequest.class)))
			.thenAnswer(invocation -> "pay-" + ids.incrementAndGet());
		lenient().when(payeeServiceMock.unsyncedPayeeWarnings(eq(ERRAND_ID), anyList())).thenReturn(List.of());
	}

	@Test
	void grantingFinalizeRecordsDecisionAndPaymentsAndCorrelatesApproved() {
		readyErrand();

		final var request = grantingRequest();
		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY);

		// The receipt
		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
		assertThat(response.getProcessMessageCorrelated()).isTrue();
		assertThat(response.getCommunication()).isEqualTo(request.getCommunication());

		// 1. The audit fields land on the entity before the decision is recorded
		final var inOrder = inOrder(repositoryMock, decisionServiceMock, paymentServiceMock, processServiceMock);
		inOrder.verify(repositoryMock).save(entityCaptor.capture());
		inOrder.verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		inOrder.verify(paymentServiceMock, times(2)).createForDecision(eq(ERRAND_ID), any(PaymentRequest.class));
		inOrder.verify(processServiceMock).correlateMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("PaymentDecisionReceived"), eq(ERRAND_ID), variablesCaptor.capture());

		assertThat(entityCaptor.getValue())
			.returns(true, FinancialAssistanceEntity::getHouseholdSizeChanged)
			.returns(true, FinancialAssistanceEntity::getNotifyMinaSidor)
			.returns(false, FinancialAssistanceEntity::getNotifyDigitalMailbox)
			.returns(true, FinancialAssistanceEntity::getNotifyLetter);

		// 2. The PAYMENT decision row
		assertThat(decisionCaptor.getValue())
			.returns("PAYMENT", Decision::getDecisionType)
			.returns("BIFALL", Decision::getValue)
			.returns("Inkomster enligt SSBTEK", Decision::getDescription)
			.returns("Du beviljas ekonomiskt bistånd", Decision::getDecisionMessage)
			.returns(LocalDate.of(2026, 6, 1), Decision::getPeriodFrom)
			.returns(LocalDate.of(2026, 6, 30), Decision::getPeriodTo)
			.returns(LocalDate.now(ZoneId.systemDefault()), Decision::getDecisionDate)
			.returns(DECIDED_BY, Decision::getCreatedBy);
		assertThat(decisionCaptor.getValue().getAmount()).isEqualByComparingTo("7900.00");

		// 3. The payment rows, one per decided utbetalning, in request order
		final ArgumentCaptor<PaymentRequest> paymentRequestCaptor = ArgumentCaptor.captor();
		verify(paymentServiceMock, times(2)).createForDecision(eq(ERRAND_ID), paymentRequestCaptor.capture());
		assertThat(paymentRequestCaptor.getAllValues()).extracting(PaymentRequest::getPayeeName, PaymentRequest::getApplicationMonth, PaymentRequest::getClearingNumber)
			.containsExactly(tuple("Hyresvärden AB", "2026-06", null), tuple("Anna Andersson", "2026-06", "6000"));
		assertThat(response.getPaymentIds()).containsExactly("pay-1", "pay-2");

		// 4. The process resumes on the approved path; the status is left to the process
		assertThat(variablesCaptor.getValue()).containsExactly(Map.entry("paymentDecision", "APPROVED"));
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void rejectingFinalizeRecordsZeroAmountSkipsPaymentsAndCorrelatesRejected() {
		readyErrand();

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, rejectingRequest(), DECIDED_BY);

		assertThat(response.getProcessMessageCorrelated()).isTrue();

		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		assertThat(decisionCaptor.getValue().getValue()).isEqualTo("AVSLAG");
		assertThat(decisionCaptor.getValue().getAmount()).isEqualByComparingTo("0"); // whatever the request carried

		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue())
			.returns(false, FinancialAssistanceEntity::getHouseholdSizeChanged) // absent in the request → false
			.returns(false, FinancialAssistanceEntity::getNotifyMinaSidor)
			.returns(true, FinancialAssistanceEntity::getNotifyDigitalMailbox)
			.returns(false, FinancialAssistanceEntity::getNotifyLetter);
		verify(paymentServiceMock, never()).createForDecision(any(), any());
	}

	@Test
	void correlationFailureIsReportedNotThrown() {
		readyErrand();
		doThrow(new IllegalStateException("engine down")).when(processServiceMock).correlateMessage(any(), any(), any(), any(), anyMap());

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, rejectingRequest(), DECIDED_BY);

		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
		assertThat(response.getProcessMessageCorrelated()).isFalse();
		verify(processServiceMock).correlateMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("PaymentDecisionReceived"), eq(ERRAND_ID), variablesCaptor.capture());
		assertThat(variablesCaptor.getValue()).containsExactly(Map.entry("paymentDecision", "REJECTED"));
		// The decision is saved, so the message must not be lost: it is queued, same variables, with the reason.
		verify(processServiceMock).queueMessageRetry(MUNICIPALITY_ID, NAMESPACE, "PaymentDecisionReceived", ERRAND_ID, Map.of("paymentDecision", "REJECTED"), "engine down");
	}

	@Test
	void aFailedQueueFailsTheFinalizeRatherThanLosingTheMessage() {
		readyErrand();
		doThrow(new IllegalStateException("engine down")).when(processServiceMock).correlateMessage(any(), any(), any(), any(), anyMap());
		doThrow(new IllegalStateException("database down")).when(processServiceMock).queueMessageRetry(any(), any(), any(), any(), anyMap(), any());
		final var request = rejectingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("database down");
	}

	@Test
	void missingErrandYields404BeforeAnythingElse() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "No errand"));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(sectionApprovalServiceMock, decisionServiceMock, repositoryMock, processServiceMock);
	}

	@Test
	void missingDeciderYields400() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("AWAITING_DECISION"));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, " "))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessageContaining("X-Sent-By");

		verifyNoInteractions(sectionApprovalServiceMock, decisionServiceMock, repositoryMock, processServiceMock);
	}

	@Test
	void wrongStatusYields409() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("SUPPLEMENT_REQUESTED"));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessage("Conflict: errand must be in status AWAITING_DECISION to be finalized, but is in status 'SUPPLEMENT_REQUESTED'");

		verifyNoInteractions(sectionApprovalServiceMock, decisionServiceMock, repositoryMock, processServiceMock);
	}

	@Test
	void unapprovedSectionsYield409NamingThem() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("AWAITING_DECISION"));
		when(sectionApprovalServiceMock.approvals(ERRAND_ID)).thenReturn(approvals(true, false, false));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessage("Conflict: all sections must be approved before the errand can be finalized - not approved: PAYMENT, DECISION");

		verifyNoInteractions(decisionServiceMock, repositoryMock, processServiceMock);
	}

	@Test
	void alreadyFinalizedYields409() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("AWAITING_DECISION"));
		when(sectionApprovalServiceMock.approvals(ERRAND_ID)).thenReturn(approvals(true, true, true));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Decision.create().withDecisionType("PAYMENT").withValue("BIFALL")));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessageContaining("already carries a PAYMENT decision");

		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verifyNoInteractions(repositoryMock, processServiceMock);
	}

	@Test
	void missingTypedErrandYields404() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("AWAITING_DECISION"));
		when(sectionApprovalServiceMock.approvals(ERRAND_ID)).thenReturn(approvals(true, true, true));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No financial-assistance errand for id errand-1");

		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verifyNoInteractions(processServiceMock);
	}

	@Test
	void finalizeCarriesTheWarningForAPayeeThatIsNotInLifecareYet() {
		readyErrand();
		when(payeeServiceMock.unsyncedPayeeWarnings(eq(ERRAND_ID), anyList()))
			.thenReturn(List.of("Betalningsmottagaren \"Hyresvärden AB\" är inte upplagd i Lifecare ännu"));

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, grantingRequest(), DECIDED_BY);

		assertThat(response.getPayeeWarnings()).containsExactly("Betalningsmottagaren \"Hyresvärden AB\" är inte upplagd i Lifecare ännu");
		// The warning is not a guard: the decision and the payment rows still happened.
		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
		assertThat(response.getPaymentIds()).containsExactly("pay-1", "pay-2");
		verify(paymentServiceMock, times(2)).createForDecision(eq(ERRAND_ID), any(PaymentRequest.class));
	}

	@Test
	void finalizeMatchesTheWarningsAgainstThePayeesTheDecisionPaysTo() {
		readyErrand();
		final var payees = ArgumentCaptor.forClass(List.class);

		service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, grantingRequest(), DECIDED_BY);

		verify(payeeServiceMock).unsyncedPayeeWarnings(eq(ERRAND_ID), payees.capture());
		assertThat(payees.getValue()).extracting("name").containsExactly("Hyresvärden AB", "Anna Andersson");
	}

	@Test
	void finalizeWithoutPaymentsAsksAboutNoPayees() {
		readyErrand();
		final var payees = ArgumentCaptor.forClass(List.class);

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, rejectingRequest(), DECIDED_BY);

		verify(payeeServiceMock).unsyncedPayeeWarnings(eq(ERRAND_ID), payees.capture());
		assertThat(payees.getValue()).isEmpty();
		assertThat(response.getPayeeWarnings()).isEmpty();
	}
}
