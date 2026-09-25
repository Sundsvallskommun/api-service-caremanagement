package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.DocumentView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.CreateLifecareDocumentRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.CreateLifecareJournalNoteRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDocumentType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareNoteType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecord;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecordBody;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.UpdateLifecareRecordRequest;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService.PATH_CREATE_DOCUMENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService.PATH_CREATE_NOTE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService.PATH_DOCUMENT_PROPOSAL;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService.PATH_LIST;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService.PATH_NOTE_PROPOSAL;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService.PATH_READ_DOCUMENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService.PATH_READ_NOTE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService.PATH_UPDATE_DOCUMENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareRecordService.PATH_UPDATE_NOTE;

@ExtendWith(MockitoExtension.class)
class LifecareRecordServiceTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final String PERSONAL_NUMBER = "198802090000";
	private static final LifecareErrand ERRAND = new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 2, null, null, null, 2026, 9);
	private static final Map<String, String> BY_CLIENT = Map.of("id", PERSONAL_NUMBER);
	private static final Map<String, String> BY_SERVICE = Map.of("id", "2");

	private static final String LIST = """
		{ "documentModels": [
		  { "id": 1, "title": "Anteckning", "date": "2026-09-23", "time": "10:00", "type": "Journalanteckning", "updateSignature": "RPA_031DEV",
		    "updateDate": "2026-09-23", "protected": false, "locked": false, "documentType_Name": "JournalNote", "typeCode": 3 },
		  { "id": 2, "title": "Anteckning", "date": "2026-09-23", "documentType_Name": "JournalNote", "typeCode": 3 },
		  { "id": 3, "title": "Brev", "date": "2026-09-23", "documentType_Name": "Regular", "typeCode": 13 },
		  { "id": 4, "title": "Fil", "date": "2026-09-23", "documentType_Name": "Pdf", "typeCode": 1 },
		  { "id": 5, "title": "Blankett", "date": "2026-09-23", "documentType_Name": "Form", "typeCode": 1 } ] }
		""";

	private static final String RECORD = """
		{ "documentId": 1, "title": "Anteckning", "content": "<p>old</p>", "occurenceDate": "2026-09-23", "time": "10:00", "protected": false,
		  "ownerId": 2, "ownerType": 53 }
		""";

	private static final String CREATED_ROW = """
		{ "id": 138, "title": "Journalanteckning", "date": "2026-09-23", "time": "12:11", "type": "Journalanteckning", "ownerTypeText": "",
		  "updateSignature": "RPA_031DEV", "updateDate": "2026-09-23", "protected": false, "locked": false, "documentType_Name": "JournalNote" }
		""";

	@Mock
	private ProfessionalWebClient client;
	@Mock
	private LifecareErrandService errandService;
	@Mock
	private LifecareAccessRecorder recorder;
	@Mock
	private LifecareCaseHistoryService caseHistoryService;

	@InjectMocks
	private LifecareRecordService service;

	@Captor
	private ArgumentCaptor<ObjectNode> bodyCaptor;

	private static JsonNode json(final String text) {
		return JSON.readTree(text);
	}

	private static Map<String, String> recordParams(final String id, final String hideRevisions, final String inEdit) {
		return Map.of("id", id, "hideRevisions", hideRevisions, "inEdit", inEdit);
	}

	private void givenErrand() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(ERRAND);
	}

	private void givenClientList() {
		givenErrand();
		when(errandService.applicantPersonalNumber(ERRAND)).thenReturn(PERSONAL_NUMBER);
		when(client.get(PATH_LIST, BY_CLIENT)).thenReturn(json(LIST));
	}

	@Test
	void listSplitsTheRecordAndLogsTheRead() {
		givenClientList();

		final var records = service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(records.getJournalNotes()).extracting(LifecareRecord::getId).containsExactly("1", "2");
		assertThat(records.getDocuments()).extracting(LifecareRecord::getId).containsExactly("3", "4", "5");
		verify(recorder).read(ERRAND, "JOURNAL_AND_DOCUMENTS", "Läste journalanteckningar och dokument i Lifecare");
	}

	@Test
	void listIsNotServedWhenTheReadCannotBeLogged() {
		givenClientList();
		doThrow(new IllegalStateException("log down")).when(recorder).read(any(), anyString(), anyString());

		assertThatThrownBy(() -> service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void journalNoteBodiesLeaveOutWhatLifecareRefuses() {
		givenClientList();
		when(client.get(PATH_READ_NOTE, recordParams("1", "true", "false"))).thenReturn(json("{\"content\": \"<p>1</p>\"}"));
		when(client.get(PATH_READ_NOTE, recordParams("2", "true", "false"))).thenThrow(Problem.valueOf(BAD_GATEWAY, "down"));

		final var bodies = service.journalNoteBodies(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(bodies).containsExactly(LifecareRecordBody.create().withId("1").withContent("<p>1</p>"), LifecareRecordBody.create().withId("2"));
		// One access-log row for the whole tab, not one per record.
		verify(recorder, times(1)).read(ERRAND, "JOURNAL_AND_DOCUMENTS", "Läste journalanteckningarnas innehåll i Lifecare");
	}

	@Test
	void documentBodiesSkipBlanketterAndPdfs() {
		givenClientList();
		when(client.get(PATH_READ_DOCUMENT, recordParams("3", "true", "false"))).thenReturn(json("{\"content\": null}"));

		final var bodies = service.documentBodies(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(bodies).containsExactly(LifecareRecordBody.create().withId("3").withContent(""));
		verify(recorder).read(ERRAND, "JOURNAL_AND_DOCUMENTS", "Läste dokumentens innehåll i Lifecare");
	}

	@Test
	void readJournalNote() {
		givenClientList();
		when(client.get(PATH_READ_NOTE, recordParams("1", "false", "true"))).thenReturn(json(RECORD));

		final var content = service.readJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1);

		assertThat(content.getContent()).isEqualTo("<p>old</p>");
		assertThat(content.getEditable()).isTrue();
		verify(recorder).read(ERRAND, "JOURNAL_NOTE", "Läste en journalanteckning i Lifecare", "1");
	}

	@Test
	void readDocument() {
		givenClientList();
		when(client.get(PATH_READ_DOCUMENT, recordParams("3", "false", "true"))).thenReturn(json(RECORD));

		final var content = service.readDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 3);

		assertThat(content.getCategory()).isEqualTo("DOCUMENT");
		verify(recorder).read(ERRAND, "DOCUMENT", "Läste ett dokument i Lifecare", "3");
	}

	@Test
	void readRefusesARecordNotInTheApplicantsList() {
		givenClientList();

		assertRefused(() -> service.readJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 999), NOT_FOUND, LifecareRecordService.NOT_THE_CLIENTS);
		verify(client, never()).get(eq(PATH_READ_NOTE), anyMap());
		verifyNoInteractions(recorder);
	}

	@Test
	void readOnAnAnswerThatIsNotAnObject() {
		givenClientList();
		when(client.get(PATH_READ_NOTE, recordParams("1", "false", "true"))).thenReturn(json("[]"));

		assertRefused(() -> service.readJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1), BAD_GATEWAY,
			LifecareRecordService.UNEXPECTED_ANSWER.formatted(PATH_READ_NOTE));
	}

	@Test
	void updateJournalNoteSavesOntoLifecaresOwnRecord() {
		givenClientList();
		when(client.get(PATH_READ_NOTE, recordParams("1", "false", "true"))).thenReturn(json(RECORD));

		final var content = service.updateJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1,
			UpdateLifecareRecordRequest.create().withContent("<p>new</p>").withTime("09:02"));

		verify(client).post(eq(PATH_UPDATE_NOTE), eq(Map.of()), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().path("content").stringValue()).isEqualTo("<p>new</p>");
		assertThat(bodyCaptor.getValue().path("ownerType").intValue()).isEqualTo(53);
		assertThat(content.getContent()).isEqualTo("<p>new</p>");
		assertThat(content.getTime()).isEqualTo("09:02");
		verify(recorder).written(ERRAND, LifecareAccessEntry.UPDATE, "JOURNAL_NOTE", "Ändrade en journalanteckning i Lifecare", "1");
	}

	@Test
	void updateDocument() {
		givenClientList();
		when(client.get(PATH_READ_DOCUMENT, recordParams("3", "false", "true"))).thenReturn(json(RECORD));

		service.updateDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 3, UpdateLifecareRecordRequest.create().withContent("<p>new</p>"));

		verify(client).post(eq(PATH_UPDATE_DOCUMENT), eq(Map.of()), any());
		verify(recorder).written(ERRAND, LifecareAccessEntry.UPDATE, "DOCUMENT", "Ändrade ett dokument i Lifecare", "3");
	}

	@Test
	void updateRefusesAFinalisedRecord() {
		givenClientList();
		when(client.get(PATH_READ_NOTE, recordParams("1", "false", "true"))).thenReturn(json("{\"documentId\": 1, \"protected\": true}"));

		assertRefused(() -> service.updateJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1, UpdateLifecareRecordRequest.create().withContent("x")),
			CONFLICT, LifecareRecordService.FINALISED);
		verify(client, never()).post(anyString(), anyMap(), any());
	}

	@Test
	void updateRefusesARecordNotInTheApplicantsList() {
		givenClientList();

		assertRefused(() -> service.updateDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 999, UpdateLifecareRecordRequest.create().withContent("x")),
			NOT_FOUND, LifecareRecordService.NOT_THE_CLIENTS);
		verify(client, never()).post(anyString(), anyMap(), any());
	}

	@Test
	void journalNoteTypes() {
		givenErrand();
		when(client.get(PATH_NOTE_PROPOSAL, BY_SERVICE)).thenReturn(json("""
			{ "documentNoteTypes": [ { "id": 1, "name": "Journalanteckning", "sortOrder": 0, "isActive": true } ] }
			"""));

		assertThat(service.journalNoteTypes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).extracting(LifecareNoteType::getCode).containsExactly(1);
	}

	@Test
	void documentTypes() {
		givenErrand();
		when(client.get(PATH_DOCUMENT_PROPOSAL, BY_SERVICE)).thenReturn(json("""
			{ "documentTypes": [ { "documentCode": 1, "name": "EK Brev", "sortOrder": 0, "isActive": true, "isForm": false } ] }
			"""));

		assertThat(service.documentTypes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).extracting(LifecareDocumentType::getCode).containsExactly(1);
	}

	@Test
	void createJournalNoteWritesOnTheInsatsAndLogsIt() {
		givenErrand();
		when(client.get(PATH_NOTE_PROPOSAL, BY_SERVICE)).thenReturn(json("""
			{ "documentNoteTypes": [ { "id": 1, "name": "Journalanteckning", "sortOrder": 0, "isActive": true } ],
			  "documentJournalNote": { "content": null, "title": "", "ownerId": 2, "ownerType": 53 } }
			"""));
		when(client.post(eq(PATH_CREATE_NOTE), eq(Map.of()), any())).thenReturn(json(CREATED_ROW));

		final var created = service.createJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			CreateLifecareJournalNoteRequest.create().withContent("<p>Hej</p>").withNoteTypeCode(1));

		assertThat(created.getId()).isEqualTo("138");
		assertThat(created.getCategory()).isEqualTo("JOURNAL_NOTE");
		verify(client).post(eq(PATH_CREATE_NOTE), eq(Map.of()), bodyCaptor.capture());
		assertThat(bodyCaptor.getValue().path("ownerId").intValue()).isEqualTo(2);
		assertThat(bodyCaptor.getValue().path("noteTypeCode").intValue()).isEqualTo(1);
		verify(recorder).written(ERRAND, LifecareAccessEntry.CREATE, "JOURNAL_NOTE", "Skrev en journalanteckning i Lifecare", "138");
	}

	@Test
	void createJournalNoteRefusesAnInactiveNoteType() {
		givenErrand();
		when(client.get(PATH_NOTE_PROPOSAL, BY_SERVICE)).thenReturn(json("""
			{ "documentNoteTypes": [ { "id": 1, "name": "Journalanteckning", "sortOrder": 0, "isActive": false } ], "documentJournalNote": {} }
			"""));

		assertRefused(() -> service.createJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			CreateLifecareJournalNoteRequest.create().withContent("x").withNoteTypeCode(1)), BAD_REQUEST, LifecareRecordService.UNKNOWN_NOTE_TYPE);
		verify(client, never()).post(anyString(), anyMap(), any());
	}

	@Test
	void createJournalNoteWithoutAnInsats() {
		when(errandService.load(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new LifecareErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, null, null, null, null, null, null));

		assertThatThrownBy(() -> service.createJournalNote(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			CreateLifecareJournalNoteRequest.create().withContent("x").withNoteTypeCode(1)))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(CONFLICT));
		verifyNoInteractions(client);
	}

	@Test
	void createDocumentWritesOnTheInsatsAndLogsIt() {
		givenErrand();
		when(client.get(PATH_DOCUMENT_PROPOSAL, BY_SERVICE)).thenReturn(json("""
			{ "documentTypes": [ { "documentCode": 1, "name": "EK Brev", "sortOrder": 0, "isActive": true, "isForm": false } ],
			  "document": { "content": null, "title": "", "ownerId": 2, "ownerType": 23 } }
			"""));
		when(client.post(eq(PATH_CREATE_DOCUMENT), eq(Map.of()), any())).thenReturn(json(CREATED_ROW.replace("138", "139")));

		final var created = service.createDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			CreateLifecareDocumentRequest.create().withContent("<p>Hej</p>").withDocumentTypeCode(1));

		assertThat(created.getId()).isEqualTo("139");
		assertThat(created.getCategory()).isEqualTo("DOCUMENT");
		verify(recorder).written(ERRAND, LifecareAccessEntry.CREATE, "DOCUMENT", "Skrev ett dokument i Lifecare", "139");
	}

	@Test
	void createDocumentRefusesABlankett() {
		givenErrand();
		when(client.get(PATH_DOCUMENT_PROPOSAL, BY_SERVICE)).thenReturn(json("""
			{ "documentTypes": [ { "documentCode": 15, "name": "Blankett", "sortOrder": 0, "isActive": true, "isForm": true } ], "document": {} }
			"""));

		assertRefused(() -> service.createDocument(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			CreateLifecareDocumentRequest.create().withContent("x").withDocumentTypeCode(15)), BAD_REQUEST, LifecareRecordService.UNKNOWN_DOCUMENT_TYPE);
	}

	@Test
	void readDocumentPdf() {
		final var pdf = "%PDF-1.7".getBytes();
		givenClientList();
		givenFcDocuments(fcDocument("fc-1", "Fil", "2026-09-23T00:00:00"), fcDocument("fc-2", "Annat", "2026-09-23T00:00:00"));
		when(caseHistoryService.documentContent(MUNICIPALITY_ID, "fc-1")).thenReturn(pdf);

		assertThat(service.readDocumentPdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 4)).isEqualTo(pdf);
		verify(recorder).read(ERRAND, "DOCUMENT", "Hämtade ett dokument som PDF ur Lifecare", "4");
	}

	@Test
	void readDocumentPdfRefusesARecordNotUnderDokument() {
		givenClientList();

		assertRefused(() -> service.readDocumentPdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 1), NOT_FOUND, LifecareRecordService.NOT_THE_CLIENTS);
		verifyNoInteractions(caseHistoryService, recorder);
	}

	@Test
	void readDocumentPdfWhenLifecareHasNoMatchingDocument() {
		givenClientList();
		givenFcDocuments(fcDocument("fc-1", "Fil", "2026-09-22T00:00:00"));

		assertRefused(() -> service.readDocumentPdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 4), NOT_FOUND, LifecareRecordService.NO_PDF);
		verify(caseHistoryService, never()).documentContent(anyString(), anyString());
		verifyNoInteractions(recorder);
	}

	@Test
	void readDocumentPdfRefusesToGuessBetweenTwins() {
		givenClientList();
		givenFcDocuments(fcDocument("fc-1", "Fil", "2026-09-23T00:00:00"), fcDocument("fc-2", "Fil", "2026-09-23T08:00:00"));

		assertRefused(() -> service.readDocumentPdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 4), CONFLICT, LifecareRecordService.AMBIGUOUS_PDF);
		verify(caseHistoryService, never()).documentContent(anyString(), anyString());
		verifyNoInteractions(recorder);
	}

	@Test
	void readDocumentPdfOnARowWithoutDate() {
		givenErrand();
		when(errandService.applicantPersonalNumber(ERRAND)).thenReturn(PERSONAL_NUMBER);
		when(client.get(PATH_LIST, BY_CLIENT)).thenReturn(json("""
			{ "documentModels": [ { "id": 7, "title": "Fil", "documentType_Name": "Pdf", "typeCode": 1 } ] }
			"""));

		assertRefused(() -> service.readDocumentPdf(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, 7), NOT_FOUND, LifecareRecordService.NO_PDF);
		verifyNoInteractions(caseHistoryService, recorder);
	}

	private void givenFcDocuments(final DocumentView... documents) {
		final var day = LocalDate.of(2026, 9, 23);
		when(errandService.applicantPartyId(ERRAND)).thenReturn("party-1");
		when(caseHistoryService.listDocuments(MUNICIPALITY_ID, "party-1", day, day)).thenReturn(List.of(documents));
	}

	private static DocumentView fcDocument(final String id, final String title, final String date) {
		return new DocumentView(id, title, date, "Inkommande handling", "owner", "person");
	}

	private static void assertRefused(final ThrowingCallable call, final HttpStatus status, final String detail) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
			assertThat(problem.getStatus()).isEqualTo(status);
			assertThat(problem.getDetail()).isEqualTo(detail);
		});
	}
}
