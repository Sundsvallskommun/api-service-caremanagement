package se.sundsvall.caremanagement.types.financialassistance.api;

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
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.PayeeService;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class PayeeResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PAYEE_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/payees";
	private static final String PAYEE_PATH = PATH + "/{payeeId}";

	private static final String INVALID_MUNICIPALITY_ID = "bad-municipality-id";
	private static final String INVALID_UUID = "not-a-valid-uuid";

	@MockitoBean
	private PayeeService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static Map<String, String> vars(final String municipalityId, final String errandId) {
		return Map.of("municipalityId", municipalityId, "namespace", NAMESPACE, "errandId", errandId);
	}

	private static Map<String, String> payeeVars(final String payeeId) {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "payeeId", payeeId);
	}

	@Test
	void listWithInvalidMunicipalityIdIsBadRequest() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(vars(INVALID_MUNICIPALITY_ID, ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void listWithInvalidErrandIdIsBadRequest() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(vars(MUNICIPALITY_ID, INVALID_UUID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithoutNameIsBadRequest() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(vars(MUNICIPALITY_ID, ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(PayeeRequest.create().withPaymentMethod("Plusgiro"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithoutPaymentMethodIsBadRequest() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(vars(MUNICIPALITY_ID, ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(PayeeRequest.create().withName("Ny Mottagare"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void createWithTooLongNameIsBadRequest() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(vars(MUNICIPALITY_ID, ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(PayeeRequest.create().withName("a".repeat(256)).withPaymentMethod("Plusgiro"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void getWithInvalidPayeeIdIsBadRequest() {
		webTestClient.get()
			.uri(uri -> uri.path(PAYEE_PATH).build(payeeVars(INVALID_UUID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void lifecareResultWithUnknownOutcomeIsBadRequest() {
		webTestClient.post()
			.uri(uri -> uri.path(PAYEE_PATH + "/lifecare-result").build(payeeVars(PAYEE_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(PayeeLifecareResult.create().withOutcome("MAYBE"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void lifecareResultWithoutOutcomeIsBadRequest() {
		webTestClient.post()
			.uri(uri -> uri.path(PAYEE_PATH + "/lifecare-result").build(payeeVars(PAYEE_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(PayeeLifecareResult.create().withLifecarePayeeId("44213"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void deleteWithInvalidPayeeIdIsBadRequest() {
		webTestClient.delete()
			.uri(uri -> uri.path(PAYEE_PATH).build(payeeVars(INVALID_UUID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}
}
