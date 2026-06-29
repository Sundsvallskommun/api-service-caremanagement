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

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

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
			.expectStatus().isBadRequest();

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void sendProcessMessage_badNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.bodyValue(ProcessMessageRequest.create().withMessageName("PaymentDecisionReceived"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void sendProcessMessage_badErrandIdUuid() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(ProcessMessageRequest.create().withMessageName("PaymentDecisionReceived"))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void sendProcessMessage_blankMessageName() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(ProcessMessageRequest.create().withMessageName(" "))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(processServiceMock);
	}

	@Test
	void sendProcessMessage_missingMessageName() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(ProcessMessageRequest.create().withVariables(Map.of("paymentDecision", "APPROVED")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(processServiceMock);
	}
}
