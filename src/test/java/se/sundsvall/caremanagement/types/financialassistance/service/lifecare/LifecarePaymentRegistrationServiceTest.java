package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentCreated;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.payment;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.underlag;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LifecarePaymentRegistrationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "e1";
	private static final LifecareErrand ERRAND = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1, null, null, null, 2026, 9);
	private static final String EXISTING = """
		{ "paymentId": 4, "amount": 1.0, "payDate": "2026-09-21", "concernedMonth": "202609", "accountNumber": "11111111", "cancellationDate": "" }
		""";

	@Mock
	private LifecareErrandService errandService;
	@Mock
	private LifecareAccessRecorder accessRecorder;
	@Mock
	private LifecarePaymentApi paymentApi;

	@InjectMocks
	private LifecarePaymentRegistrationService service;

	@Captor
	private ArgumentCaptor<JsonNode> bodyCaptor;

	private static void assertProblem(final Throwable thrown, final HttpStatus status, final String detail) {
		assertThat(thrown).isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
			assertThat(problem.getStatus()).isEqualTo(status);
			assertThat(problem.getDetail()).contains(detail);
		});
	}

	@BeforeEach
	void setUp() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(paymentApi.readPaymentForCreate(1)).thenReturn(underlag());
		when(paymentApi.readLatestPayments(1)).thenReturn(json("[]"));
		when(paymentApi.hasHouseholdOn("19800101T001", "2026-09-21")).thenReturn(true);
	}

	@Test
	void registersTheUtbetalningInLifecareLinksItAndAnswersWithItsId() {
		when(paymentApi.createPayment(eq(1), any())).thenReturn(json("{ \"paymentId\": 4 }"));

		final var created = service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, payment());

		assertThat(created).isEqualTo(new LifecarePaymentCreated("4", true));
		verify(paymentApi).createPayment(eq(1), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().get("amount").decimalValue()).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(bodyCaptor.getValue().get("concernedMonth").asString()).isEqualTo("202609");
		assertThat(bodyCaptor.getValue().get("billingNumber").asString()).isEqualTo("123");
		verify(errandService).linkPayment(ERRAND, "4");
		verify(accessRecorder).read(ERRAND, "PAYEES", "Läste utbetalningsunderlag i Lifecare");
		verify(accessRecorder).written(ERRAND, "CREATE", "PAYMENT", "Registrerade en utbetalning i Lifecare", "4");
	}

	@Test
	void refusesAnUtbetalningLifecareAlreadyHoldsInsteadOfPayingItTwice() {
		when(paymentApi.readLatestPayments(1)).thenReturn(json("[" + EXISTING + "]"));

		assertProblem(catchThrowable(() -> service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, payment())), CONFLICT, "id 4");
		verify(paymentApi, never()).createPayment(anyInt(), any());
	}

	@Test
	void doesNotTakeAMakuleradOrDifferentUtbetalningForTheSameOne() {
		final var existing = (ObjectNode) json(EXISTING);
		when(paymentApi.readLatestPayments(1)).thenReturn(json("[%s, %s, %s]".formatted(
			existing.deepCopy().put("cancellationDate", "2026-09-22"),
			existing.deepCopy().put("paymentId", 5).put("amount", 2),
			existing.deepCopy().put("paymentId", 6).put("payDate", "2026-09-08"))));
		when(paymentApi.createPayment(eq(1), any())).thenReturn(json("{ \"paymentId\": 7 }"));

		assertThat(service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, payment()).lifecareId()).isEqualTo("7");
	}

	@Test
	void doesNotSendAnUtbetalningForAPersonWithoutHushallOnThePaymentDate() {
		when(paymentApi.hasHouseholdOn("19800101T001", "2026-09-21")).thenReturn(false);

		assertProblem(catchThrowable(() -> service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, payment())), UNPROCESSABLE_CONTENT,
			"inget hushåll");
		verify(paymentApi, never()).createPayment(anyInt(), any());
	}

	@Test
	void doesNotSendAnUtbetalningWhenLifecareNamesNoPerson() {
		final var underlag = underlag();
		((ObjectNode) underlag.get("payment")).put("susPersonId", "");
		when(paymentApi.readPaymentForCreate(1)).thenReturn(underlag);

		assertProblem(catchThrowable(() -> service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, payment())), UNPROCESSABLE_CONTENT,
			"inget hushåll");
		verify(paymentApi, never()).hasHouseholdOn(anyString(), anyString());
		verify(paymentApi, never()).createPayment(anyInt(), any());
	}

	@Test
	void doesNotSendOneItCannotVouchForAndSaysWhy() {
		final var tooMuch = payment(BigDecimal.valueOf(50), "Bankgiro via Plusgiro", "2026-09", "11111111", null);

		assertProblem(catchThrowable(() -> service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, tooMuch)), UNPROCESSABLE_CONTENT,
			"räcker inte");
		verify(paymentApi, never()).createPayment(anyInt(), any());
	}

	@Test
	void passesARefusalFromLifecareOnInLifecaresWords() {
		final var refusal = Problem.valueOf(UNPROCESSABLE_CONTENT, "Lifecare godtog inte uppgifterna.");
		when(paymentApi.createPayment(eq(1), any())).thenThrow(refusal);

		assertThatThrownBy(() -> service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, payment())).isSameAs(refusal);
		verify(errandService, never()).linkPayment(any(), anyString());
	}

	@Test
	void saysToCheckLifecareWhenItDidNotAnswerAndNeverRetries() {
		when(paymentApi.createPayment(eq(1), any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare could not be reached (HttpTimeoutException)"));

		assertProblem(catchThrowable(() -> service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, payment())), BAD_GATEWAY, "Kontrollera");
		verify(paymentApi, times(1)).createPayment(anyInt(), any());
		verify(accessRecorder, never()).written(any(), anyString(), anyString(), anyString(), any());
	}

	@Test
	void saysToCheckLifecareOnAnyOtherFailureOfTheCreate() {
		when(paymentApi.createPayment(eq(1), any())).thenThrow(new IllegalStateException("boom"));

		assertProblem(catchThrowable(() -> service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, payment())), BAD_GATEWAY, "Kontrollera");
	}

	@Test
	void saysToCheckLifecareWhenItsAnswerCarriesNoId() {
		when(paymentApi.createPayment(eq(1), any())).thenReturn(json("{}"));

		assertProblem(catchThrowable(() -> service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, payment())), BAD_GATEWAY, "Kontrollera");
		verify(errandService, never()).linkPayment(any(), anyString());
	}

	@Test
	void keepsTheRegistrationAndSaysSoWhenTheErrandCannotBeLinked() {
		when(paymentApi.createPayment(eq(1), any())).thenReturn(json("{ \"paymentId\": 4 }"));
		doThrow(new IllegalStateException("db down")).when(errandService).linkPayment(ERRAND, "4");

		assertThat(service.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, payment())).isEqualTo(new LifecarePaymentCreated("4", false));
		verify(errandService, times(1)).linkPayment(ERRAND, "4");
		verify(accessRecorder).written(ERRAND, "CREATE", "PAYMENT", "Registrerade en utbetalning i Lifecare", "4");
	}
}
