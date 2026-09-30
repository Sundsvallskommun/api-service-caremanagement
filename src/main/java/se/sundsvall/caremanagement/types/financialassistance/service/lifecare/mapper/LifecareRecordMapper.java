package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.CreateLifecareDocumentRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.CreateLifecareJournalNoteRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDocumentType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareNoteType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecord;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecordContent;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecords;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.UpdateLifecareRecordRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.isTrue;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.textOrEmpty;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.truthy;

/**
 * Maps Lifecare's document answers (Document/GetDocumentsListForClient, the record reads and the note and document
 * proposals) onto careM's API models, and builds the bodies Lifecare's create and update endpoints take back.
 *
 * <p>
 * Every write body is the object Lifecare handed out (the proposal's blank record, or the record as its editor opened
 * it) with a few fields set, so nothing Lifecare expects back is lost on the way.
 * </p>
 */
public final class LifecareRecordMapper {

	/** A journalanteckning. */
	public static final String JOURNAL_NOTE = "JOURNAL_NOTE";

	/** A document. */
	public static final String DOCUMENT = "DOCUMENT";

	/** Lifecare's kind of a journalanteckning row; every other kind (Regular, Form, Pdf) is a document. */
	private static final String KIND_JOURNAL_NOTE = "JournalNote";

	/**
	 * The journalanteckning type codes shown under Journal: 1 Journalanteckning, 3 Beslut. A note's typeCode is its note
	 * type, from its own code list, so it is read together with the row's kind.
	 */
	private static final Set<Integer> JOURNAL_NOTE_TYPE_CODES = Set.of(1, 3);

	/**
	 * The document type codes shown under Dokument: 1 Inkommen handling, 13 brev, 14 and 15 dokument och blanketter.
	 * Verksamheten's list as of 2026-09-24; Lifecare's codes are undocumented, and a row of any other code is shown
	 * under neither group.
	 */
	private static final Set<Integer> DOCUMENT_TYPE_CODES = Set.of(1, 13, 14, 15);

	/** Rows with no written body: a blankett is built from form fields, a PDF is a file. */
	private static final List<String> NO_TEXT_BODY_TYPES = List.of("Form", "Pdf");

	private static final String FIELD_DOCUMENT_MODELS = "documentModels";
	private static final String FIELD_DOCUMENT_TYPE_NAME = "documentType_Name";
	private static final String FIELD_TITLE = "title";
	private static final String FIELD_PROTECTED = "protected";
	private static final String FIELD_OCCURENCE_TIME = "occurenceTime";
	private static final String FIELD_CONTENT = "content";
	private static final String FIELD_OCCURENCE_DATE = "occurenceDate";
	private static final String FIELD_WRITE_PROTECT_AUTO = "writeProtectAuto";

	private LifecareRecordMapper() {}

	/**
	 * The group a Lifecare row is shown under: a journalanteckning under Journal and any other kind under Dokument, each
	 * only for the type codes that group shows. Journal notes and documents have separate type code lists (1 is
	 * Journalanteckning for a note and Inkommen handling for a document), so the kind decides the group.
	 *
	 * @param  model a row of GetDocumentsListForClient
	 * @return       JOURNAL_NOTE or DOCUMENT, empty when the row is shown under neither
	 */
	public static Optional<String> categoryOf(final JsonNode model) {
		final var typeCode = integer(model.path("typeCode"));
		if (typeCode == null) {
			return Optional.empty();
		}
		if (KIND_JOURNAL_NOTE.equals(textOrEmpty(model.path(FIELD_DOCUMENT_TYPE_NAME)))) {
			return Optional.of(JOURNAL_NOTE).filter(category -> JOURNAL_NOTE_TYPE_CODES.contains(typeCode));
		}
		return Optional.of(DOCUMENT).filter(category -> DOCUMENT_TYPE_CODES.contains(typeCode));
	}

	/**
	 * Splits Lifecare's flat list into journalanteckningar and documents, keeping Lifecare's order. A row neither group
	 * shows is left out.
	 *
	 * @param  list the GetDocumentsListForClient answer
	 * @return      the two groups
	 */
	public static LifecareRecords toRecords(final JsonNode list) {
		final var records = list.path(FIELD_DOCUMENT_MODELS).valueStream()
			.flatMap(model -> categoryOf(model).map(category -> toRecord(model, category)).stream())
			.toList();
		return LifecareRecords.create()
			.withJournalNotes(records.stream().filter(row -> JOURNAL_NOTE.equals(row.getCategory())).toList())
			.withDocuments(records.stream().filter(row -> DOCUMENT.equals(row.getCategory())).toList());
	}

	/**
	 * One row of Lifecare's list for the client, when it is in the given group.
	 *
	 * @param  list     the GetDocumentsListForClient answer
	 * @param  recordId the record id
	 * @param  category JOURNAL_NOTE or DOCUMENT
	 * @return          the row, empty when the list holds no such row in that group
	 */
	public static Optional<JsonNode> findRecord(final JsonNode list, final int recordId, final String category) {
		return list.path(FIELD_DOCUMENT_MODELS).valueStream()
			.filter(model -> Integer.valueOf(recordId).equals(integer(model.path("id"))))
			.filter(model -> categoryOf(model).filter(category::equals).isPresent())
			.findFirst();
	}

	/**
	 * The ids of the rows in one group that carry a written body worth showing.
	 *
	 * @param  list     the GetDocumentsListForClient answer
	 * @param  category JOURNAL_NOTE or DOCUMENT
	 * @return          the ids, in Lifecare's order
	 */
	public static List<String> textRecordIds(final JsonNode list, final String category) {
		return list.path(FIELD_DOCUMENT_MODELS).valueStream()
			.filter(model -> categoryOf(model).filter(category::equals).isPresent())
			.filter(model -> !NO_TEXT_BODY_TYPES.contains(textOrEmpty(model.path(FIELD_DOCUMENT_TYPE_NAME))))
			.map(model -> idText(model.path("id")))
			.toList();
	}

	/**
	 * One Lifecare row (from the list, or the row a create answers with) as the UI shows it.
	 *
	 * @param  model    the row
	 * @param  category JOURNAL_NOTE or DOCUMENT
	 * @return          the record
	 */
	public static LifecareRecord toRecord(final JsonNode model, final String category) {
		final var date = text(model.path("date"));
		final var updateDate = text(model.path("updateDate"));
		// Lifecare hands date and time apart; they are joined so the UI can format one value.
		var dateTime = date;
		if (truthy(model.path("time"))) {
			dateTime = date + "T" + text(model.path("time"));
		}
		var modifiedBy = updateDate;
		if (truthy(model.path("updateSignature"))) {
			modifiedBy = text(model.path("updateSignature")) + " " + updateDate;
		}
		return LifecareRecord.create()
			.withId(idText(model.path("id")))
			.withCategory(category)
			.withTitle(text(model.path(FIELD_TITLE)))
			.withDateTime(dateTime)
			.withType(text(model.path("type")))
			.withOwnerTypeText(text(model.path("ownerTypeText")))
			.withResponsibleCaseworker(text(model.path("responsibleCaseworker")))
			.withModifiedBy(modifiedBy)
			.withLocked(model.path("locked").asBoolean(false))
			.withWriteProtected(model.path(FIELD_PROTECTED).asBoolean(false))
			.withDocumentKind(text(model.path(FIELD_DOCUMENT_TYPE_NAME)));
	}

	/**
	 * Whether a record may still be edited. A finalised (upprättad) record is protected, a locked one carries a lock flag,
	 * signature or date; any of them closes it.
	 *
	 * @param  row the record as GetJournalNoteWithContent or GetDocumentWithContent returns it
	 * @return     true when editable
	 */
	public static boolean isEditable(final JsonNode row) {
		return !isTrue(row.path(FIELD_PROTECTED)) && !isTrue(row.path("locked")) && !truthy(row.path("lockedSignature"))
			&& !truthy(row.path("lockedDate"));
	}

	/**
	 * The record with its body, as shown when it is opened.
	 *
	 * @param  row      the record as Lifecare returns it
	 * @param  category JOURNAL_NOTE or DOCUMENT
	 * @return          the content view
	 */
	public static LifecareRecordContent toRecordContent(final JsonNode row, final String category) {
		var time = row.path("time");
		if (time.isMissingNode() || time.isNull()) {
			time = row.path(FIELD_OCCURENCE_TIME);
		}
		return LifecareRecordContent.create()
			.withId(idText(row.path("documentId")))
			.withCategory(category)
			.withTitle(textOrEmpty(row.path(FIELD_TITLE)))
			.withContent(textOrEmpty(row.path(FIELD_CONTENT)))
			.withOccurenceDate(textOrEmpty(row.path(FIELD_OCCURENCE_DATE)))
			.withTime(textOrEmpty(time))
			.withEditable(isEditable(row));
	}

	/**
	 * The caseworker's edit applied onto a copy of Lifecare's own record. Everything not named here is kept as Lifecare
	 * returned it. The time is written to both time and occurenceTime, which Lifecare carries side by side. Skrivskydd is
	 * only ever switched on.
	 *
	 * @param  row  the record as its editor opened it
	 * @param  edit the edit
	 * @return      the body to post
	 */
	public static ObjectNode applyRecordEdit(final ObjectNode row, final UpdateLifecareRecordRequest edit) {
		final var updated = row.deepCopy();
		updated.put(FIELD_CONTENT, edit.getContent());
		if (hasText(edit.getOccurenceDate())) {
			updated.put(FIELD_OCCURENCE_DATE, edit.getOccurenceDate());
		}
		if (hasText(edit.getTime())) {
			updated.put("time", edit.getTime());
			updated.put(FIELD_OCCURENCE_TIME, edit.getTime());
		}
		if (Boolean.TRUE.equals(edit.getWriteProtected())) {
			updated.put(FIELD_PROTECTED, true);
		}
		return updated;
	}

	/**
	 * The active note types of a note proposal, in Lifecare's order.
	 *
	 * @param  proposal the GetNoteProposalForService answer
	 * @return          the note types
	 */
	public static List<LifecareNoteType> toNoteTypes(final JsonNode proposal) {
		return activeNoteTypes(proposal).stream()
			.map(noteType -> LifecareNoteType.create()
				.withCode(integer(noteType.path("id")))
				.withName(text(noteType.path("name")))
				.withProtectedByDefault(isTrue(noteType.path(FIELD_WRITE_PROTECT_AUTO))))
			.toList();
	}

	/**
	 * The active note types of a note proposal as Lifecare lists them, in Lifecare's order.
	 *
	 * @param  proposal the GetNoteProposalForService answer
	 * @return          the note type nodes
	 */
	public static List<JsonNode> activeNoteTypes(final JsonNode proposal) {
		return proposal.path("documentNoteTypes").valueStream()
			.filter(noteType -> isTrue(noteType.path("isActive")))
			.sorted(Comparator.comparingInt(noteType -> noteType.path("sortOrder").asInt(0)))
			.toList();
	}

	/**
	 * The proposal's blank note filled in with what the caseworker wrote. Without a rubrik of its own the note is titled
	 * by its type. Skrivskydd is always sent, from the caseworker or else the note type's default.
	 *
	 * @param  blank    the proposal's documentJournalNote
	 * @param  noteType the note type picked
	 * @param  request  what the caseworker wrote
	 * @return          the body to post
	 */
	public static ObjectNode toJournalNoteBody(final ObjectNode blank, final JsonNode noteType, final CreateLifecareJournalNoteRequest request) {
		final var body = blank.deepCopy();
		body.put(FIELD_CONTENT, request.getContent());
		body.put(FIELD_TITLE, titleOrDefault(request.getTitle(), text(noteType.path("name"))));
		body.put("noteTypeCode", integer(noteType.path("id")));
		body.put(FIELD_PROTECTED, Optional.ofNullable(request.getWriteProtected()).orElse(isTrue(noteType.path(FIELD_WRITE_PROTECT_AUTO))));
		if (hasText(request.getOccurenceDate())) {
			body.put(FIELD_OCCURENCE_DATE, request.getOccurenceDate());
		}
		if (hasText(request.getOccurenceTime())) {
			body.put(FIELD_OCCURENCE_TIME, request.getOccurenceTime());
		}
		return body;
	}

	/**
	 * The document types a free-text body can be written as: active, and not a blankett, in Lifecare's order.
	 *
	 * @param  proposal the GetDocumentProposalForService answer
	 * @return          the document type nodes
	 */
	public static List<JsonNode> writableDocumentTypes(final JsonNode proposal) {
		return proposal.path("documentTypes").valueStream()
			.filter(documentType -> isTrue(documentType.path("isActive")))
			.filter(documentType -> !isTrue(documentType.path("isForm")))
			.sorted(Comparator.comparingInt(documentType -> documentType.path("sortOrder").asInt(0)))
			.toList();
	}

	/**
	 * The writable document types of a document proposal.
	 *
	 * @param  proposal the GetDocumentProposalForService answer
	 * @return          the document types
	 */
	public static List<LifecareDocumentType> toDocumentTypes(final JsonNode proposal) {
		return writableDocumentTypes(proposal).stream()
			.map(documentType -> LifecareDocumentType.create()
				.withCode(integer(documentType.path("documentCode")))
				.withName(text(documentType.path("name")))
				.withCanChangeOccurenceDate(isTrue(documentType.path("canChangeOccurenceDate")))
				.withProtectedByDefault(isTrue(documentType.path(FIELD_WRITE_PROTECT_AUTO))))
			.toList();
	}

	/**
	 * The proposal's blank document filled in with what the caseworker wrote. A type that fixes its date keeps the date
	 * Lifecare proposed.
	 *
	 * @param  blank        the proposal's document
	 * @param  documentType the document type picked
	 * @param  request      what the caseworker wrote
	 * @return              the body to post
	 */
	public static ObjectNode toDocumentBody(final ObjectNode blank, final JsonNode documentType, final CreateLifecareDocumentRequest request) {
		final var body = blank.deepCopy();
		body.put(FIELD_CONTENT, request.getContent());
		body.put(FIELD_TITLE, titleOrDefault(request.getTitle(), text(documentType.path("name"))));
		body.put("documentTypeCode", integer(documentType.path("documentCode")));
		body.put(FIELD_PROTECTED, Optional.ofNullable(request.getWriteProtected()).orElse(isTrue(documentType.path(FIELD_WRITE_PROTECT_AUTO))));
		if (hasText(request.getOccurenceDate()) && isTrue(documentType.path("canChangeOccurenceDate"))) {
			body.put(FIELD_OCCURENCE_DATE, request.getOccurenceDate());
		}
		return body;
	}

	private static String titleOrDefault(final String title, final String typeName) {
		return Optional.ofNullable(title)
			.map(String::trim)
			.filter(trimmed -> !trimmed.isEmpty())
			.orElse(typeName);
	}

	private static String idText(final JsonNode id) {
		if (id.isMissingNode() || id.isNull()) {
			return "";
		}
		return id.asString();
	}
}
