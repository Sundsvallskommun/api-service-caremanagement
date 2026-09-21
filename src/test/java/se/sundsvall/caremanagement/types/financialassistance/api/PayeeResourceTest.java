package se.sundsvall.caremanagement.types.financialassistance.api;

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
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeLifecareResult;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeOption;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.PayeeService;

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
class PayeeResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PAYEE_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/payees";
	private static final String PAYEE_PATH = PATH + "/{payeeId}";

	private static final Map<String, String> BASE_VARS = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	private static final Map<String, String> PAYEE_VARS = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "payeeId", PAYEE_ID);

	@MockitoBean
	private PayeeService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static PayeeOption option() {
		return PayeeOption.create()
			.withId(PAYEE_ID)
			.withName("Sundsvalls Hyresbostäder AB")
			.withPaymentMethod("Bankgiro via Plusgiro")
			.withAccountNumber("5555-6666")
			.withSource("MANUAL")
			.withLifecareStatus("PENDING");
	}

	@Test
	void listPayees() {
		when(serviceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(option()));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(BASE_VARS))
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBodyList(PayeeOption.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).containsExactly(option());
		verify(serviceMock).list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void createPayee() {
		when(serviceMock.create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(PayeeRequest.class))).thenReturn(option());

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PATH).build(BASE_VARS))
			.contentType(APPLICATION_JSON)
			.bodyValue(PayeeRequest.create().withName("Sundsvalls Hyresbostäder AB").withPaymentMethod("Bankgiro via Plusgiro").withAccountNumber("5555-6666"))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectHeader().location("/%s/%s/errands/financial-assistance/%s/payees/%s".formatted(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID))
			.expectBody(PayeeOption.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(option());
		verify(serviceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(PayeeRequest.class));
	}

	@Test
	void getPayee() {
		when(serviceMock.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID)).thenReturn(option());

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PAYEE_PATH).build(PAYEE_VARS))
			.exchange()
			.expectStatus().isOk()
			.expectBody(PayeeOption.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isEqualTo(option());
		verify(serviceMock).get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID);
	}

	@Test
	void reportLifecareResult() {
		when(serviceMock.recordLifecareResult(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(PAYEE_ID), any(PayeeLifecareResult.class)))
			.thenReturn(option().withLifecareStatus("SYNCED"));

		final var response = webTestClient.post()
			.uri(uri -> uri.path(PAYEE_PATH + "/lifecare-result").build(PAYEE_VARS))
			.contentType(APPLICATION_JSON)
			.bodyValue(PayeeLifecareResult.create().withOutcome("ADDED").withLifecarePayeeId("44213"))
			.exchange()
			.expectStatus().isOk()
			.expectBody(PayeeOption.class)
			.returnResult()
			.getResponseBody();

		assertThat(response.getLifecareStatus()).isEqualTo("SYNCED");
		verify(serviceMock).recordLifecareResult(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(PAYEE_ID), any(PayeeLifecareResult.class));
	}

	@Test
	void deletePayee() {
		webTestClient.delete()
			.uri(uri -> uri.path(PAYEE_PATH).build(PAYEE_VARS))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PAYEE_ID);
	}
}
