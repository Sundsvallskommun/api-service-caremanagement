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
import se.sundsvall.caremanagement.document.service.DocumentService;
import se.sundsvall.caremanagement.journal.service.JournalEntryService;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.caremanagement.rpa.service.RpaAction;
import se.sundsvall.caremanagement.rpa.service.RpaService;
import se.sundsvall.caremanagement.rpa.service.RpaService.EnqueueOutcome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CommunicationChannels;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizePayment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Monitoring;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RpaTask;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.REGISTER_PAYMENT;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.WRITE_DECISION;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.WRITE_DOCUMENT;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.WRITE_JOURNAL;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.WRITE_MONITORING;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceFinalizeServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String DECIDED_BY = "jane02doe";
	private static final String DECISION_ID = "decision-1";
	private static final String REFERENCE_PREFIX = NAMESPACE + ":" + ERRAND_ID + ":";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private SectionApprovalService sectionApprovalServiceMock;

	@Mock
	private DecisionService decisionServiceMock;

	@Mock
	private RpaService rpaServiceMock;

	@Mock
	private ProcessService processServiceMock;

	@Mock
	private MonitoringService monitoringServiceMock;

	@Mock
	private JournalEntryService journalEntryServiceMock;

	@Mock
	private DocumentService documentServiceMock;

	@Captor
	private ArgumentCaptor<Decision> decisionCaptor;

	@Captor
	private ArgumentCaptor<FinancialAssistanceEntity> entityCaptor;

	@Captor
	private ArgumentCaptor<Map<String, String>> contentCaptor;

	@Captor
	private ArgumentCaptor<Map<String, Object>> variablesCaptor;

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
	}

	private void rpaEnqueuesEverything() {
		when(rpaServiceMock.enqueue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(RpaAction.class), any(), anyMap()))
			.thenAnswer(invocation -> {
				final RpaAction action = invocation.getArgument(3);
				final String suffix = invocation.getArgument(4);
				final String reference;
				if (suffix == null) {
					reference = REFERENCE_PREFIX + action;
				} else {
					reference = REFERENCE_PREFIX + action + ":" + suffix;
				}
				return new EnqueueOutcome(reference, true);
			});
	}

	@Test
	void grantingFinalizeRecordsDecisionEnqueuesWriteBacksAndCorrelatesApproved() {
		readyErrand();
		rpaEnqueuesEverything();
		when(monitoringServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Monitoring.create().withId("m-lifecare").withSource("LIFECARE").withLifecareId("42"),
			Monitoring.create().withId("m-local").withSource("CASEWORKER")));
		when(journalEntryServiceMock.listLocallyAuthoredIds(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of("j-1", "j-2"));
		when(documentServiceMock.listLocallyAuthoredIds(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());

		final var request = grantingRequest();
		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY);

		// The receipt
		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
		assertThat(response.getProcessMessageCorrelated()).isTrue();
		assertThat(response.getCommunication()).isEqualTo(request.getCommunication());
		assertThat(response.getRpaTasks()).extracting(RpaTask::getAction, RpaTask::getReference, RpaTask::getEnqueued).containsExactly(
			tuple("WRITE_DECISION", REFERENCE_PREFIX + "WRITE_DECISION", true),
			tuple("REGISTER_PAYMENT", REFERENCE_PREFIX + "REGISTER_PAYMENT:1", true),
			tuple("REGISTER_PAYMENT", REFERENCE_PREFIX + "REGISTER_PAYMENT:2", true),
			tuple("WRITE_MONITORING", REFERENCE_PREFIX + "WRITE_MONITORING", true),
			tuple("WRITE_JOURNAL", REFERENCE_PREFIX + "WRITE_JOURNAL", true));

		// 1. The audit fields land on the entity before the decision is recorded
		final var inOrder = inOrder(repositoryMock, decisionServiceMock, rpaServiceMock, processServiceMock);
		inOrder.verify(repositoryMock).save(entityCaptor.capture());
		inOrder.verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		inOrder.verify(rpaServiceMock).enqueue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(WRITE_DECISION), isNull(), anyMap());
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

		// 3. The queue items: decision content, one per payment with its own suffix, id lists for the local rows only
		verify(rpaServiceMock).enqueue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(WRITE_DECISION), isNull(), contentCaptor.capture());
		assertThat(contentCaptor.getValue())
			.containsEntry("decisionId", DECISION_ID)
			.containsEntry("outcome", "BIFALL")
			.containsEntry("reason", "Inkomster enligt SSBTEK")
			.containsEntry("periodFrom", "2026-06-01")
			.containsEntry("periodTo", "2026-06-30")
			.containsEntry("amount", "7900.00")
			.containsEntry("communicationChannels", "MINA_SIDOR,LETTER")
			.containsEntry("householdSizeChanged", "true");

		verify(rpaServiceMock).enqueue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(REGISTER_PAYMENT), eq("1"), contentCaptor.capture());
		assertThat(contentCaptor.getValue())
			.containsEntry("sequence", "1")
			.containsEntry("paymentDate", "2026-06-25")
			.containsEntry("amount", "6000.00")
			.containsEntry("concernedMonth", "2026-06")
			.containsEntry("payeeName", "Hyresvärden AB")
			.containsEntry("paymentMethod", "BANKGIRO")
			.containsEntry("accountNumber", "123-4567")
			.containsEntry("accountingCode", "5011")
			.doesNotContainKey("clearing");
		verify(rpaServiceMock).enqueue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(REGISTER_PAYMENT), eq("2"), contentCaptor.capture());
		assertThat(contentCaptor.getValue()).containsEntry("sequence", "2").containsEntry("clearing", "6000").containsEntry("payeeName", "Anna Andersson");

		verify(rpaServiceMock).enqueue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(WRITE_MONITORING), isNull(), contentCaptor.capture());
		assertThat(contentCaptor.getValue()).containsEntry("monitoringIds", "m-local").containsEntry("count", "1");
		verify(rpaServiceMock).enqueue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(WRITE_JOURNAL), isNull(), contentCaptor.capture());
		assertThat(contentCaptor.getValue()).containsEntry("journalEntryIds", "j-1,j-2").containsEntry("count", "2");
		verify(rpaServiceMock, never()).enqueue(any(), any(), any(), eq(WRITE_DOCUMENT), any(), anyMap());
		// WRITE_NORMBERAKNING is the commit path's item, never finalize's
		verify(rpaServiceMock, never()).enqueue(any(), any(), any(), eq(RpaAction.WRITE_NORMBERAKNING), any(), anyMap());
		verify(rpaServiceMock, never()).enqueue(any(), any(), any());

		// 4. The process resumes on the approved path; the status is left to the process
		assertThat(variablesCaptor.getValue()).containsExactly(Map.entry("paymentDecision", "APPROVED"));
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void rejectingFinalizeRecordsZeroAmountSkipsPaymentsAndCorrelatesRejected() {
		readyErrand();
		rpaEnqueuesEverything();
		when(monitoringServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(journalEntryServiceMock.listLocallyAuthoredIds(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(documentServiceMock.listLocallyAuthoredIds(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of("d-1"));

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, rejectingRequest(), DECIDED_BY);

		assertThat(response.getRpaTasks()).extracting(RpaTask::getAction).containsExactly("WRITE_DECISION", "WRITE_DOCUMENT");
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
		verify(rpaServiceMock, never()).enqueue(any(), any(), any(), eq(REGISTER_PAYMENT), any(), anyMap());
	}

	@Test
	void rpaFailuresAreReportedNotThrown() {
		readyErrand();
		when(rpaServiceMock.enqueue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(RpaAction.class), any(), anyMap()))
			.thenThrow(Problem.valueOf(INTERNAL_SERVER_ERROR, "Orchestrator down"));
		when(monitoringServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(journalEntryServiceMock.listLocallyAuthoredIds(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(documentServiceMock.listLocallyAuthoredIds(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, grantingRequest(), DECIDED_BY);

		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
		assertThat(response.getRpaTasks()).extracting(RpaTask::getAction, RpaTask::getReference, RpaTask::getEnqueued).containsExactly(
			tuple("WRITE_DECISION", null, false),
			tuple("REGISTER_PAYMENT", null, false),
			tuple("REGISTER_PAYMENT", null, false));
		// the process is still resumed — the decision is recorded, the robot steps can be re-enqueued
		verify(processServiceMock).correlateMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("PaymentDecisionReceived"), eq(ERRAND_ID), anyMap());
		assertThat(response.getProcessMessageCorrelated()).isTrue();
	}

	@Test
	void rpaDisabledIsReportedAsNotEnqueuedWithReference() {
		readyErrand();
		when(rpaServiceMock.enqueue(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(RpaAction.class), any(), anyMap()))
			.thenReturn(new EnqueueOutcome("ref", false));
		when(monitoringServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(journalEntryServiceMock.listLocallyAuthoredIds(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(documentServiceMock.listLocallyAuthoredIds(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, rejectingRequest(), DECIDED_BY);

		assertThat(response.getRpaTasks()).singleElement().satisfies(task -> {
			assertThat(task.getReference()).isEqualTo("ref");
			assertThat(task.getEnqueued()).isFalse();
		});
	}

	@Test
	void correlationFailureIsReportedNotThrown() {
		readyErrand();
		rpaEnqueuesEverything();
		when(monitoringServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(journalEntryServiceMock.listLocallyAuthoredIds(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(documentServiceMock.listLocallyAuthoredIds(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		doThrow(new IllegalStateException("engine down")).when(processServiceMock).correlateMessage(any(), any(), any(), any(), anyMap());

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, rejectingRequest(), DECIDED_BY);

		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
		assertThat(response.getProcessMessageCorrelated()).isFalse();
		verify(processServiceMock).correlateMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("PaymentDecisionReceived"), eq(ERRAND_ID), variablesCaptor.capture());
		assertThat(variablesCaptor.getValue()).containsExactly(Map.entry("paymentDecision", "REJECTED"));
	}

	@Test
	void missingErrandYields404BeforeAnythingElse() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "No errand"));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(sectionApprovalServiceMock, decisionServiceMock, repositoryMock, rpaServiceMock, processServiceMock);
	}

	@Test
	void missingDeciderYields400() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("AWAITING_DECISION"));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, " "))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessageContaining("X-Sent-By");

		verifyNoInteractions(sectionApprovalServiceMock, decisionServiceMock, repositoryMock, rpaServiceMock, processServiceMock);
	}

	@Test
	void wrongStatusYields409() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("SUPPLEMENT_REQUESTED"));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessage("Conflict: errand must be in status AWAITING_DECISION to be finalized, but is in status 'SUPPLEMENT_REQUESTED'");

		verifyNoInteractions(sectionApprovalServiceMock, decisionServiceMock, repositoryMock, rpaServiceMock, processServiceMock);
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

		verifyNoInteractions(decisionServiceMock, repositoryMock, rpaServiceMock, processServiceMock);
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
		verifyNoInteractions(repositoryMock, rpaServiceMock, processServiceMock);
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
		verifyNoInteractions(rpaServiceMock, processServiceMock);
	}
}
