package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare")
@Tag(name = FinancialAssistanceApiTags.LIFECARE, description = FinancialAssistanceApiTags.LIFECARE_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceLifecareRecordResource {

	private static final String RECORD_LOCATION = "/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare/documents/%s/{id}";

	private final LifecareRecordService service;

	FinancialAssistanceLifecareRecordResource(final LifecareRecordService service) {
		this.service = service;
	}

	@GetMapping(path = "/documents", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the applicant's journalanteckningar and documents in Lifecare",
		description = "The applicant's Lifecare record, split into journalanteckningar and documents, read live from Lifecare. Lifecare's list is person-wide, so it spans all of the applicant's akter, not only this errand. The applicant is resolved from the errand. The read is logged in the errand's access log.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict - the applicant's personnummer could not be resolved", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<LifecareRecords> listRecords(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.list(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/journal-note-bodies", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "The bodies of the applicant's journalanteckningar",
		description = "Each journalanteckning's body, read in view mode, so the list can show its text without opening it. A body Lifecare will not hand over is returned without content rather than failing the rest.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict - the applicant's personnummer could not be resolved", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareRecordBody>> listJournalNoteBodies(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.journalNoteBodies(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/document-bodies", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "The bodies of the applicant's documents",
		description = "Each written document's body (blanketter and PDF files have none), read in view mode. A body Lifecare will not hand over is returned without content rather than failing the rest.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict - the applicant's personnummer could not be resolved", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareRecordBody>> listDocumentBodies(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.documentBodies(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/documents/journal-note-types", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "The note types a new journalanteckning can have",
		description = "The active note types Lifecare offers for a new journalanteckning on the errand's insats, in Lifecare's order.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict - the errand has no Lifecare insats yet", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareNoteType>> listJournalNoteTypes(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.journalNoteTypes(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/documents/document-types", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "The document types a new document can have",
		description = "The active document types Lifecare offers for a new document on the errand's insats, in Lifecare's order. Blanketter are left out: they are filled in field by field in Lifecare's own editor.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict - the errand has no Lifecare insats yet", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareDocumentType>> listDocumentTypes(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.documentTypes(municipalityId, namespace, errandId));
	}

	@PostMapping(path = "/documents/journal-notes", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Write a new journalanteckning on the errand's insats",
		description = "Writes a journalanteckning on the errand's Lifecare insats, filling in Lifecare's own proposal. The rubrik defaults to the note type's name, skrivskydd to the note type's default. 400 when the note type is not an active one in Lifecare.",
		responses = {
			@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Created", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict - the errand has no Lifecare insats yet", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "422", description = "Lifecare refused the note", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<LifecareRecord> createJournalNote(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final CreateLifecareJournalNoteRequest request) {

		final var record = service.createJournalNote(municipalityId, namespace, errandId, request);
		return created(fromPath(RECORD_LOCATION.formatted("journal-notes")).buildAndExpand(municipalityId, namespace, errandId, record.getId()).toUri())
			.body(record);
	}

	@PostMapping(path = "/documents/documents", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Write a new document on the errand's insats",
		description = "Writes a document on the errand's Lifecare insats, filling in Lifecare's own proposal. The rubrik defaults to the document type's name, skrivskydd to the type's default; the date is only taken when the type allows it. 400 when the document type is not a writable one in Lifecare.",
		responses = {
			@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Created", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict - the errand has no Lifecare insats yet", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "422", description = "Lifecare refused the document", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<LifecareRecord> createDocument(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final CreateLifecareDocumentRequest request) {

		final var record = service.createDocument(municipalityId, namespace, errandId, request);
		return created(fromPath(RECORD_LOCATION.formatted("documents")).buildAndExpand(municipalityId, namespace, errandId, record.getId()).toUri())
			.body(record);
	}

	@GetMapping(path = "/documents/journal-notes/{id}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read a journalanteckning with its body",
		description = "One of the applicant's journalanteckningar with its body and whether it may still be edited. 404 when the record is not in the applicant's Lifecare record.",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<LifecareRecordContent> readJournalNote(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The Lifecare record id") @Positive @PathVariable final Integer id) {

		return ok(service.readJournalNote(municipalityId, namespace, errandId, id));
	}

	@PutMapping(path = "/documents/journal-notes/{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Save an edit to a journalanteckning",
		description = "Saves the body, date, time and optionally skrivskydd onto Lifecare's own record. 404 when the record is not in the applicant's Lifecare record; 409 when it is finalised or locked in Lifecare.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict - the record is finalised in Lifecare", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "422", description = "Lifecare refused the edit", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<LifecareRecordContent> updateJournalNote(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The Lifecare record id") @Positive @PathVariable final Integer id,
		@Valid @NotNull @RequestBody final UpdateLifecareRecordRequest request) {

		return ok(service.updateJournalNote(municipalityId, namespace, errandId, id, request));
	}

	@GetMapping(path = "/documents/documents/{id}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read a document with its body",
		description = "One of the applicant's documents with its body and whether it may still be edited. 404 when the record is not in the applicant's Lifecare record.",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<LifecareRecordContent> readDocument(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The Lifecare record id") @Positive @PathVariable final Integer id) {

		return ok(service.readDocument(municipalityId, namespace, errandId, id));
	}

	@GetMapping(path = "/documents/documents/{id}/pdf", produces = APPLICATION_PDF_VALUE)
	@Operation(summary = "Read a document as a PDF file",
		description = "One of the applicant's documents as the PDF file Lifecare holds for it, for sending it on as a bilaga. The file is read from Lifecare's FC API, matched on the document's title and date. 404 when the record is not in the applicant's Lifecare record or Lifecare holds no PDF for it (only PDF-backed documents have one); 409 when several Lifecare documents share its title and date. The read is logged.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", content = @Content(mediaType = APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "409", description = "Conflict - the document cannot be told apart from another in Lifecare", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<byte[]> readDocumentPdf(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The Lifecare record id") @Positive @PathVariable final Integer id) {

		return ok().header(CONTENT_TYPE, APPLICATION_PDF_VALUE).body(service.readDocumentPdf(municipalityId, namespace, errandId, id));
	}

	@PutMapping(path = "/documents/documents/{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Save an edit to a document",
		description = "Saves the body, date, time and optionally skrivskydd onto Lifecare's own record. 404 when the record is not in the applicant's Lifecare record; 409 when it is finalised or locked in Lifecare.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict - the record is finalised in Lifecare", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "422", description = "Lifecare refused the edit", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<LifecareRecordContent> updateDocument(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The Lifecare record id") @Positive @PathVariable final Integer id,
		@Valid @NotNull @RequestBody final UpdateLifecareRecordRequest request) {

		return ok(service.updateDocument(municipalityId, namespace, errandId, id, request));
	}
}
