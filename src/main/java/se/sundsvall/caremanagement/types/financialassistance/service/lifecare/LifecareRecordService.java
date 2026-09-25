package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecordContent;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRecords;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.UpdateLifecareRecordRequest;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareNodeValues.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.DOCUMENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.JOURNAL_NOTE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.activeNoteTypes;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.applyRecordEdit;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.containsRecord;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.findRecord;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.isEditable;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.textRecordIds;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.toDocumentBody;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.toDocumentTypes;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.toJournalNoteBody;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.toNoteTypes;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.toRecord;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.toRecordContent;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.toRecords;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper.LifecareRecordMapper.writableDocumentTypes;

/**
 * The applicant's Lifecare journalanteckningar and documents, seen from an errand, read and written live in Lifecare.
 * careM keeps nothing of them but the access-log rows.
 *
 * <p>
 * Lifecare's list is keyed on the person, not the errand: it spans every akt the applicant has. The personnummer comes
 * from the errand, never from the caller. A record read or changed by id must be in that list, so an errand cannot
 * reach
 * another person's record by naming its id. New records are written on the errand's insats.
 * </p>
 */
@Service
public class LifecareRecordService {

	static final String PATH_LIST = "api2/Document/GetDocumentsListForClient/";
	static final String PATH_READ_NOTE = "api2/Document/GetJournalNoteWithContent/";
	static final String PATH_READ_DOCUMENT = "api2/Document/GetDocumentWithContent/";
	static final String PATH_UPDATE_NOTE = "api2/Document/UpdateJournalNote/";
	static final String PATH_UPDATE_DOCUMENT = "api2/Document/UpdateDocument/";
	static final String PATH_NOTE_PROPOSAL = "api2/Document/GetNoteProposalForService";
	static final String PATH_DOCUMENT_PROPOSAL = "api2/Document/GetDocumentProposalForService";
	static final String PATH_CREATE_NOTE = "api2/Document/CreateJournalNote/";
	static final String PATH_CREATE_DOCUMENT = "api2/Document/CreateDocument/";

	static final String TARGET_ALL = "JOURNAL_AND_DOCUMENTS";

	static final String NOT_THE_CLIENTS = "The record is not in the applicant's Lifecare record";
	static final String FINALISED = "The record is finalised in Lifecare and can no longer be edited";
	static final String UNKNOWN_NOTE_TYPE = "Anteckningstypen finns inte i Lifecare";
	static final String UNKNOWN_DOCUMENT_TYPE = "Dokumenttypen finns inte i Lifecare";
	static final String UNEXPECTED_ANSWER = "Lifecare answered %s without the expected object";
	static final String NO_PDF = "Lifecare has no PDF for the document";
	static final String AMBIGUOUS_PDF = "Lifecare has several documents with the same title and date, so the PDF cannot be picked safely";

	private static final Logger LOG = LoggerFactory.getLogger(LifecareRecordService.class);

	/** The Lifecare endpoints and log wording for one kind of record. */
	private record Kind(String category, String readPath, String updatePath, String readDescription, String updateDescription,
		String bodiesDescription) {
	}

	private static final Kind NOTE = new Kind(JOURNAL_NOTE, PATH_READ_NOTE, PATH_UPDATE_NOTE, "Läste en journalanteckning i Lifecare",
		"Ändrade en journalanteckning i Lifecare", "Läste journalanteckningarnas innehåll i Lifecare");
	private static final Kind DOC = new Kind(DOCUMENT, PATH_READ_DOCUMENT, PATH_UPDATE_DOCUMENT, "Läste ett dokument i Lifecare",
		"Ändrade ett dokument i Lifecare", "Läste dokumentens innehåll i Lifecare");

	private final ProfessionalWebClient client;
	private final LifecareErrandService errandService;
	private final LifecareAccessRecorder recorder;
	private final LifecareCaseHistoryService caseHistoryService;

	LifecareRecordService(final ProfessionalWebClient client, final LifecareErrandService errandService, final LifecareAccessRecorder recorder,
		final LifecareCaseHistoryService caseHistoryService) {
		this.client = client;
		this.errandService = errandService;
		this.recorder = recorder;
		this.caseHistoryService = caseHistoryService;
	}

	/**
	 * The applicant's journalanteckningar and documents, across all of the applicant's akter.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the two groups
	 */
	public LifecareRecords list(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var records = toRecords(readList(errand));
		recorder.read(errand, TARGET_ALL, "Läste journalanteckningar och dokument i Lifecare");
		return records;
	}

	/**
	 * The bodies of the journalanteckningar, so the list can show each one's text without opening it.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the bodies
	 */
	public List<LifecareRecordBody> journalNoteBodies(final String municipalityId, final String namespace, final String errandId) {
		return bodies(municipalityId, namespace, errandId, NOTE);
	}

	/**
	 * The bodies of the documents that have a written body (not blanketter or PDF files).
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the bodies
	 */
	public List<LifecareRecordBody> documentBodies(final String municipalityId, final String namespace, final String errandId) {
		return bodies(municipalityId, namespace, errandId, DOC);
	}

	public LifecareRecordContent readJournalNote(final String municipalityId, final String namespace, final String errandId, final int id) {
		return read(municipalityId, namespace, errandId, id, NOTE);
	}

	public LifecareRecordContent readDocument(final String municipalityId, final String namespace, final String errandId, final int id) {
		return read(municipalityId, namespace, errandId, id, DOC);
	}

	/**
	 * One of the applicant's documents as a PDF file, for sending it on as a bilaga.
	 *
	 * <p>
	 * The document must be in the applicant's ProfessionalWeb list under Dokument. Its file is read from Lifecare's FC API,
	 * whose documents carry their own ids, so the FC document is picked by the row's title and date: none yields 404,
	 * several 409 rather than a guess. FC holds a file only for PDF-backed documents; for the others Lifecare answers 404.
	 * </p>
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @param  id             the ProfessionalWeb record id
	 * @return                the PDF bytes
	 */
	public byte[] readDocumentPdf(final String municipalityId, final String namespace, final String errandId, final int id) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var row = findRecord(readList(errand), id, DOCUMENT)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOT_THE_CLIENTS));
		final var title = text(row.path("title"));
		final var date = text(row.path("date"));
		if (title == null || date == null) {
			throw Problem.valueOf(NOT_FOUND, NO_PDF);
		}
		final var day = LocalDate.parse(date.substring(0, Math.min(date.length(), 10)));
		final var matches = caseHistoryService.listDocuments(municipalityId, errandService.applicantPartyId(errand), day, day).stream()
			.filter(document -> title.equals(document.title()))
			.filter(document -> document.date() != null && document.date().startsWith(day.toString()))
			.map(DocumentView::id)
			.distinct()
			.toList();
		if (matches.isEmpty()) {
			throw Problem.valueOf(NOT_FOUND, NO_PDF);
		}
		if (matches.size() > 1) {
			throw Problem.valueOf(CONFLICT, AMBIGUOUS_PDF);
		}
		final var pdf = caseHistoryService.documentContent(municipalityId, matches.getFirst());
		recorder.read(errand, DOCUMENT, "Hämtade ett dokument som PDF ur Lifecare", String.valueOf(id));
		return pdf;
	}

	public LifecareRecordContent updateJournalNote(final String municipalityId, final String namespace, final String errandId, final int id,
		final UpdateLifecareRecordRequest edit) {
		return update(municipalityId, namespace, errandId, id, edit, NOTE);
	}

	public LifecareRecordContent updateDocument(final String municipalityId, final String namespace, final String errandId, final int id,
		final UpdateLifecareRecordRequest edit) {
		return update(municipalityId, namespace, errandId, id, edit, DOC);
	}

	/**
	 * The note types a new journalanteckning on the errand's insats can have.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the note types
	 */
	public List<LifecareNoteType> journalNoteTypes(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		return toNoteTypes(client.get(PATH_NOTE_PROPOSAL, Map.of("id", String.valueOf(errand.requireServiceId()))));
	}

	/**
	 * The document types a new document on the errand's insats can have.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the document types
	 */
	public List<LifecareDocumentType> documentTypes(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		return toDocumentTypes(client.get(PATH_DOCUMENT_PROPOSAL, Map.of("id", String.valueOf(errand.requireServiceId()))));
	}

	/**
	 * Writes a new journalanteckning on the errand's insats, the way Lifecare's own editor does: the proposal's blank note,
	 * filled in.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @param  request        the note
	 * @return                the new row, as Lifecare answers it
	 */
	public LifecareRecord createJournalNote(final String municipalityId, final String namespace, final String errandId,
		final CreateLifecareJournalNoteRequest request) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var proposal = client.get(PATH_NOTE_PROPOSAL, Map.of("id", String.valueOf(errand.requireServiceId())));
		final var noteType = activeNoteTypes(proposal).stream()
			.filter(candidate -> request.getNoteTypeCode().equals(integer(candidate.path("id"))))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, UNKNOWN_NOTE_TYPE));

		final var created = client.post(PATH_CREATE_NOTE, Map.of(),
			toJournalNoteBody(requireObject(proposal.path("documentJournalNote"), PATH_NOTE_PROPOSAL), noteType, request));
		final var record = toRecord(created, JOURNAL_NOTE);
		recorder.written(errand, LifecareAccessEntry.CREATE, JOURNAL_NOTE, "Skrev en journalanteckning i Lifecare", record.getId());
		return record;
	}

	/**
	 * Writes a new document on the errand's insats, the way Lifecare's own editor does: the proposal's blank document,
	 * filled in.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @param  request        the document
	 * @return                the new row, as Lifecare answers it
	 */
	public LifecareRecord createDocument(final String municipalityId, final String namespace, final String errandId,
		final CreateLifecareDocumentRequest request) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var proposal = client.get(PATH_DOCUMENT_PROPOSAL, Map.of("id", String.valueOf(errand.requireServiceId())));
		final var documentType = writableDocumentTypes(proposal).stream()
			.filter(candidate -> request.getDocumentTypeCode().equals(integer(candidate.path("documentCode"))))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, UNKNOWN_DOCUMENT_TYPE));

		final var created = client.post(PATH_CREATE_DOCUMENT, Map.of(),
			toDocumentBody(requireObject(proposal.path("document"), PATH_DOCUMENT_PROPOSAL), documentType, request));
		final var record = toRecord(created, DOCUMENT);
		recorder.written(errand, LifecareAccessEntry.CREATE, DOCUMENT, "Skrev ett dokument i Lifecare", record.getId());
		return record;
	}

	/**
	 * Each body is read on its own, in view mode (a bulk read must not look like the records were opened for editing). A
	 * body Lifecare will not hand over is left out rather than failing the rest; the reads land in the access log as one
	 * row.
	 */
	private List<LifecareRecordBody> bodies(final String municipalityId, final String namespace, final String errandId, final Kind kind) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var bodies = textRecordIds(readList(errand), kind.category()).stream()
			.map(id -> readBody(kind, id))
			.toList();
		recorder.read(errand, TARGET_ALL, kind.bodiesDescription());
		return bodies;
	}

	private LifecareRecordBody readBody(final Kind kind, final String id) {
		try {
			final var record = client.get(kind.readPath(), params(id, "true", "false"));
			final var content = text(record.path("content"));
			if (content == null) {
				return LifecareRecordBody.create().withId(id).withContent("");
			}
			return LifecareRecordBody.create().withId(id).withContent(content);
		} catch (final RuntimeException e) {
			LOG.info("Left out the body of Lifecare record {} ({})", id, e.getClass().getSimpleName());
			return LifecareRecordBody.create().withId(id);
		}
	}

	private LifecareRecordContent read(final String municipalityId, final String namespace, final String errandId, final int id, final Kind kind) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		assertTheClients(errand, id);
		final var content = toRecordContent(readEditable(kind, id), kind.category());
		recorder.read(errand, kind.category(), kind.readDescription(), String.valueOf(id));
		return content;
	}

	/**
	 * Reads the record, applies the edit onto its own object and posts it back. Editability is checked against the freshly
	 * read record rather than trusted from the caller, so a record finalised in Lifecare since it was opened is refused
	 * rather than overwritten.
	 */
	private LifecareRecordContent update(final String municipalityId, final String namespace, final String errandId, final int id,
		final UpdateLifecareRecordRequest edit, final Kind kind) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		assertTheClients(errand, id);
		final var record = readEditable(kind, id);
		if (!isEditable(record)) {
			throw Problem.valueOf(CONFLICT, FINALISED);
		}
		final var updated = applyRecordEdit(record, edit);
		client.post(kind.updatePath(), Map.of(), updated);
		recorder.written(errand, LifecareAccessEntry.UPDATE, kind.category(), kind.updateDescription(), String.valueOf(id));
		return toRecordContent(updated, kind.category());
	}

	/**
	 * The record in the shape Lifecare's own editor works on (inEdit=true), the object that goes back on save.
	 */
	private ObjectNode readEditable(final Kind kind, final int id) {
		return requireObject(client.get(kind.readPath(), params(String.valueOf(id), "false", "true")), kind.readPath());
	}

	private void assertTheClients(final LifecareErrand errand, final int id) {
		if (!containsRecord(readList(errand), id)) {
			throw Problem.valueOf(NOT_FOUND, NOT_THE_CLIENTS);
		}
	}

	private JsonNode readList(final LifecareErrand errand) {
		return client.get(PATH_LIST, Map.of("id", errandService.applicantPersonalNumber(errand)));
	}

	private static ObjectNode requireObject(final JsonNode node, final String path) {
		if (node instanceof final ObjectNode object) {
			return object;
		}
		throw Problem.valueOf(BAD_GATEWAY, UNEXPECTED_ANSWER.formatted(path));
	}

	private static Map<String, String> params(final String id, final String hideRevisions, final String inEdit) {
		final var params = new LinkedHashMap<String, String>();
		params.put("id", id);
		params.put("hideRevisions", hideRevisions);
		params.put("inEdit", inEdit);
		return params;
	}
}
