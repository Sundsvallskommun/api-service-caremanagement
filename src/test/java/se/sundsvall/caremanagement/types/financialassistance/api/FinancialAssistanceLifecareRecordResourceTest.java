package se.sundsvall.caremanagement.types.financialassistance.api;

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
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.CreateLifecareDocumentRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.CreateLifecareJournalNoteRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDocumentType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareNoteType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecord;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecordBody;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecordContent;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecords;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.UpdateLifecareRecordRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("junit")
class FinancialAssistanceLifecareRecordResourceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final String PATH = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare";
	private static final Map<String, Object> URI_VARIABLES = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID);
	private static final Map<String, Object> RECORD_URI_VARIABLES = Map.of("municipalityId", MUNICIPALITY_ID, "namespace", NAMESPACE, "errandId", ERRAND_ID,
		"id", 138);

	@MockitoBean
	private LifecareRecordService serviceMock;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void listRecords() {
		when(serviceMock.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(LifecareRecords.create()
			.withJournalNotes(List.of(LifecareRecord.create().withId("1").withWriteProtected(true)))
			.withDocuments(List.of()));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/documents").build(URI_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			// The flag carries Lifecare's (and the BFF's) name, not the Java field name.
			.jsonPath("$.journalNotes[0].protected").isEqualTo(true)
			.jsonPath("$.journalNotes[0].writeProtected").doesNotExist()
			.returnResult();

		assertThat(response.getStatus().value()).isEqualTo(200);
		verify(serviceMock).list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void listJournalNoteBodies() {
		when(serviceMock.journalNoteBodies(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(LifecareRecordBody.create().withId("1").withContent("<p>1</p>")));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/journal-note-bodies").build(URI_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(LifecareRecordBody.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).singleElement().extracting(LifecareRecordBody::getContent).isEqualTo("<p>1</p>");
		verify(serviceMock).journalNoteBodies(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void listDocumentBodies() {
		when(serviceMock.documentBodies(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(LifecareRecordBody.create().withId("3")));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/document-bodies").build(URI_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(LifecareRecordBody.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).singleElement().extracting(LifecareRecordBody::getId).isEqualTo("3");
		verify(serviceMock).documentBodies(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void listJournalNoteTypes() {
		when(serviceMock.journalNoteTypes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(LifecareNoteType.create().withCode(1)));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/documents/journal-note-types").build(URI_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(LifecareNoteType.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).singleElement().extracting(LifecareNoteType::getCode).isEqualTo(1);
		verify(serviceMock).journalNoteTypes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void listDocumentTypes() {
		when(serviceMock.documentTypes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(LifecareDocumentType.create().withCode(1)));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/documents/document-types").build(URI_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBodyList(LifecareDocumentType.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).singleElement().extracting(LifecareDocumentType::getCode).isEqualTo(1);
		verify(serviceMock).documentTypes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void createJournalNote() {
		final var request = CreateLifecareJournalNoteRequest.create().withContent("<p>Hej</p>").withNoteTypeCode(1).withWriteProtected(true);
		when(serviceMock.createJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request)).thenReturn(LifecareRecord.create().withId("138"));

		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/documents/journal-notes").build(URI_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(Map.of("content", "<p>Hej</p>", "noteTypeCode", 1, "protected", true))
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID + "/lifecare/documents/journal-notes/138")
			.expectBody().jsonPath("$.id").isEqualTo("138");

		verify(serviceMock).createJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request);
	}

	@Test
	void createDocument() {
		final var request = CreateLifecareDocumentRequest.create().withContent("<p>Hej</p>").withDocumentTypeCode(1);
		when(serviceMock.createDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request)).thenReturn(LifecareRecord.create().withId("139"));

		webTestClient.post()
			.uri(builder -> builder.path(PATH + "/documents/documents").build(URI_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().location("/" + MUNICIPALITY_ID + "/" + NAMESPACE + "/errands/financial-assistance/" + ERRAND_ID + "/lifecare/documents/documents/139");

		verify(serviceMock).createDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request);
	}

	@Test
	void readJournalNote() {
		when(serviceMock.readJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 138)).thenReturn(LifecareRecordContent.create().withId("138"));

		final var response = webTestClient.get()
			.uri(builder -> builder.path(PATH + "/documents/journal-notes/{id}").build(RECORD_URI_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody(LifecareRecordContent.class)
			.returnResult()
			.getResponseBody();

		assertThat(response).isNotNull().extracting(LifecareRecordContent::getId).isEqualTo("138");
		verify(serviceMock).readJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 138);
	}

	@Test
	void readDocument() {
		when(serviceMock.readDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 138)).thenReturn(LifecareRecordContent.create().withId("138"));

		webTestClient.get()
			.uri(builder -> builder.path(PATH + "/documents/documents/{id}").build(RECORD_URI_VARIABLES))
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.id").isEqualTo("138");

		verify(serviceMock).readDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 138);
	}

	@Test
	void updateJournalNote() {
		final var request = UpdateLifecareRecordRequest.create().withContent("<p>new</p>").withTime("09:02");
		when(serviceMock.updateJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 138, request)).thenReturn(LifecareRecordContent.create().withEditable(true));

		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/documents/journal-notes/{id}").build(RECORD_URI_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody().jsonPath("$.editable").isEqualTo(true);

		verify(serviceMock).updateJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 138, request);
	}

	@Test
	void updateDocument() {
		final var request = UpdateLifecareRecordRequest.create().withContent("<p>new</p>").withWriteProtected(true);
		when(serviceMock.updateDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 138, request)).thenReturn(LifecareRecordContent.create().withEditable(false));

		webTestClient.put()
			.uri(builder -> builder.path(PATH + "/documents/documents/{id}").build(RECORD_URI_VARIABLES))
			.contentType(APPLICATION_JSON)
			.bodyValue(Map.of("content", "<p>new</p>", "protected", true))
			.exchange()
			.expectStatus().isOk();

		verify(serviceMock).updateDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 138, request);
	}
}
