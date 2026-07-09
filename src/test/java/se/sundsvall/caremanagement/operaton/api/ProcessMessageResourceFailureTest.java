package se.sundsvall.caremanagement.operaton.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.operaton.api.model.ProcessMessageRequest;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static se.sundsvall.caremanagement.support.ConstraintViolationAssertions.assertConstraintViolation;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class ProcessMessageResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/process-messages";

	@MockitoBean
	private ProcessService processServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void sendProcessMessage_badMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(ProcessMessageRequest.create().withMessageName("PaymentDecisionReceived"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("sendProcessMessage.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void sendProcessMessage_badNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.bodyValue(ProcessMessageRequest.create().withMessageName("PaymentDecisionReceived"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("sendProcessMessage.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void sendProcessMessage_badErrandIdUuid() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(ProcessMessageRequest.create().withMessageName("PaymentDecisionReceived"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("sendProcessMessage.errandId", "not a valid UUID")));

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void sendProcessMessage_blankMessageName() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(ProcessMessageRequest.create().withMessageName(" "))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("messageName", "must not be blank")));

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void sendProcessMessage_missingMessageName() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(ProcessMessageRequest.create().withVariables(Map.of("paymentDecision", "APPROVED")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("messageName", "must not be blank")));

		verifyNoInteractions(processServiceMock);
	}
}
