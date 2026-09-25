package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentStatus;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.underlag;

@ExtendWith(MockitoExtension.class)
class ErrandLifecarePaymentServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "e1";
	private static final LifecareErrand ERRAND = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1, null, null, null, 2026, 9);
	private static final String REGISTERED = """
		[
		  { "paymentId": 4, "amount": 1.0, "payDate": "2026-09-21", "concernedMonth": "202609", "accountNumber": "11111111", "cancellationDate": "" },
		  { "paymentId": 5, "amount": 2.0, "payDate": "2026-09-22", "concernedMonth": "202609", "accountNumber": "22222222", "cancellationDate": "2026-09-22" }
		]
		""";

	@Mock
	private LifecareErrandService errandService;
	@Mock
	private LifecareAccessRecorder accessRecorder;
	@Mock
	private LifecarePaymentApi paymentApi;

	@InjectMocks
	private ErrandLifecarePaymentService service;

	@Captor
	private ArgumentCaptor<JsonNode> bodyCaptor;

	private static LifecareErrand errand(final Integer serviceId, final Integer year, final Integer month) {
		return new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, serviceId, null, null, null, year, month);
	}

	@Test
	void paymentOptions() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(paymentApi.readPaymentForCreate(1)).thenReturn(underlag());
		when(paymentApi.readLatestPayments(1)).thenReturn(json(REGISTERED));

		final var options = service.paymentOptions(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(options.proposal().payeeId()).isEqualTo(2);
		assertThat(options.payees()).hasSize(2);
		verify(accessRecorder).read(ERRAND, "PAYEES", "Läste utbetalningsunderlag i Lifecare");
	}

	@Test
	void paymentOptionsNotServedWhenTheReadCannotBeLogged() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(paymentApi.readPaymentForCreate(1)).thenReturn(underlag());
		when(paymentApi.readLatestPayments(1)).thenReturn(json(REGISTERED));
		doThrow(new IllegalStateException("log down")).when(accessRecorder).read(any(), anyString(), anyString());

		assertThatThrownBy(() -> service.paymentOptions(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void paymentStatus() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(paymentApi.readLatestPayments(1)).thenReturn(json(REGISTERED));

		final var status = service.paymentStatus(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(status).isEqualTo(new LifecarePaymentStatus("2026-09", true, "2026-09-21", new BigDecimal("1.0"), null, false));
		verify(accessRecorder).read(ERRAND, "PAYMENTS", "Läste utbetalningar i Lifecare");
	}

	@Test
	void paymentStatusUnavailableWithoutApplicationMonth() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand(1, 2026, null), errand(1, null, 9), errand(1, 0, 9), errand(1, 2026, 0));

		for (var call = 0; call < 4; call++) {
			assertThat(service.paymentStatus(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(new LifecarePaymentStatus(null, false, null, null, null, true));
		}
		verifyNoInteractions(paymentApi, accessRecorder);
	}

	@Test
	void paymentStatusUnavailableWhenLifecareCannotBeRead() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(paymentApi.readLatestPayments(1)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare could not be reached"));

		assertThat(service.paymentStatus(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(new LifecarePaymentStatus("2026-09", false, null, null, null, true));
		verifyNoInteractions(accessRecorder);
	}

	@Test
	void paymentStatusUnavailableWithoutInsats() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand(null, 2026, 9));

		assertThat(service.paymentStatus(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).unavailable()).isTrue();
		verifyNoInteractions(paymentApi);
	}

	@Test
	void registeredPayments() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(paymentApi.readLatestPayments(1)).thenReturn(json(REGISTERED));

		assertThat(service.registeredPayments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).hasSize(2);
		verify(accessRecorder).read(ERRAND, "PAYMENTS", "Läste utbetalningar i Lifecare");
	}

	@Test
	void registeredPaymentsWithoutInsats() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errand(null, 2026, 9));

		assertThatThrownBy(() -> service.registeredPayments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(CONFLICT));
		verifyNoInteractions(paymentApi, accessRecorder);
	}

	@Test
	void createPayeeFilesItUnderThePersonLifecareNamesForTheInsats() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(paymentApi.readPaymentForCreate(1)).thenReturn(underlag());
		when(paymentApi.createPayee(any())).thenReturn(json("""
			{ "payeeId": 3, "payeeName": "Konto B", "personId": "19800101T001", "paymentMethod": 14, "paymentMethodText": null,
			  "accountNumber": "22222222", "clearing": "", "name": "Kontoinnehavare B", "isActive": true }
			"""));

		final var created = service.createPayee(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecarePayeeRequest("Kontoinnehavare B", null, 14, null, "22222222"));

		assertThat(created.id()).isEqualTo(3);
		assertThat(created.paymentMethod()).isEqualTo("Bankgiro via Plusgiro");
		verify(paymentApi).createPayee(bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().get("personId").asString()).isEqualTo("19800101T001");
		assertThat(bodyCaptor.getValue().get("paymentMethod").asInt()).isEqualTo(14);
		assertThat(bodyCaptor.getValue().get("accountNumber").asString()).isEqualTo("22222222");
		verify(accessRecorder).read(ERRAND, "PAYEES", "Läste betalningsmottagare i Lifecare");
		verify(accessRecorder).written(ERRAND, "CREATE", "PAYEE", "Lade till en betalningsmottagare i Lifecare", "3");
	}

	@Test
	void createPayeeReturnsTheExistingPayeeInsteadOfCreatingTheSameOneTwice() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(paymentApi.readPaymentForCreate(1)).thenReturn(underlag());

		final var payee = service.createPayee(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecarePayeeRequest("Någon", null, 14, null, "1111-1111"));

		assertThat(payee.id()).isEqualTo(2);
		verify(paymentApi, never()).createPayee(any());
		verify(accessRecorder, never()).written(any(), anyString(), anyString(), anyString(), any());
	}

	@Test
	void createPayeeRefusesABetalsattTheInsatsDoesNotOffer() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(paymentApi.readPaymentForCreate(1)).thenReturn(underlag());

		assertThatThrownBy(() -> service.createPayee(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecarePayeeRequest("Någon", null, 21, null, "1")))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(BAD_REQUEST));
		verify(paymentApi, never()).createPayee(any());
	}

	@Test
	void createPayeeFailsWhenLifecareNamesNoPerson() {
		final var underlag = underlag();
		((ObjectNode) underlag.get("payment")).putNull("susPersonId");
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
		when(paymentApi.readPaymentForCreate(1)).thenReturn(underlag);

		assertThatThrownBy(() -> service.createPayee(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new LifecarePayeeRequest("Ny", null, 14, null, "33333333")))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY));
		verify(paymentApi, never()).createPayee(any());
		verify(accessRecorder).read(eq(ERRAND), eq("PAYEES"), anyString());
		verifyNoMoreInteractions(accessRecorder);
	}
}
