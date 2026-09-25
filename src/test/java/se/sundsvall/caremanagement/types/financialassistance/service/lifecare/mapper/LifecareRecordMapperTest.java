package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.CreateLifecareDocumentRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.CreateLifecareJournalNoteRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDocumentType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareNoteType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecord;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.UpdateLifecareRecordRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.DOCUMENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.JOURNAL_NOTE;

class LifecareRecordMapperTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	static final String LIST = """
		{ "documentModels": [
		  { "id": 1, "title": "Journalanteckning", "date": "2026-09-22", "time": "17:01", "type": "Beslut", "ownerTypeText": "EK Ekonomiskt bistånd",
		    "responsibleCaseworker": "RPA_031DEV", "updateSignature": "RPA_031DEV", "updateDate": "2026-09-22", "protected": true, "locked": false,
		    "documentType_Name": "JournalNote", "typeCode": 3 },
		  { "id": 2, "title": "Inkommen", "date": "2026-09-21", "time": "", "type": "Inkommen handling", "ownerTypeText": "",
		    "responsibleCaseworker": null, "updateSignature": "", "updateDate": "2026-09-21", "protected": false, "locked": true,
		    "documentType_Name": "Pdf", "typeCode": 1 },
		  { "id": 3, "title": "Brev", "date": "2026-09-20", "time": "08:00", "type": "Avgifter Brev", "ownerTypeText": "",
		    "updateSignature": "X", "updateDate": "2026-09-20", "protected": false, "locked": false, "documentType_Name": "Regular", "typeCode": 13 },
		  { "id": 4, "title": "Blankett", "date": "2026-09-20", "documentType_Name": "Form", "typeCode": 15 },
		  { "id": 5, "title": "Blankett", "date": "2026-09-20", "documentType_Name": "Form", "typeCode": 1 },
		  { "id": 6, "title": "Okänd", "date": "2026-09-20", "documentType_Name": "JournalNote" }
		] }
		""";

	static final String NOTE_PROPOSAL = """
		{ "documentNoteTypes": [
		    { "id": 3, "name": "Beslut", "sortOrder": 0, "isActive": true, "writeProtectAuto": true },
		    { "id": 1, "name": "Journalanteckning", "sortOrder": 240, "isActive": true },
		    { "id": 9, "name": "Utgången typ", "sortOrder": 100, "isActive": false } ],
		  "documentJournalNote": { "content": null, "title": "", "documentId": 0, "ownerId": 1, "ownerType": 53, "noteTypeCode": 0,
		    "occurenceDate": "2026-09-23", "lifecareCreated": true } }
		""";

	static final String DOCUMENT_PROPOSAL = """
		{ "documentTypes": [
		    { "documentCode": 15, "name": "X Exempelblankett I", "sortOrder": 0, "isActive": true, "isForm": true, "canChangeOccurenceDate": true },
		    { "documentCode": 4, "name": "EK Utredning", "sortOrder": 20, "isActive": true, "isForm": false, "canChangeOccurenceDate": false, "writeProtectAuto": true },
		    { "documentCode": 9, "name": "Utgången typ", "sortOrder": 5, "isActive": false, "isForm": false, "canChangeOccurenceDate": true },
		    { "documentCode": 1, "name": "EK Brev", "sortOrder": 0, "isActive": true, "isForm": false, "canChangeOccurenceDate": true } ],
		  "document": { "content": null, "title": "", "documentId": 0, "ownerId": 2, "ownerType": 23, "documentTypeCode": 0, "noteTypeCode": 0,
		    "occurenceDate": "2026-09-23", "lifecareCreated": true } }
		""";

	static JsonNode json(final String text) {
		return JSON.readTree(text);
	}

	@Test
	void toRecordsSplitsByTypeCode() {
		final var records = LifecareRecordMapper.toRecords(json(LIST));

		assertThat(records.getJournalNotes()).extracting(LifecareRecord::getId).containsExactly("1");
		assertThat(records.getDocuments()).extracting(LifecareRecord::getId).containsExactly("2", "3", "5");
	}

	@Test
	void toRecordsOnAnEmptyAnswer() {
		final var records = LifecareRecordMapper.toRecords(MissingNode.getInstance());

		assertThat(records.getJournalNotes()).isEmpty();
		assertThat(records.getDocuments()).isEmpty();
	}

	@Test
	void toRecordJoinsDateAndTimeAndSignature() {
		final var models = json(LIST).path("documentModels");

		assertThat(LifecareRecordMapper.toRecord(models.path(0), JOURNAL_NOTE)).isEqualTo(LifecareRecord.create()
			.withId("1")
			.withCategory(JOURNAL_NOTE)
			.withTitle("Journalanteckning")
			.withDateTime("2026-09-22T17:01")
			.withType("Beslut")
			.withOwnerTypeText("EK Ekonomiskt bistånd")
			.withResponsibleCaseworker("RPA_031DEV")
			.withModifiedBy("RPA_031DEV 2026-09-22")
			.withLocked(false)
			.withWriteProtected(true)
			.withDocumentKind("JournalNote"));
		assertThat(LifecareRecordMapper.toRecord(models.path(1), DOCUMENT)).satisfies(record -> {
			assertThat(record.getDocumentKind()).isEqualTo("Pdf");
			assertThat(record.getDateTime()).isEqualTo("2026-09-21");
			assertThat(record.getModifiedBy()).isEqualTo("2026-09-21");
			assertThat(record.getResponsibleCaseworker()).isNull();
			assertThat(record.getLocked()).isTrue();
		});
	}

	@Test
	void textRecordIdsLeavesOutBlanketterAndPdfs() {
		final var list = json(LIST);

		assertThat(LifecareRecordMapper.textRecordIds(list, JOURNAL_NOTE)).containsExactly("1");
		assertThat(LifecareRecordMapper.textRecordIds(list, DOCUMENT)).containsExactly("3");
	}

	@Test
	void containsRecord() {
		final var list = json(LIST);

		assertThat(LifecareRecordMapper.containsRecord(list, 3)).isTrue();
		assertThat(LifecareRecordMapper.containsRecord(list, 6)).isTrue();
		assertThat(LifecareRecordMapper.containsRecord(list, 99)).isFalse();
		assertThat(LifecareRecordMapper.containsRecord(MissingNode.getInstance(), 3)).isFalse();
	}

	@Test
	void findRecord() {
		final var list = json(LIST);

		assertThat(LifecareRecordMapper.findRecord(list, 2, DOCUMENT)).hasValueSatisfying(row -> assertThat(row.path("id").asInt()).isEqualTo(2));
		assertThat(LifecareRecordMapper.findRecord(list, 1, DOCUMENT)).isEmpty();
		assertThat(LifecareRecordMapper.findRecord(list, 4, DOCUMENT)).isEmpty();
		assertThat(LifecareRecordMapper.findRecord(list, 99, DOCUMENT)).isEmpty();
		assertThat(LifecareRecordMapper.findRecord(MissingNode.getInstance(), 2, DOCUMENT)).isEmpty();
	}

	@ParameterizedTest
	@MethodSource("editabilityArguments")
	void isEditable(final String record, final boolean expected) {
		assertThat(LifecareRecordMapper.isEditable(json(record))).isEqualTo(expected);
	}

	private static Stream<Arguments> editabilityArguments() {
		return Stream.of(
			Arguments.of("{\"protected\": false, \"locked\": false}", true),
			Arguments.of("{\"lockedSignature\": \"\", \"lockedDate\": null}", true),
			Arguments.of("{\"protected\": true}", false),
			Arguments.of("{\"locked\": true}", false),
			Arguments.of("{\"protected\": false, \"lockedSignature\": \"ebb14eri\"}", false),
			Arguments.of("{\"lockedDate\": \"2026-09-24\"}", false));
	}

	@Test
	void toRecordContentReadsBodyDateAndTime() {
		final var record = json("""
			{ "documentId": 135, "title": "Journalanteckning", "content": "<p>hej</p>", "occurenceDate": "2026-09-23", "time": "09:02", "protected": false }
			""");

		final var content = LifecareRecordMapper.toRecordContent(record, JOURNAL_NOTE);

		assertThat(content.getId()).isEqualTo("135");
		assertThat(content.getCategory()).isEqualTo(JOURNAL_NOTE);
		assertThat(content.getTitle()).isEqualTo("Journalanteckning");
		assertThat(content.getContent()).isEqualTo("<p>hej</p>");
		assertThat(content.getOccurenceDate()).isEqualTo("2026-09-23");
		assertThat(content.getTime()).isEqualTo("09:02");
		assertThat(content.getEditable()).isTrue();
	}

	@Test
	void toRecordContentFallsBackToOccurenceTime() {
		final var content = LifecareRecordMapper.toRecordContent(json("{\"documentId\": 1, \"time\": null, \"occurenceTime\": \"08:00\"}"), DOCUMENT);

		assertThat(content.getTime()).isEqualTo("08:00");
		assertThat(content.getContent()).isEmpty();
	}

	@Test
	void toRecordContentWithoutAnId() {
		assertThat(LifecareRecordMapper.toRecordContent(json("{}"), DOCUMENT).getId()).isEmpty();
	}

	@Test
	void applyRecordEditChangesOnlyTheEditedFields() {
		final var record = (ObjectNode) json("""
			{ "documentId": 135, "content": "<p>old</p>", "occurenceDate": "2026-09-01", "time": "08:00",
			  "documentNoteType": { "id": 1, "name": "Journalanteckning" }, "ownerId": 7, "ownerType": 53, "protected": false }
			""");

		final var updated = LifecareRecordMapper.applyRecordEdit(record,
			UpdateLifecareRecordRequest.create().withContent("<p>new</p>").withOccurenceDate("2026-09-23").withTime("09:02"));

		assertThat(updated.path("content").stringValue()).isEqualTo("<p>new</p>");
		assertThat(updated.path("occurenceDate").stringValue()).isEqualTo("2026-09-23");
		assertThat(updated.path("time").stringValue()).isEqualTo("09:02");
		assertThat(updated.path("occurenceTime").stringValue()).isEqualTo("09:02");
		assertThat(updated.path("documentNoteType")).isEqualTo(record.path("documentNoteType"));
		assertThat(updated.path("ownerId").intValue()).isEqualTo(7);
		assertThat(updated.path("protected").booleanValue()).isFalse();
		// The record read from Lifecare is not touched.
		assertThat(record.path("content").stringValue()).isEqualTo("<p>old</p>");
	}

	@Test
	void applyRecordEditKeepsDateAndTimeWhenLeftOutAndWriteProtectsOnRequest() {
		final var record = (ObjectNode) json("{\"documentId\": 1, \"occurenceDate\": \"2026-09-01\", \"time\": \"08:00\", \"protected\": false}");

		final var updated = LifecareRecordMapper.applyRecordEdit(record, UpdateLifecareRecordRequest.create().withContent("<p>x</p>").withWriteProtected(true));

		assertThat(updated.path("occurenceDate").stringValue()).isEqualTo("2026-09-01");
		assertThat(updated.path("time").stringValue()).isEqualTo("08:00");
		assertThat(updated.path("protected").booleanValue()).isTrue();
	}

	@Test
	void toNoteTypesListsTheActiveOnesInLifecareOrder() {
		assertThat(LifecareRecordMapper.toNoteTypes(json(NOTE_PROPOSAL))).containsExactly(
			LifecareNoteType.create().withCode(3).withName("Beslut").withProtectedByDefault(true),
			LifecareNoteType.create().withCode(1).withName("Journalanteckning").withProtectedByDefault(false));
	}

	@Test
	void toJournalNoteBodyFillsTheBlankNote() {
		final var proposal = json(NOTE_PROPOSAL);
		final var blank = (ObjectNode) proposal.path("documentJournalNote");
		final var noteType = proposal.path("documentNoteTypes").path(1);

		final var body = LifecareRecordMapper.toJournalNoteBody(blank, noteType, CreateLifecareJournalNoteRequest.create().withContent("<p>Hej</p>").withNoteTypeCode(1));

		final var expected = blank.deepCopy();
		expected.put("content", "<p>Hej</p>");
		expected.put("title", "Journalanteckning");
		expected.put("noteTypeCode", 1);
		expected.put("protected", false);
		assertThat(body.toString()).isEqualTo(expected.toString());
	}

	@Test
	void toJournalNoteBodyTakesWhatTheCaseworkerGave() {
		final var proposal = json(NOTE_PROPOSAL);
		final var blank = (ObjectNode) proposal.path("documentJournalNote");
		final var request = CreateLifecareJournalNoteRequest.create().withContent("<p>Hej</p>").withNoteTypeCode(1).withTitle(" Telefonsamtal ")
			.withOccurenceTime("11:50").withOccurenceDate("2026-09-20").withWriteProtected(true);

		final var body = LifecareRecordMapper.toJournalNoteBody(blank, proposal.path("documentNoteTypes").path(1), request);

		assertThat(body.path("title").stringValue()).isEqualTo("Telefonsamtal");
		assertThat(body.path("occurenceTime").stringValue()).isEqualTo("11:50");
		assertThat(body.path("occurenceDate").stringValue()).isEqualTo("2026-09-20");
		assertThat(body.path("protected").booleanValue()).isTrue();
	}

	@Test
	void toJournalNoteBodyFollowsTheNoteTypeOnSkrivskydd() {
		final var proposal = json(NOTE_PROPOSAL);
		final var blank = (ObjectNode) proposal.path("documentJournalNote");
		final var decisionType = proposal.path("documentNoteTypes").path(0);

		assertThat(LifecareRecordMapper.toJournalNoteBody(blank, decisionType, CreateLifecareJournalNoteRequest.create().withContent("x").withTitle("  "))
			.path("protected").booleanValue()).isTrue();
		assertThat(LifecareRecordMapper.toJournalNoteBody(blank, decisionType, CreateLifecareJournalNoteRequest.create().withContent("x").withWriteProtected(false))
			.path("protected").booleanValue()).isFalse();
	}

	@Test
	void toDocumentTypesListsTheWritableOnesInLifecareOrder() {
		assertThat(LifecareRecordMapper.toDocumentTypes(json(DOCUMENT_PROPOSAL))).containsExactly(
			LifecareDocumentType.create().withCode(1).withName("EK Brev").withCanChangeOccurenceDate(true).withProtectedByDefault(false),
			LifecareDocumentType.create().withCode(4).withName("EK Utredning").withCanChangeOccurenceDate(false).withProtectedByDefault(true));
	}

	@Test
	void toDocumentBodyFillsTheBlankDocument() {
		final var proposal = json(DOCUMENT_PROPOSAL);
		final var blank = (ObjectNode) proposal.path("document");
		final var letterType = proposal.path("documentTypes").path(3);

		final var body = LifecareRecordMapper.toDocumentBody(blank, letterType, CreateLifecareDocumentRequest.create().withContent("<p>Hej</p>").withDocumentTypeCode(1));

		final var expected = blank.deepCopy();
		expected.put("content", "<p>Hej</p>");
		expected.put("title", "EK Brev");
		expected.put("documentTypeCode", 1);
		expected.put("protected", false);
		assertThat(body.toString()).isEqualTo(expected.toString());
	}

	@Test
	void toDocumentBodyTakesTheDateOnlyWhenTheTypeAllowsIt() {
		final var proposal = json(DOCUMENT_PROPOSAL);
		final var blank = (ObjectNode) proposal.path("document");
		final var request = CreateLifecareDocumentRequest.create().withContent("<p>Hej</p>").withTitle(" Beslut om bistånd ").withOccurenceDate("2026-09-20")
			.withWriteProtected(true);

		final var letter = LifecareRecordMapper.toDocumentBody(blank, proposal.path("documentTypes").path(3), request);
		final var fixedDate = LifecareRecordMapper.toDocumentBody(blank, proposal.path("documentTypes").path(1), request);

		assertThat(letter.path("title").stringValue()).isEqualTo("Beslut om bistånd");
		assertThat(letter.path("occurenceDate").stringValue()).isEqualTo("2026-09-20");
		assertThat(letter.path("protected").booleanValue()).isTrue();
		assertThat(fixedDate.path("occurenceDate").stringValue()).isEqualTo("2026-09-23");
	}
}
