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
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentCount;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentLifecareResult;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.PaymentService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class PaymentResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";

	private static final String PAYMENT_ID = randomUUID().toString();
	private static final String PAYMENT_PATH = PATH + "/{errandId}/payments";

	@MockitoBean
	private PaymentService paymentServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static PaymentRequest request() {
		return PaymentRequest.create().withMoneyType("FORSORJNINGSSTOD").withAmount(new BigDecimal("4500.00")).withApplicationMonth("2026-08");
	}

	private Map<String, ?> paymentBase() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	}

	private Map<String, ?> withPayment() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "paymentId", PAYMENT_ID);
	}

	@Test
	void createPayment() {
		when(paymentServiceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(PaymentRequest.class)))
			.thenReturn(Payment.create().withId(PAYMENT_ID).withStatus("DRAFT").withMoneyType("FORSORJNINGSSTOD"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PAYMENT_PATH).build(paymentBase()))
			.bodyValue(request())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID + "/payments/" + PAYMENT_ID)
			.expectBody(Payment.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(PAYMENT_ID);
		assertThat(response.getStatus()).isEqualTo("DRAFT");
		verify(paymentServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(PaymentRequest.class));
	}

	@Test
	void listPayments() {
		when(paymentServiceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(Payment.create().withId(PAYMENT_ID).withMoneyType("FORSORJNINGSSTOD")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PAYMENT_PATH).build(paymentBase()))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Payment.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().getMoneyType()).isEqualTo("FORSORJNINGSSTOD");
		verify(paymentServiceMock).list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void countPayments() {
		when(paymentServiceMock.count(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(2L);

		final var body = webTestClient.get()
			.uri(uri -> uri.path(PAYMENT_PATH + "/count").build(paymentBase()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(PaymentCount.class)
			.returnResult()
			.getResponseBody();

		assertThat(body).isNotNull();
		assertThat(body.count()).isEqualTo(2L);
		verify(paymentServiceMock).count(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void getPayment() {
		when(paymentServiceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYMENT_ID))
			.thenReturn(Payment.create().withId(PAYMENT_ID).withMoneyType("FORSORJNINGSSTOD"));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PAYMENT_PATH + "/{paymentId}").build(withPayment()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Payment.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getId()).isEqualTo(PAYMENT_ID);
		verify(paymentServiceMock).get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYMENT_ID);
	}

	@Test
	void updatePayment() {
		when(paymentServiceMock.update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(PAYMENT_ID), any(PaymentRequest.class)))
			.thenReturn(Payment.create().withId(PAYMENT_ID).withMoneyType("ANNAN_TYP"));

		final var response = webTestClient.put()
			.uri(uri -> uri.path(PAYMENT_PATH + "/{paymentId}").build(withPayment()))
			.bodyValue(request().withMoneyType("ANNAN_TYP"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Payment.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getMoneyType()).isEqualTo("ANNAN_TYP");
		verify(paymentServiceMock).update(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(PAYMENT_ID), any(PaymentRequest.class));
	}

	@Test
	void reportLifecareResult() {
		when(paymentServiceMock.recordLifecareResult(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(PAYMENT_ID), any(PaymentLifecareResult.class)))
			.thenReturn(Payment.create().withId(PAYMENT_ID).withStatus("REGISTERED").withLifecareId("4"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PAYMENT_PATH + "/{paymentId}/lifecare-result").build(withPayment()))
			.contentType(APPLICATION_JSON)
			.bodyValue(PaymentLifecareResult.create().withOutcome("REGISTERED").withLifecarePaymentId("4"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Payment.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull();
		assertThat(response.getStatus()).isEqualTo("REGISTERED");
		assertThat(response.getLifecareId()).isEqualTo("4");
		verify(paymentServiceMock).recordLifecareResult(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(PAYMENT_ID), any(PaymentLifecareResult.class));
	}

	@Test
	void deletePayment() {
		webTestClient.delete()
			.uri(uri -> uri.path(PAYMENT_PATH + "/{paymentId}").build(withPayment()))
			.exchange()
			.expectStatus().isNoContent();

		verify(paymentServiceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYMENT_ID);
	}
}
