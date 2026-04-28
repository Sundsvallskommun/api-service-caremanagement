package se.sundsvall.caremanagement.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.api.model.ProcessMessageRequest;
import se.sundsvall.caremanagement.service.ProcessService;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class ProcessMessageResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/process-messages";

	@MockitoBean
	private ProcessService processServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void sendProcessMessageWithVariables() {
		final var request = ProcessMessageRequest.create()
			.withMessageName("PaymentDecisionReceived")
			.withVariables(Map.of("paymentDecision", "APPROVED"));

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isNoContent();

		verify(processServiceMock).correlateMessage(MUNICIPALITY_ID, "PaymentDecisionReceived", ERRAND_ID, Map.of("paymentDecision", "APPROVED"));
	}

	@Test
	void sendProcessMessageWithoutVariables() {
		final var request = ProcessMessageRequest.create()
			.withMessageName("DocumentReady");

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isNoContent();

		verify(processServiceMock).correlateMessage(MUNICIPALITY_ID, "DocumentReady", ERRAND_ID, null);
	}
}
