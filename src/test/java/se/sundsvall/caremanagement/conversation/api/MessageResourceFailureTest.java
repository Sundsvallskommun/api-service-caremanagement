package se.sundsvall.caremanagement.conversation.api;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.conversation.api.model.CreateMessage;
import se.sundsvall.caremanagement.conversation.service.MessageService;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class MessageResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/messages";

	@MockitoBean
	private MessageService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	private void postMessageExpectingBadRequest(final String errandId, final CreateMessage message) {
		final var builder = new MultipartBodyBuilder();
		builder.part("message", message, APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void post_blankDirection() {
		postMessageExpectingBadRequest(ERRAND_ID, new CreateMessage("", "body", "author"));
	}

	@Test
	void post_invalidDirection() {
		postMessageExpectingBadRequest(ERRAND_ID, new CreateMessage("SIDEWAYS", "body", "author"));
	}

	@Test
	void post_blankBody() {
		postMessageExpectingBadRequest(ERRAND_ID, new CreateMessage("OUTBOUND", "", "author"));
	}

	@Test
	void post_invalidErrandId() {
		postMessageExpectingBadRequest("not-a-uuid", new CreateMessage("OUTBOUND", "body", "author"));
	}
}
