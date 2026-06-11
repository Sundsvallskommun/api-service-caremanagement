package se.sundsvall.caremanagement.conversation.api;

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
import se.sundsvall.caremanagement.conversation.api.model.CreateMessage;
import se.sundsvall.caremanagement.conversation.api.model.Message;
import se.sundsvall.caremanagement.conversation.service.MessageService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class MessageResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String MESSAGE_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/messages";

	@MockitoBean
	private MessageService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void post() {
		when(serviceMock.post(ERRAND_ID, new CreateMessage("OUTBOUND", "body", "author"))).thenReturn(MESSAGE_ID);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(new CreateMessage("OUTBOUND", "body", "author"))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).post(ERRAND_ID, new CreateMessage("OUTBOUND", "body", "author"));
	}

	@Test
	void list() {
		when(serviceMock.listForErrand(ERRAND_ID)).thenReturn(List.of(Message.create().withId("m1")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Message.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).listForErrand(ERRAND_ID);
	}

	@Test
	void read() {
		when(serviceMock.read(MESSAGE_ID)).thenReturn(Message.create().withId(MESSAGE_ID).withBody("b"));

		final var message = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{messageId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "messageId", MESSAGE_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Message.class)
			.returnResult()
			.getResponseBody();

		assertThat(message).isNotNull();
		assertThat(message.getId()).isEqualTo(MESSAGE_ID);
		verify(serviceMock).read(MESSAGE_ID);
	}
}
