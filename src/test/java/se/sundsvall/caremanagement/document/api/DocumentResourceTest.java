package se.sundsvall.caremanagement.document.api;

import java.time.LocalDate;
import java.time.LocalTime;
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
import se.sundsvall.caremanagement.document.api.model.CreateDocument;
import se.sundsvall.caremanagement.document.api.model.Document;
import se.sundsvall.caremanagement.document.api.model.LockDocument;
import se.sundsvall.caremanagement.document.api.model.UpdateDocument;
import se.sundsvall.caremanagement.document.service.DocumentService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class DocumentResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = randomUUID().toString();
	private static final String DOCUMENT_ID = randomUUID().toString();
	private static final String PATH = "/{municipalityId}/{namespace}/errands/{errandId}/documents";
	private static final LocalDate DOCUMENT_DATE = LocalDate.parse("2025-05-30");
	private static final LocalTime DOCUMENT_TIME = LocalTime.of(14, 30);

	@MockitoBean
	private DocumentService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void add() {
		final var request = new CreateDocument("Brev", "Rubrik", "body", DOCUMENT_DATE, DOCUMENT_TIME, "carola");
		when(serviceMock.add(ERRAND_ID, request)).thenReturn(DOCUMENT_ID);

		webTestClient.post()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/" + ERRAND_ID + "/documents/" + DOCUMENT_ID);

		verify(serviceMock).add(ERRAND_ID, request);
	}

	@Test
	void list() {
		when(serviceMock.listForErrand(ERRAND_ID)).thenReturn(List.of(Document.create().withId("d1")));

		final var response = webTestClient.get()
			.uri(uri -> uri.path(PATH).build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(Document.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).hasSize(1);
		verify(serviceMock).listForErrand(ERRAND_ID);
	}

	@Test
	void read() {
		when(serviceMock.read(DOCUMENT_ID)).thenReturn(Document.create().withId(DOCUMENT_ID).withHeading("H"));

		final var document = webTestClient.get()
			.uri(uri -> uri.path(PATH + "/{documentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "documentId", DOCUMENT_ID)))
			.exchange()
			.expectStatus().isOk()
			.expectBody(Document.class)
			.returnResult()
			.getResponseBody();

		assertThat(document).isNotNull();
		assertThat(document.getId()).isEqualTo(DOCUMENT_ID);
		verify(serviceMock).read(DOCUMENT_ID);
	}

	@Test
	void update() {
		final var request = new UpdateDocument("Brev", "Ny rubrik", "updated", DOCUMENT_DATE, DOCUMENT_TIME, "editor");
		when(serviceMock.update(DOCUMENT_ID, request)).thenReturn(Document.create().withId(DOCUMENT_ID).withHeading("Ny rubrik").withModifiedBy("editor"));

		final var document = webTestClient.patch()
			.uri(uri -> uri.path(PATH + "/{documentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "documentId", DOCUMENT_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(Document.class)
			.returnResult()
			.getResponseBody();

		assertThat(document).isNotNull();
		assertThat(document.getHeading()).isEqualTo("Ny rubrik");
		assertThat(document.getModifiedBy()).isEqualTo("editor");
		verify(serviceMock).update(DOCUMENT_ID, request);
	}

	@Test
	void lock() {
		final var request = new LockDocument("carola");
		when(serviceMock.lock(DOCUMENT_ID, request)).thenReturn(Document.create().withId(DOCUMENT_ID).withStatus("LOCKED").withLockedBy("carola"));

		final var document = webTestClient.post()
			.uri(uri -> uri.path(PATH + "/{documentId}/lock").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "documentId", DOCUMENT_ID)))
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(Document.class)
			.returnResult()
			.getResponseBody();

		assertThat(document).isNotNull();
		assertThat(document.getStatus()).isEqualTo("LOCKED");
		assertThat(document.getLockedBy()).isEqualTo("carola");
		verify(serviceMock).lock(DOCUMENT_ID, request);
	}

	@Test
	void delete() {
		webTestClient.delete()
			.uri(uri -> uri.path(PATH + "/{documentId}").build(Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID, "documentId", DOCUMENT_ID)))
			.exchange()
			.expectStatus().isNoContent();

		verify(serviceMock).delete(DOCUMENT_ID);
	}
}
