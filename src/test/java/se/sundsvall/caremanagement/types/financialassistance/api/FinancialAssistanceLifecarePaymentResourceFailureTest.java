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
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.ErrandLifecarePaymentService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentRegistrationService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecarePaymentResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare";

	@MockitoBean
	private ErrandLifecarePaymentService paymentServiceMock;
	@MockitoBean
	private LifecarePaymentRegistrationService registrationServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}

	@Test
	void readPaymentOptionsInvalidPathVariables() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/payment-options").build(Map.of("municipalityId", "invalid", "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("readPaymentOptions.municipalityId", "not a valid municipality ID"),
				tuple("readPaymentOptions.errandId", "not a valid UUID")));

		verifyNoInteractions(paymentServiceMock, registrationServiceMock);
	}

	@Test
	void readPaymentStatusInvalidErrandId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/payment-status").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(), tuple("readPaymentStatus.errandId", "not a valid UUID")));

		verifyNoInteractions(paymentServiceMock, registrationServiceMock);
	}

	@Test
	void registerPaymentInvalidErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/payments").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.contentType(APPLICATION_JSON)
			.bodyValue(Map.of("amount", 1))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(), tuple("registerPayment.errandId", "not a valid UUID")));

		verifyNoInteractions(paymentServiceMock, registrationServiceMock);
	}

	@Test
	void createPayeeInvalidBody() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/payees").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", randomUUID().toString())))
			.contentType(APPLICATION_JSON)
			.bodyValue(new LifecarePayeeRequest(" ", null, null, "12345678901234567", null))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("name", "must not be blank"),
				tuple("paymentMethod", "must not be null"),
				tuple("clearing", "size must be between 0 and 16")));

		verifyNoInteractions(paymentServiceMock, registrationServiceMock);
	}
}
