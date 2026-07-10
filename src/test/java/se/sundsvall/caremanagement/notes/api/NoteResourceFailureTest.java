package se.sundsvall.caremanagement.notes.api;

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
import se.sundsvall.caremanagement.notes.api.model.CreateNote;
import se.sundsvall.caremanagement.notes.api.model.UpdateNote;
import se.sundsvall.caremanagement.notes.service.NoteService;
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
class NoteResourceFailureTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String NOTE_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/notes";

	@MockitoBean
	private NoteService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	// --- POST (add) ---

	@Test
	void addWithInvalidMunicipalityId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(new CreateNote("body", "author"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNote.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void addWithInvalidNamespace() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", "bad namespace", "errandId", ERRAND_ID)))
			.bodyValue(new CreateNote("body", "author"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNote.namespace", "can only contain A-Z, a-z, 0-9, - and _")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void addWithInvalidErrandId() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.bodyValue(new CreateNote("body", "author"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("createNote.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void addWithBlankBody() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(new CreateNote(" ", "author"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("body", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void addWithMissingBody() {
		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(new CreateNote(null, "author"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("body", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	// --- GET list ---

	@Test
	void listWithInvalidMunicipalityId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", "bad-municipality-id", "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("list.municipalityId", "not a valid municipality ID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void listWithInvalidErrandId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("list.errandId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	// --- GET read (by id) ---

	@Test
	void readWithInvalidNoteId() {
		webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{noteId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "noteId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("read.noteId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	// --- PATCH (update) ---

	@Test
	void updateWithInvalidNoteId() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/{noteId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "noteId", "not-a-uuid")))
			.bodyValue(new UpdateNote("updated body", "editor"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("update.noteId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithBlankBody() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/{noteId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "noteId", NOTE_ID)))
			.bodyValue(new UpdateNote(" ", "editor"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("body", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	@Test
	void updateWithMissingBody() {
		webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/{noteId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "noteId", NOTE_ID)))
			.bodyValue(new UpdateNote(null, "editor"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("body", "must not be blank")));

		verifyNoInteractions(serviceMock);
	}

	// --- DELETE ---

	@Test
	void deleteWithInvalidNoteId() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{noteId}").build(Map.of(
				"municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "noteId", "not-a-uuid")))
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.consumeWith(result -> assertConstraintViolation(result.getResponseBody(),
				tuple("delete.noteId", "not a valid UUID")));

		verifyNoInteractions(serviceMock);
	}

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
}
