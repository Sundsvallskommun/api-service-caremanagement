package se.sundsvall.caremanagement.conversation.api;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
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
import se.sundsvall.caremanagement.conversation.api.model.Message;
import se.sundsvall.caremanagement.conversation.api.model.UnreadCount;
import se.sundsvall.caremanagement.conversation.service.MessageReadService;
import se.sundsvall.caremanagement.conversation.service.MessageService;
import se.sundsvall.dept44.support.Identifier;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class MessageResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String MESSAGE_ID = randomUUID().toString();
	private static final String ATTACHMENT_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/messages";

	@MockitoBean
	private MessageService serviceMock;

	@MockitoBean
	private MessageReadService readServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void post() {
		when(serviceMock.post(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(CreateMessage.class), any())).thenReturn(MESSAGE_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("message", new CreateMessage("OUTBOUND", "body", "author", null), APPLICATION_JSON);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "joe001doe; type=adAccount")
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/messages/" + MESSAGE_ID);

		verify(serviceMock).post(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(CreateMessage.class), any());
	}

	@Test
	void postWithAttachments() {
		when(serviceMock.post(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(CreateMessage.class), any())).thenReturn(MESSAGE_ID);

		final var builder = new MultipartBodyBuilder();
		builder.part("message", new CreateMessage("OUTBOUND", "Please see attached", "author", null), APPLICATION_JSON);
		builder.part("attachments", "certificate".getBytes()).filename("certificate.pdf");
		builder.part("attachments", "photo".getBytes()).filename("photo.png");

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "joe001doe; type=adAccount")
			.contentType(MULTIPART_FORM_DATA)
			.bodyValue(builder.build())
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().contentType(ALL)
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/messages/" + MESSAGE_ID);

		verify(serviceMock).post(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any(CreateMessage.class), any());
	}

	@Test
	void list() {
		when(serviceMock.listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Message.create().withId("m1")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Message.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void unreadCountForCaseworker() {
		when(readServiceMock.unreadCount(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Identifier.parse("joe001doe; type=adAccount"))).thenReturn(5L);

		final var body = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/unread-count").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "joe001doe; type=adAccount")
			.exchange()
			.expectStatus().isOk()
			.expectBody(UnreadCount.class)
			.returnResult()
			.getResponseBody();

		assertThat(body).isNotNull();
		assertThat(body.unreadCount()).isEqualTo(5L);
		verify(readServiceMock).unreadCount(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Identifier.parse("joe001doe; type=adAccount"));
	}

	@Test
	void unreadCountForClient() {
		when(readServiceMock.unreadCount(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Identifier.parse("f47ac10b-58cc-4372-a567-0e02b2c3d479; type=partyId"))).thenReturn(2L);

		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/unread-count").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "f47ac10b-58cc-4372-a567-0e02b2c3d479; type=partyId")
			.exchange()
			.expectStatus().isOk();

		verify(readServiceMock).unreadCount(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Identifier.parse("f47ac10b-58cc-4372-a567-0e02b2c3d479; type=partyId"));
	}

	@Test
	void markRead() {
		final var messageIds = List.of(randomUUID().toString(), randomUUID().toString());

		webTestClient.post()
			.uri(uri -> uri.path(PATH + "/read").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.header(Identifier.HEADER_NAME, "joe001doe; type=adAccount")
			.contentType(APPLICATION_JSON)
			.bodyValue(new MarkMessagesRead(messageIds))
			.exchange()
			.expectStatus().isNoContent();

		verify(readServiceMock).markRead(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, Identifier.parse("joe001doe; type=adAccount"), messageIds);
	}

	@Test
	void read() {
		when(serviceMock.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, MESSAGE_ID)).thenReturn(Message.create().withId(MESSAGE_ID).withBody("b"));

		final var message = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{messageId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "messageId", MESSAGE_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Message.class)
			.returnResult()
			.getResponseBody();

		assertThat(message).isNotNull();
		assertThat(message.getId()).isEqualTo(MESSAGE_ID);
		verify(serviceMock).read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, MESSAGE_ID);
	}

	@Test
	void streamAttachmentFile() {
		doAnswer(invocation -> {
			final HttpServletResponse response = invocation.getArgument(5);
			response.getOutputStream().write("file-bytes".getBytes());
			return null;
		}).when(serviceMock).streamAttachmentFile(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(MESSAGE_ID), eq(ATTACHMENT_ID), any());

		final var body = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{messageId}/attachments/{attachmentId}/file")
				.build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "messageId", MESSAGE_ID, "attachmentId", ATTACHMENT_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(byte[].class)
			.returnResult()
			.getResponseBody();

		assertThat(new String(body)).isEqualTo("file-bytes");
		verify(serviceMock).streamAttachmentFile(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(MESSAGE_ID), eq(ATTACHMENT_ID), any());
	}
}
