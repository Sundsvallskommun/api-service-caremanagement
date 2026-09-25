package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CommunicationChannels;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeResponse;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceFinalizeService;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapperTest.SAVED;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapperTest.tree;

/**
 * Ported from the Draken BFF's finalize-request.test.ts and errand-finalize.service.test.ts: the finalize request is
 * built from the beslut as it stands in Lifecare.
 */
@ExtendWith(MockitoExtension.class)
class LifecareFinalizeServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "errand-1";
	private static final String DECIDED_BY = "jane02doe";
	private static final CommunicationChannels COMMUNICATION = CommunicationChannels.create().withMinaSidor(true).withDigitalMailbox(false).withLetter(true);

	@Mock
	private LifecareErrandService errandServiceMock;

	@Mock
	private LifecareDecisionClient lifecareMock;

	@Mock
	private LifecareAccessRecorder accessRecorderMock;

	@Mock
	private FinancialAssistanceFinalizeService finalizeServiceMock;

	@Captor
	private ArgumentCaptor<FinalizeRequest> requestCaptor;

	private LifecareFinalizeService service;

	@BeforeEach
	void setUp() {
		final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		service = new LifecareFinalizeService(errandServiceMock, lifecareMock, accessRecorderMock, finalizeServiceMock, validator);
	}

	private static FinalizeRequest withoutDecision() {
		return FinalizeRequest.create().withCommunication(COMMUNICATION).withHouseholdSizeChanged(true);
	}

	private LifecareErrand linkedTo(final Integer decisionId) {
		final var errand = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1, 31, decisionId, null, 2026, 9);
		when(errandServiceMock.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand);
		return errand;
	}

	@Test
	void finalizesARequestCarryingADecisionAsItIs() {
		final var request = withoutDecision().withDecision(FinalizeDecision.create().withOutcome("AVSLAG"));
		final var response = FinalizeResponse.create().withDecisionId("decision-1");
		when(finalizeServiceMock.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY)).thenReturn(response);

		assertThat(service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request, DECIDED_BY)).isSameAs(response);
		verifyNoInteractions(errandServiceMock, lifecareMock, accessRecorderMock);
	}

	@Test
	void buildsTheDecisionFromTheLifecareBeslut() {
		final var errand = linkedTo(98);
		when(lifecareMock.readDecision(98)).thenReturn(tree(SAVED));
		final var response = FinalizeResponse.create().withDecisionId("decision-1");
		when(finalizeServiceMock.finalize(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), requestCaptor.capture(), eq(DECIDED_BY))).thenReturn(response);

		assertThat(service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, withoutDecision(), DECIDED_BY)).isSameAs(response);

		final var request = requestCaptor.getValue();
		assertThat(request.getDecision()).isEqualTo(FinalizeDecision.create()
			.withOutcome("BIFALL")
			.withReason("Arbetar deltid ofrivilligt, otillräcklig inkomst")
			.withPeriodFrom(LocalDate.of(2026, 9, 1))
			.withPeriodTo(LocalDate.of(2026, 9, 30))
			.withAmount(new BigDecimal("3000"))
			.withDecisionMessage("<p>Beslut</p>"));
		assertThat(request.getCommunication()).isEqualTo(COMMUNICATION);
		assertThat(request.getHouseholdSizeChanged()).isTrue();
		verify(accessRecorderMock).read(errand, "DECISION", "Läste beslutet i Lifecare", "98");
	}

	@Test
	void sendsNoAmountForAnAvslagAndReadsAPeriodWithATime() {
		linkedTo(98);
		final var avslag = (ObjectNode) tree(SAVED);
		avslag.put("decisionType", 10).put("amount", 500).put("fromDate", "2026-09-01T00:00:00").put("toDate", "");
		when(lifecareMock.readDecision(98)).thenReturn(avslag);
		when(finalizeServiceMock.finalize(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), requestCaptor.capture(), eq(DECIDED_BY)))
			.thenReturn(FinalizeResponse.create());

		service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, withoutDecision(), DECIDED_BY);

		final var decision = requestCaptor.getValue().getDecision();
		assertThat(decision.getOutcome()).isEqualTo("AVSLAG");
		assertThat(decision.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(decision.getPeriodFrom()).isEqualTo(LocalDate.of(2026, 9, 1));
		assertThat(decision.getPeriodTo()).isNull();
	}

	@Test
	void refusesWhenNoBeslutHasBeenSaved() {
		linkedTo(null);

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, withoutDecision(), DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessageContaining("Spara beslutet");
		verifyNoInteractions(lifecareMock, finalizeServiceMock);
	}

	@Test
	void refusesALifecareBeslutstypCaremDoesNotFinalize() {
		linkedTo(98);
		final var recovery = (ObjectNode) tree(SAVED);
		recovery.put("decisionType", 9);
		when(lifecareMock.readDecision(98)).thenReturn(recovery);

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, withoutDecision(), DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessageContaining("inte går att verkställa");
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void refusesABeslutThatBreaksTheDecisionsRules() {
		linkedTo(98);
		final var tooLong = (ObjectNode) tree(SAVED);
		tooLong.put("message", "x".repeat(8193)).put("toDate", "2026-08-31");
		when(lifecareMock.readDecision(98)).thenReturn(tooLong);

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, withoutDecision(), DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("decision.decisionMessage")
			.hasMessageContaining("decision.periodTo");
		verifyNoInteractions(finalizeServiceMock);
	}

	@Test
	void refusesAPeriodLifecareWroteInAnUnreadableForm() {
		linkedTo(98);
		final var odd = (ObjectNode) tree(SAVED);
		odd.put("fromDate", "01.09.2026");
		when(lifecareMock.readDecision(98)).thenReturn(odd);

		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, withoutDecision(), DECIDED_BY))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
		verify(finalizeServiceMock, never()).finalize(any(), any(), any(), any(), any());
	}

	@Test
	void readsNothingForAnUnidentifiedCaller() {
		assertThatThrownBy(() -> service.finalize(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, withoutDecision(), " "))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
		verifyNoInteractions(errandServiceMock, lifecareMock, finalizeServiceMock);
	}
}
