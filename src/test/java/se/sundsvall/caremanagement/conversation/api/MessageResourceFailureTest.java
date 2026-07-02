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
import se.sundsvall.caremanagement.conversation.api.model.MarkMessagesRead;
import se.sundsvall.caremanagement.conversation.service.MessageReadService;
import se.sundsvall.caremanagement.conversation.service.MessageService;
import se.sundsvall.dept44.support.Identifier;

import static java.util.List.of;
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

	@MockitoBean
	private MessageReadService readServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	private void postMessageExpectingBadRequest(final String errandId, final CreateMessage message) {
		final var builder = new MultipartBodyBuilder();
		builder.part("message", message, APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", errandId)))
			.header(Identifier.HEADER_NAME, "joe001doe; type=adAccount")
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void post_missingIdentifier() {
		final var builder = new MultipartBodyBuilder();
		builder.part("message", new CreateMessage("OUTBOUND", "body", "author", null), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(serviceMock);
	}

	@Test
	void post_blankDirection() {
		postMessageExpectingBadRequest(ERRAND_ID, new CreateMessage("", "body", "author", null));
	}

	@Test
	void post_invalidDirection() {
		postMessageExpectingBadRequest(ERRAND_ID, new CreateMessage("SIDEWAYS", "body", "author", null));
	}

	@Test
	void post_blankBody() {
		postMessageExpectingBadRequest(ERRAND_ID, new CreateMessage("OUTBOUND", "", "author", null));
	}

	@Test
	void post_invalidErrandId() {
		postMessageExpectingBadRequest("not-a-uuid", new CreateMessage("OUTBOUND", "body", "author", null));
	}

	@Test
	void post_invalidInReplyToId() {
		postMessageExpectingBadRequest(ERRAND_ID, new CreateMessage("OUTBOUND", "body", "author", "not-a-uuid"));
	}

	@Test
	void unreadCount_missingIdentifier() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/unread-count").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(readServiceMock);
	}

	@Test
	void markRead_missingIdentifier() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/read").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.contentType(APPLICATION_JSON)
			.bodyValue(new MarkMessagesRead(of(randomUUID().toString())))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(readServiceMock);
	}

	@Test
	void markRead_emptyMessageIds() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/read").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "joe001doe; type=adAccount")
			.contentType(APPLICATION_JSON)
			.bodyValue(new MarkMessagesRead(of()))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(readServiceMock);
	}

	@Test
	void markRead_invalidMessageId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/read").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "joe001doe; type=adAccount")
			.contentType(APPLICATION_JSON)
			.bodyValue(new MarkMessagesRead(of("not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest();

		verifyNoInteractions(readServiceMock);
	}
}
