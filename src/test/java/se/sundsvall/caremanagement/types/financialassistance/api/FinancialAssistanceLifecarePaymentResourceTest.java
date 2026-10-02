package se.sundsvall.caremanagement.types.financialassistance.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentBalance;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentConcernMonth;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentCreated;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentMethod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentOptions;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentPosting;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentProposal;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentStatus;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRegisteredPayment;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.ErrandLifecarePaymentService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentRegistrationService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecarePaymentResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare";
	private static final Map<String, String> VARS = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	private static final LifecarePayee PAYEE = new LifecarePayee(2, "Konto A", "Kontoinnehavare A", 14, "Bankgiro via Plusgiro", "", "11111111", "", "", "", "Sundsvall",
		false);

	@MockitoBean
	private ErrandLifecarePaymentService paymentServiceMock;
	@MockitoBean
	private LifecarePaymentRegistrationService registrationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void readPaymentOptions() {
		final var options = new LifecarePaymentOptions(
			List.of(new LifecarePaymentMethod(14, "Bankgiro via Plusgiro", false, false)),
			List.of(PAYEE),
			List.of(new LifecarePaymentPosting(1, "Försörjningsstöd")),
			List.of(new LifecarePaymentBalance("Ek. Bistånd", BigDecimal.valueOf(4), BigDecimal.ONE, BigDecimal.valueOf(3))),
			List.of(new LifecarePaymentConcernMonth("2026-09", "September 2026")),
			new LifecarePaymentProposal("2026-09-21", "2026-09", BigDecimal.valueOf(3), 2));
		when(paymentServiceMock.paymentOptions(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(options);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/payment-options").build(VARS))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(LifecarePaymentOptions.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(options);
		verify(paymentServiceMock).paymentOptions(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoMoreInteractions(paymentServiceMock, registrationServiceMock);
	}

	@Test
	void readPaymentStatus() {
		final var status = new LifecarePaymentStatus("2026-09", true, "2026-09-21", BigDecimal.ONE, "Utbetald", false);
		when(paymentServiceMock.paymentStatus(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(status);

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/payment-status").build(VARS))
			.exchange()
			.expectStatus().isOk()
			.expectBody(LifecarePaymentStatus.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(status);
		verify(paymentServiceMock).paymentStatus(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readPayments() {
		final var payment = new LifecareRegisteredPayment(4, "2026-09-21", "2026-09", BigDecimal.ONE, "Bankgiro via Plusgiro", "Kontoinnehavare A", "Utbetald", false);
		when(paymentServiceMock.registeredPayments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(payment));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/payments").build(VARS))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(LifecareRegisteredPayment.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).containsExactly(payment);
		verify(paymentServiceMock).registeredPayments(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void registerPayment() {
		final var request = new LifecarePaymentRequest("2026-09-21", BigDecimal.ONE, "2026-09", "Bankgiro via Plusgiro", "Kontoinnehavare A", null, null, null, "Sundsvall",
			"", "11111111", "1", null, "123", false, List.of("Hyra september"));
		when(registrationServiceMock.register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request)).thenReturn(new LifecarePaymentCreated("4", true));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/payments").build(VARS))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(LifecarePaymentCreated.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(new LifecarePaymentCreated("4", true));
		verify(registrationServiceMock).register(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request);
		verifyNoMoreInteractions(paymentServiceMock, registrationServiceMock);
	}

	@Test
	void createPayee() {
		final var request = new LifecarePayeeRequest("Kontoinnehavare A", "Konto A", 14, null, "11111111");
		when(paymentServiceMock.createPayee(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request)).thenReturn(PAYEE);

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/payees").build(VARS))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(LifecarePayee.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(PAYEE);
		verify(paymentServiceMock).createPayee(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request);
	}
}
