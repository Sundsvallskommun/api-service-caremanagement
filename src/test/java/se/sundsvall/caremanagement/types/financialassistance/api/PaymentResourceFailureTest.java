package se.sundsvall.caremanagement.types.financialassistance.api;

import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.PaymentService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class PaymentResourceFailureTest {
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PAYMENT_PATH = PATH + "/{errandId}/payments";

	@MockitoBean
	private PaymentService paymentServiceMock;
	@Autowired
	private WebTestClient webTestClient;

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.isNotEmpty()
			.allSatisfy(violation -> assertThat(violation.field()).isNotBlank())
			.allSatisfy(violation -> assertThat(violation.message()).isNotBlank());
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}

	private Map<String, ?> paymentBase() {
		return Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	}

	@Test
	void createPaymentInvalidSource() {
		webTestClient.post()
			.uri(uri -> uri.path(PAYMENT_PATH).build(paymentBase()))
			.bodyValue(PaymentRequest.create().withSource("SOMETHING_ELSE"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("source", "must be one of: [CASEWORKER, LIFECARE]")));

		verifyNoInteractions(paymentServiceMock);
	}

	@Test
	void createPaymentInvalidApplicationMonth() {
		webTestClient.post()
			.uri(uri -> uri.path(PAYMENT_PATH).build(paymentBase()))
			.bodyValue(PaymentRequest.create().withApplicationMonth("2026-13"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("applicationMonth", "must be in the format yyyy-MM")));

		verifyNoInteractions(paymentServiceMock);
	}

	@Test
	void createPaymentTooLongPayeeName() {
		webTestClient.post()
			.uri(uri -> uri.path(PAYMENT_PATH).build(paymentBase()))
			.bodyValue(PaymentRequest.create().withPayeeName("a".repeat(256)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("payeeName", "size must be between 0 and 255")));

		verifyNoInteractions(paymentServiceMock);
	}

	@Test
	void createPaymentInvalidErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(PAYMENT_PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(PaymentRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createPayment.errandId", "not a valid UUID")));

		verifyNoInteractions(paymentServiceMock);
	}

	@Test
	void getPaymentInvalidPaymentId() {
		webTestClient.get()
			.uri(uri -> uri.path(PAYMENT_PATH + "/{paymentId}").build(
				Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "paymentId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("getPayment.paymentId", "not a valid UUID")));

		verifyNoInteractions(paymentServiceMock);
	}

	@Test
	void createPaymentInvalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PAYMENT_PATH).build(Map.of("municipalityId", "abc", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(PaymentRequest.create())
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createPayment.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(paymentServiceMock);
	}
}
