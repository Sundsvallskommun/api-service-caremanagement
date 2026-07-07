package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareCalculation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDocument;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands")
@Tag(name = FinancialAssistanceApiTags.LIFECARE, description = FinancialAssistanceApiTags.LIFECARE_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceLifecareResource {

	private final FinancialAssistanceService service;

	FinancialAssistanceLifecareResource(final FinancialAssistanceService service) {
		this.service = service;
	}

	@GetMapping(path = "/financial-assistance/calculations", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the applicant's Lifecare calculations",
		description = "The applicant's Lifecare calculations over the period — the full breakdown (header sums plus income, expense, special-expense and household-member rows) the frontend renders straight from Lifecare. The applicant is identified by partyId (resolved to a personnummer via the citizen service). The period defaults to the last 24 months up to today when from/to are omitted.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareCalculation>> listCalculations(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The applicant's partyId (personId GUID)") @ValidUuid @RequestParam final String partyId,
		@Parameter(description = "Inclusive start of the listing period (ISO date). Defaults to 24 months before 'to'.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@Parameter(description = "Inclusive end of the listing period (ISO date). Defaults to today.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to) {

		return ok(service.listCalculations(municipalityId, partyId, from, to));
	}

	@GetMapping(path = "/financial-assistance/decisions", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the applicant's Lifecare decisions",
		description = "The applicant's Lifecare decisions over the period — the decision header plus the persons each concerned — served straight from Lifecare. The applicant is identified by partyId (resolved to a personnummer via the citizen service). The period defaults to the last 24 months up to today when from/to are omitted.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareDecision>> listDecisions(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The applicant's partyId (personId GUID)") @ValidUuid @RequestParam final String partyId,
		@Parameter(description = "Inclusive start of the listing period (ISO date). Defaults to 24 months before 'to'.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@Parameter(description = "Inclusive end of the listing period (ISO date). Defaults to today.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to) {

		return ok(service.listDecisions(municipalityId, partyId, from, to));
	}

	@GetMapping(path = "/financial-assistance/documents", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the applicant's Lifecare documents (metadata)",
		description = "The applicant's Lifecare documents over the period — metadata only (id, title, date, type, owner). Fetch a single document's content via the /documents/{documentId}/content endpoint. The applicant is identified by partyId (resolved to a personnummer via the citizen service). The period defaults to the last 24 months up to today when from/to are omitted.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareDocument>> listDocuments(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The applicant's partyId (personId GUID)") @ValidUuid @RequestParam final String partyId,
		@Parameter(description = "Inclusive start of the listing period (ISO date). Defaults to 24 months before 'to'.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@Parameter(description = "Inclusive end of the listing period (ISO date). Defaults to today.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to) {

		return ok(service.listDocuments(municipalityId, partyId, from, to));
	}

	@GetMapping(path = "/financial-assistance/documents/{documentId}/content", produces = APPLICATION_PDF_VALUE)
	@Operation(summary = "Read a Lifecare document's content (PDF)",
		description = "Streams a single Lifecare document's content (the generated PDF) straight from Lifecare. caremanagement only forwards the bytes. The document must belong to the given applicant (partyId) — a document id not in that applicant's list for the period yields 404, so a caller cannot read another applicant's document by its Lifecare id. 502 when Lifecare is unreachable; Lifecare answers 404 when the document has no generated PDF (content exists only for PDF-backed document types).",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", content = @Content(mediaType = APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<byte[]> readDocumentContent(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The Lifecare document id") @PathVariable final String documentId,
		@Parameter(description = "The applicant's partyId (personId GUID) that owns the document") @ValidUuid @RequestParam final String partyId,
		@Parameter(description = "Inclusive start of the ownership-check period (ISO date). Defaults to 24 months before 'to'.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@Parameter(description = "Inclusive end of the ownership-check period (ISO date). Defaults to today.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to) {

		return ok().header(CONTENT_TYPE, APPLICATION_PDF_VALUE).body(service.readDocumentContent(municipalityId, partyId, documentId, from, to));
	}
}
