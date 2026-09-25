package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

	/**
	 * Which group a Lifecare row is shown under, by its typeCode: 3 under Journal, 1 and 13 under Dokument. A row of any
	 * other code is shown under neither. Verksamheten's split as of 2026-09-24; Lifecare's codes are undocumented.
	 */
	private static final Map<Integer, String> CATEGORY_BY_TYPE_CODE = Map.of(3, JOURNAL_NOTE, 1, DOCUMENT, 13, DOCUMENT);

	/** Rows with no written body: a blankett is built from form fields, a PDF is a file. */
	private static final List<String> NO_TEXT_BODY_TYPES = List.of("Form", "Pdf");

	private LifecareRecordMapper() {}

	/**
	 * The group a Lifecare row is shown under.
	 *
	 * @param  model a row of GetDocumentsListForClient
	 * @return       JOURNAL_NOTE or DOCUMENT, empty when the row is shown under neither
	 */
	public static Optional<String> categoryOf(final JsonNode model) {
		return Optional.ofNullable(integer(model.path("typeCode"))).map(CATEGORY_BY_TYPE_CODE::get);
	}

	/**
	 * Splits Lifecare's flat list into journalanteckningar and documents, keeping Lifecare's order. A row of any other
	 * type code is left out.
	 *
	 * @param  list the GetDocumentsListForClient answer
	 * @return      the two groups
	 */
	public static LifecareRecords toRecords(final JsonNode list) {
		final var records = list.path("documentModels").valueStream()
			.flatMap(model -> categoryOf(model).map(category -> toRecord(model, category)).stream())
			.toList();
		return LifecareRecords.create()
			.withJournalNotes(records.stream().filter(record -> JOURNAL_NOTE.equals(record.getCategory())).toList())
			.withDocuments(records.stream().filter(record -> DOCUMENT.equals(record.getCategory())).toList());
	}

	/**
	 * Whether a record id is in Lifecare's list for the client.
	 *
	 * @param  list     the GetDocumentsListForClient answer
	 * @param  recordId the record id
	 * @return          true when the list holds the record
	 */
	public static boolean containsRecord(final JsonNode list, final int recordId) {
		return list.path("documentModels").valueStream()
			.anyMatch(model -> Integer.valueOf(recordId).equals(integer(model.path("id"))));
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
		return list.path("documentModels").valueStream()
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
		return list.path("documentModels").valueStream()
			.filter(model -> categoryOf(model).filter(category::equals).isPresent())
			.filter(model -> !NO_TEXT_BODY_TYPES.contains(textOrEmpty(model.path("documentType_Name"))))
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
			.withTitle(text(model.path("title")))
			.withDateTime(dateTime)
			.withType(text(model.path("type")))
			.withOwnerTypeText(text(model.path("ownerTypeText")))
			.withResponsibleCaseworker(text(model.path("responsibleCaseworker")))
			.withModifiedBy(modifiedBy)
			.withLocked(model.path("locked").asBoolean(false))
			.withWriteProtected(model.path("protected").asBoolean(false))
			.withDocumentKind(text(model.path("documentType_Name")));
	}

	/**
	 * Whether a record may still be edited. A finalised (upprättad) record is protected, a locked one carries a lock flag,
	 * signature or date; any of them closes it.
	 *
	 * @param  record the record as GetJournalNoteWithContent or GetDocumentWithContent returns it
	 * @return        true when editable
	 */
	public static boolean isEditable(final JsonNode record) {
		return !isTrue(record.path("protected")) && !isTrue(record.path("locked")) && !truthy(record.path("lockedSignature"))
			&& !truthy(record.path("lockedDate"));
	}

	/**
	 * The record with its body, as shown when it is opened.
	 *
	 * @param  record   the record as Lifecare returns it
	 * @param  category JOURNAL_NOTE or DOCUMENT
	 * @return          the content view
	 */
	public static LifecareRecordContent toRecordContent(final JsonNode record, final String category) {
		var time = record.path("time");
		if (time.isMissingNode() || time.isNull()) {
			time = record.path("occurenceTime");
		}
		return LifecareRecordContent.create()
			.withId(idText(record.path("documentId")))
			.withCategory(category)
			.withTitle(textOrEmpty(record.path("title")))
			.withContent(textOrEmpty(record.path("content")))
			.withOccurenceDate(textOrEmpty(record.path("occurenceDate")))
			.withTime(textOrEmpty(time))
			.withEditable(isEditable(record));
	}

	/**
	 * The caseworker's edit applied onto a copy of Lifecare's own record. Everything not named here is kept as Lifecare
	 * returned it. The time is written to both time and occurenceTime, which Lifecare carries side by side. Skrivskydd is
	 * only ever switched on.
	 *
	 * @param  record the record as its editor opened it
	 * @param  edit   the edit
	 * @return        the body to post
	 */
	public static ObjectNode applyRecordEdit(final ObjectNode record, final UpdateLifecareRecordRequest edit) {
		final var updated = record.deepCopy();
		updated.put("content", edit.getContent());
		if (hasText(edit.getOccurenceDate())) {
			updated.put("occurenceDate", edit.getOccurenceDate());
		}
		if (hasText(edit.getTime())) {
			updated.put("time", edit.getTime());
			updated.put("occurenceTime", edit.getTime());
		}
		if (Boolean.TRUE.equals(edit.getWriteProtected())) {
			updated.put("protected", true);
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
				.withProtectedByDefault(isTrue(noteType.path("writeProtectAuto"))))
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
		body.put("content", request.getContent());
		body.put("title", titleOrDefault(request.getTitle(), text(noteType.path("name"))));
		body.put("noteTypeCode", integer(noteType.path("id")));
		body.put("protected", Optional.ofNullable(request.getWriteProtected()).orElse(isTrue(noteType.path("writeProtectAuto"))));
		if (hasText(request.getOccurenceDate())) {
			body.put("occurenceDate", request.getOccurenceDate());
		}
		if (hasText(request.getOccurenceTime())) {
			body.put("occurenceTime", request.getOccurenceTime());
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
				.withProtectedByDefault(isTrue(documentType.path("writeProtectAuto"))))
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
		body.put("content", request.getContent());
		body.put("title", titleOrDefault(request.getTitle(), text(documentType.path("name"))));
		body.put("documentTypeCode", integer(documentType.path("documentCode")));
		body.put("protected", Optional.ofNullable(request.getWriteProtected()).orElse(isTrue(documentType.path("writeProtectAuto"))));
		if (hasText(request.getOccurenceDate()) && isTrue(documentType.path("canChangeOccurenceDate"))) {
			body.put("occurenceDate", request.getOccurenceDate());
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
