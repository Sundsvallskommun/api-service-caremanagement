package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaCalculationDraftRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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
	private static final Integer LIFECARE_CALCULATION_ID = 4711;
	private static final Integer LIFECARE_DECISION_ID = 815;
	private static final List<String> LIFECARE_PAYMENT_IDS = List.of("90210", "90211");

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private DecisionService decisionServiceMock;

	@Mock
	private ProcessService processServiceMock;

	@Mock
	private FaCalculationDraftRepository calculationDraftRepositoryMock;

	@Captor
	private ArgumentCaptor<Decision> decisionCaptor;

	@Captor
	private ArgumentCaptor<FinancialAssistanceEntity> entityCaptor;

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
			.withHouseholdSizeChanged(true);
	}

	private static FinalizeRequest rejectingRequest() {
		return FinalizeRequest.create()
			.withDecision(FinalizeDecision.create().withOutcome("AVSLAG").withReason("Tillgångar överstiger normen").withAmount(new BigDecimal("500")))
			.withCommunication(CommunicationChannels.create().withMinaSidor(false).withDigitalMailbox(true).withLetter(false));
	}

	/** An errand linked to its beslut, normberäkning and payments in Lifecare — what a bifall needs. */
	private static FinancialAssistanceEntity grantable() {
		return FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)
			.withLifecareDecisionId(LIFECARE_DECISION_ID)
			.withLifecareCalculationId(LIFECARE_CALCULATION_ID)
			.withLifecarePaymentIds(LIFECARE_PAYMENT_IDS);
	}

	/** An errand linked only to its beslut — all an avslag needs. */
	private static FinancialAssistanceEntity rejectable() {
		return FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)
			.withLifecareDecisionId(LIFECARE_DECISION_ID);
	}

	/** The errand is in AWAITING_DECISION and not decided yet; the typed row is the given one. */
	private void awaitingDecision(final FinancialAssistanceEntity entity) {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("AWAITING_DECISION"));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Decision.create().withDecisionType("RECOMMENDATION").withValue("OK")));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.ofNullable(entity));
	}

	/** As {@link #awaitingDecision}, and the decision row is created. */
	private void readyErrand(final FinancialAssistanceEntity entity) {
		awaitingDecision(entity);
		when(decisionServiceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Decision.class))).thenReturn(DECISION_ID);
	}

	private void assertRefusedWithConflict(final FinalizeRequest request, final String message) {
		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessage("Conflict: " + message);

		verify(repositoryMock, never()).save(any());
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verifyNoInteractions(processServiceMock, calculationDraftRepositoryMock);
	}

	@Test
	void grantingFinalizeRecordsTheDecisionAndCorrelatesApproved() {
		readyErrand(grantable());

		final var request = grantingRequest();
		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY);

		// The receipt
		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
		assertThat(response.getProcessMessageCorrelated()).isTrue();
		assertThat(response.getCommunication()).isEqualTo(request.getCommunication());

		// 1. The audit fields land on the entity before the decision is recorded, 2. the decision, 3. the process
		final var inOrder = inOrder(repositoryMock, decisionServiceMock, processServiceMock);
		inOrder.verify(repositoryMock).save(entityCaptor.capture());
		inOrder.verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		inOrder.verify(processServiceMock).correlateMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("PaymentDecisionReceived"), eq(ERRAND_ID), variablesCaptor.capture());

		assertThat(entityCaptor.getValue())
			.returns(true, FinancialAssistanceEntity::getHouseholdSizeChanged)
			.returns(true, FinancialAssistanceEntity::getNotifyMinaSidor)
			.returns(false, FinancialAssistanceEntity::getNotifyDigitalMailbox)
			.returns(true, FinancialAssistanceEntity::getNotifyLetter)
			// The Lifecare references are left exactly as Draken linked them.
			.returns(LIFECARE_PAYMENT_IDS, FinancialAssistanceEntity::getLifecarePaymentIds);

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

		// The process resumes on the approved path; the status is left to the process
		assertThat(variablesCaptor.getValue()).containsExactly(Map.entry("paymentDecision", "APPROVED"));
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void partialGrantIsFinalizedLikeABifall() {
		readyErrand(grantable());
		final var request = grantingRequest();
		request.getDecision().setOutcome("DELAVSLAG");

		service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY);

		verify(processServiceMock).correlateMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("PaymentDecisionReceived"), eq(ERRAND_ID), variablesCaptor.capture());
		assertThat(variablesCaptor.getValue()).containsExactly(Map.entry("paymentDecision", "APPROVED"));
	}

	@Test
	void rejectingFinalizeNeedsNoCalculationNorPaymentsAndCorrelatesRejected() {
		readyErrand(rejectable());

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
		verify(processServiceMock).correlateMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("PaymentDecisionReceived"), eq(ERRAND_ID), variablesCaptor.capture());
		assertThat(variablesCaptor.getValue()).containsExactly(Map.entry("paymentDecision", "REJECTED"));
	}

	@Test
	void rejectingFinalizeAcceptsAnEmptyPaymentList() {
		readyErrand(rejectable().withLifecarePaymentIds(List.of()));

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, rejectingRequest(), DECIDED_BY);

		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
	}

	@Test
	void correlationFailureIsReportedNotThrown() {
		readyErrand(rejectable());
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
		readyErrand(rejectable());
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

		verifyNoInteractions(decisionServiceMock, repositoryMock, processServiceMock);
	}

	@Test
	void missingDeciderYields400() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("AWAITING_DECISION"));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, " "))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessageContaining("X-Sent-By");

		verifyNoInteractions(decisionServiceMock, repositoryMock, processServiceMock);
	}

	@Test
	void wrongStatusYields409() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("SUPPLEMENT_REQUESTED"));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessage("Conflict: errand must be in status AWAITING_DECISION to be finalized, but is in status 'SUPPLEMENT_REQUESTED'");

		verifyNoInteractions(decisionServiceMock, repositoryMock, processServiceMock);
	}

	@Test
	void alreadyFinalizedYields409() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID).withStatus("AWAITING_DECISION"));
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Decision.create().withDecisionType("PAYMENT").withValue("BIFALL")));
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT)
			.hasMessageContaining("already carries a PAYMENT decision");

		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verifyNoInteractions(repositoryMock, processServiceMock);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"BIFALL", "DELAVSLAG", "AVSLAG"
	})
	void anyDecisionWithoutLifecareDecisionYields409(final String outcome) {
		awaitingDecision(grantable().withLifecareDecisionId(null));
		final var request = grantingRequest();
		request.getDecision().setOutcome(outcome);

		assertRefusedWithConflict(request,
			"a decision requires the beslut to be saved in Lifecare first - save it and set lifecareDecisionId on errand 'errand-1' (PATCH .../financial-assistance/{errandId}/data) before finalizing");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"BIFALL", "DELAVSLAG"
	})
	void grantingDecisionWithoutLifecareCalculationYields409(final String outcome) {
		awaitingDecision(grantable().withLifecareCalculationId(null));
		final var request = grantingRequest();
		request.getDecision().setOutcome(outcome);

		assertRefusedWithConflict(request, "a " + outcome
			+ " decision requires the normberäkning to be saved in Lifecare first - save it and set lifecareCalculationId on errand 'errand-1' (PATCH .../financial-assistance/{errandId}/data) before finalizing");
	}

	@ParameterizedTest
	@NullAndEmptySource
	void grantingDecisionWithoutLinkedPaymentsIsFinalized(final List<String> lifecarePaymentIds) {
		// Draken registers the payments in Lifecare without handing careM their ids; the process finds them there.
		readyErrand(grantable().withLifecarePaymentIds(lifecarePaymentIds));

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, grantingRequest(), DECIDED_BY);

		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
		verify(processServiceMock).correlateMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("PaymentDecisionReceived"), eq(ERRAND_ID), variablesCaptor.capture());
		assertThat(variablesCaptor.getValue()).containsExactly(Map.entry("paymentDecision", "APPROVED"));
	}

	@Test
	void rejectionLinkedToLifecarePaymentsYields409() {
		awaitingDecision(rejectable().withLifecarePaymentIds(List.of("90210")));

		assertRefusedWithConflict(rejectingRequest(),
			"an AVSLAG decision pays nothing, but errand 'errand-1' is linked to Lifecare payments [90210] - remove them in Lifecare and clear lifecarePaymentIds before finalizing");
	}

	@Test
	void aDecisionMissingBothLifecareReferencesIsRefusedForTheBeslutFirst() {
		awaitingDecision(grantable().withLifecareDecisionId(null).withLifecareCalculationId(null));

		assertRefusedWithConflict(grantingRequest(),
			"a decision requires the beslut to be saved in Lifecare first - save it and set lifecareDecisionId on errand 'errand-1' (PATCH .../financial-assistance/{errandId}/data) before finalizing");
	}

	@Test
	void rejectionWithASavedNormberakningIsStillFinalized() {
		// A caseworker may save the normberäkning in Lifecare and still reject; the calculation id is simply not needed.
		readyErrand(rejectable().withLifecareCalculationId(LIFECARE_CALCULATION_ID));

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, rejectingRequest(), DECIDED_BY);

		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
		verify(processServiceMock).correlateMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("PaymentDecisionReceived"), eq(ERRAND_ID), variablesCaptor.capture());
		assertThat(variablesCaptor.getValue()).containsExactly(Map.entry("paymentDecision", "REJECTED"));
	}

	@Test
	void partialGrantLinkedToLifecarePaymentsIsFinalized() {
		// Only an avslag is refused for linked payments; a delavslag pays, so its payments belong to it.
		readyErrand(grantable());
		final var request = grantingRequest();
		request.getDecision().setOutcome("DELAVSLAG");

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY);

		assertThat(response.getDecisionId()).isEqualTo(DECISION_ID);
	}

	@Test
	void grantingFinalizePurgesTheFrozenDraftOnceTheDecisionIsRecorded() {
		readyErrand(grantable());
		when(calculationDraftRepositoryMock.existsById(ERRAND_ID)).thenReturn(true);

		service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, grantingRequest(), DECIDED_BY);

		// The decision is recorded and the process resumed before careM's copy of the normberäkning is disposed of.
		final var inOrder = inOrder(decisionServiceMock, processServiceMock, calculationDraftRepositoryMock);
		inOrder.verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(Decision.class));
		inOrder.verify(processServiceMock).correlateMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq("PaymentDecisionReceived"), eq(ERRAND_ID), anyMap());
		inOrder.verify(calculationDraftRepositoryMock).deleteById(ERRAND_ID);
	}

	@Test
	void rejectionWithASavedNormberakningPurgesTheDraftToo() {
		readyErrand(rejectable().withLifecareCalculationId(LIFECARE_CALCULATION_ID));
		when(calculationDraftRepositoryMock.existsById(ERRAND_ID)).thenReturn(true);

		service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, rejectingRequest(), DECIDED_BY);

		verify(calculationDraftRepositoryMock).deleteById(ERRAND_ID);
	}

	@Test
	void rejectionWithoutASavedNormberakningKeepsTheDraft() {
		// No calculation in Lifecare: the draft is the only trace of the proposal and stays for the errand's own disposal.
		readyErrand(rejectable());

		service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, rejectingRequest(), DECIDED_BY);

		verifyNoInteractions(calculationDraftRepositoryMock);
	}

	@Test
	void anErrandWithoutADraftHasNothingToPurge() {
		readyErrand(grantable());
		when(calculationDraftRepositoryMock.existsById(ERRAND_ID)).thenReturn(false);

		service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, grantingRequest(), DECIDED_BY);

		verify(calculationDraftRepositoryMock, never()).deleteById(any());
	}

	@Test
	void theDraftIsPurgedEvenWhenTheProcessCouldNotBeReached() {
		readyErrand(grantable());
		when(calculationDraftRepositoryMock.existsById(ERRAND_ID)).thenReturn(true);
		doThrow(new IllegalStateException("engine down")).when(processServiceMock).correlateMessage(any(), any(), any(), any(), anyMap());

		final var response = service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, grantingRequest(), DECIDED_BY);

		// The decision stands and the message is queued, so the errand is decided: the draft goes either way.
		assertThat(response.getProcessMessageCorrelated()).isFalse();
		verify(calculationDraftRepositoryMock).deleteById(ERRAND_ID);
	}

	@Test
	void missingTypedErrandYields404() {
		awaitingDecision(null);
		final var request = grantingRequest();

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No financial-assistance errand for id errand-1");

		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verifyNoInteractions(processServiceMock);
	}
}
