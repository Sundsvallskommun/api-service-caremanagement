package se.sundsvall.caremanagement.notes.api;

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
import se.sundsvall.caremanagement.notes.api.model.CreateNote;
import se.sundsvall.caremanagement.notes.api.model.Note;
import se.sundsvall.caremanagement.notes.service.NoteService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class NoteResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String NOTE_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/notes";

	@MockitoBean
	private NoteService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void add() {
		when(serviceMock.add(ERRAND_ID, new CreateNote("body", "author"))).thenReturn(NOTE_ID);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(new CreateNote("body", "author"))
			.exchange()
			.expectStatus().isCreated();

		verify(serviceMock).add(ERRAND_ID, new CreateNote("body", "author"));
	}

	@Test
	void list() {
		when(serviceMock.listForErrand(ERRAND_ID)).thenReturn(List.of(Note.create().withId("n1")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Note.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).listForErrand(ERRAND_ID);
	}

	@Test
	void read() {
		when(serviceMock.read(NOTE_ID)).thenReturn(Note.create().withId(NOTE_ID).withBody("b"));

		final var note = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{noteId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "noteId", NOTE_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Note.class)
			.returnResult()
			.getResponseBody();

		assertThat(note).isNotNull();
		assertThat(note.getId()).isEqualTo(NOTE_ID);
		verify(serviceMock).read(NOTE_ID);
	}

	@Test
	void delete() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{noteId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "noteId", NOTE_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(NOTE_ID);
	}
}
