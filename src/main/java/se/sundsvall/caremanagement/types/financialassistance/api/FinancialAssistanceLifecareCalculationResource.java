package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareCalculationSaveRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareCalculationView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningPreviousCalculation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningRowInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningTypes;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.ErrandLifecareCalculationService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.ErrandNormberakningService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static java.lang.Boolean.TRUE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

/**
 * The errand's normberäkning towards Lifecare ProfessionalWeb: the beräkning saved in Lifecare (read, save, PDF), the
 * previous beräkning, and the Normberäkning tab's rows, which are careM's draft until the beräkning is first saved in
 * Lifecare and Lifecare's beräkning after that.
 */
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
class FinancialAssistanceLifecareCalculationResource {

	private static final String SECTION_REGEXP = "^(persons|incomes|expenses)$";
	private static final String SECTION_MESSAGE = "must be persons, incomes or expenses";
	private static final String ROW_ID_REGEXP = "^[A-Za-z0-9-]{1,64}$";
	private static final String ROW_ID_MESSAGE = "not a valid row id";

	private final ErrandLifecareCalculationService calculationService;
	private final ErrandNormberakningService normberakningService;

	FinancialAssistanceLifecareCalculationResource(final ErrandLifecareCalculationService calculationService, final ErrandNormberakningService normberakningService) {
		this.calculationService = calculationService;
		this.normberakningService = normberakningService;
	}

	@GetMapping(path = "/calculation", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the errand's normberäkning as it stands in Lifecare",
		description = "Lifecare's own beräkning linked to the errand (lifecareCalculationId) with Lifecare's summering. 204 while none is saved in Lifecare. The read is logged in the errand's access log.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "204", description = "No beräkning saved in Lifecare yet")
		})
	ResponseEntity<LifecareCalculationView> readCalculation(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return calculationService.read(municipalityId, namespace, errandId)
			.map(ResponseEntity::ok)
			.orElseGet(() -> noContent().build());
	}

	@PostMapping(path = "/calculation", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Save the errand's normberäkning in Lifecare",
		description = "The first save creates the beräkning in Lifecare from careM's draft (Calculation/Create) and links it to the errand; after that it saves Lifecare's own beräkning (Calculation/Update). finalize saves it as slutlig, after which Lifecare allows no change. 422 when the draft cannot be sent as it stands (no period, an included co-applicant, no one included, a type Lifecare does not know) or when Lifecare refuses. A create Lifecare did not answer is never retried: 502 then tells the caseworker to check Lifecare before saving again.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "422", description = "Unprocessable Content", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<LifecareCalculationView> saveCalculation(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final LifecareCalculationSaveRequest request) {

		return ok(calculationService.save(municipalityId, namespace, errandId, TRUE.equals(request.getFinalize())));
	}

	@GetMapping(path = "/calculation/pdf", produces = APPLICATION_PDF_VALUE)
	@Operation(summary = "Read the errand's normberäkning as Lifecare prints it (PDF)",
		description = "Lifecare's print of the linked beräkning (RenderPdf/PrintContainer). 404 while none is saved in Lifecare. The read is logged.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", content = @Content(mediaType = APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary")))
		})
	ResponseEntity<byte[]> readCalculationPdf(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok().header(CONTENT_TYPE, APPLICATION_PDF_VALUE).body(calculationService.pdf(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/normberakning/previous", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the beräkning preceding the errand's own period from Lifecare",
		description = "Picked from the insats's beräkningar in Lifecare: the latest period before the errand's own (its saved beräkning's, or the draft's), a slutlig one first. Read-only; members go without personnummer. 204 when there is none.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "204", description = "No previous beräkning")
		})
	ResponseEntity<NormberakningPreviousCalculation> readPreviousCalculation(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return calculationService.readPrevious(municipalityId, namespace, errandId)
			.map(ResponseEntity::ok)
			.orElseGet(() -> noContent().build());
	}

	@GetMapping(path = "/normberakning", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the Normberäkning tab's rows",
		description = "careM's draft (source CAREM, persons with their personnummer) until the beräkning is saved in Lifecare, and Lifecare's beräkning (source LIFECARE) after that. 404 when neither exists.",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<NormberakningDraft> readNormberakning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(normberakningService.readDraft(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/normberakning/types", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the norms, income and cost types a new row can have",
		description = "Lifecare's own catalogues once the beräkning is saved there; careM's catalogue before, with Lifecare's norms for the insats (best-effort).",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<NormberakningTypes> readNormberakningTypes(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(normberakningService.types(municipalityId, namespace, errandId));
	}

	@PatchMapping(path = "/normberakning/header", consumes = APPLICATION_JSON_VALUE)
	@Operation(summary = "Change the normberäkning header",
		description = "In careM's draft: norm, dates and household size. In Lifecare: the norm and the household size (Annan hushållsstorlek, saved for the household's coming beräkningar); the period is Lifecare's and is refused with 422.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful Operation"),
			@ApiResponse(responseCode = "422", description = "Unprocessable Content", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> updateNormberakningHeader(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final NormHeaderInput input) {

		normberakningService.updateHeader(municipalityId, namespace, errandId, input);
		return noContent().build();
	}

	@PostMapping(path = "/normberakning/{section}", consumes = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add a caseworker row to a normberäkning section",
		description = "In careM's draft any section. In Lifecare an income (a type already on the beräkning is refused) or an utgift / levnadskostnad i övrigt by Lifecare code; persons are added in Lifecare itself (422).",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful Operation"),
			@ApiResponse(responseCode = "422", description = "Unprocessable Content", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> addNormberakningRow(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The section", schema = @Schema(allowableValues = {
			"persons", "incomes", "expenses"
		})) @Pattern(regexp = SECTION_REGEXP, message = SECTION_MESSAGE) @PathVariable final String section,
		@Valid @NotNull @RequestBody final NormberakningRowInput input) {

		normberakningService.addRow(municipalityId, namespace, errandId, section, input);
		return noContent().build();
	}

	@PatchMapping(path = "/normberakning/{section}/{rowId}", consumes = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the caseworker's values on a normberäkning row",
		description = "rowId is careM's row id in the draft, and in Lifecare the income code, the expense id (E-3, S-7, E-3-2) or the member's personKey. A member's days and normintervall make Lifecare count its amount again.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful Operation"),
			@ApiResponse(responseCode = "422", description = "Unprocessable Content", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> updateNormberakningRow(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The section", schema = @Schema(allowableValues = {
			"persons", "incomes", "expenses"
		})) @Pattern(regexp = SECTION_REGEXP, message = SECTION_MESSAGE) @PathVariable final String section,
		@Parameter(description = "The row") @Pattern(regexp = ROW_ID_REGEXP, message = ROW_ID_MESSAGE) @PathVariable final String rowId,
		@Valid @NotNull @RequestBody final NormberakningRowInput input) {

		normberakningService.updateRow(municipalityId, namespace, errandId, section, rowId, input);
		return noContent().build();
	}

	@DeleteMapping(path = "/normberakning/{section}/{rowId}")
	@Operation(summary = "Remove a normberäkning row",
		description = "A soft delete in careM's draft; in Lifecare the row's amounts go to 0 and Lifecare drops it. Members are not removed from a beräkning in Lifecare (422).",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful Operation"),
			@ApiResponse(responseCode = "422", description = "Unprocessable Content", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> deleteNormberakningRow(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The section", schema = @Schema(allowableValues = {
			"persons", "incomes", "expenses"
		})) @Pattern(regexp = SECTION_REGEXP, message = SECTION_MESSAGE) @PathVariable final String section,
		@Parameter(description = "The row") @Pattern(regexp = ROW_ID_REGEXP, message = ROW_ID_MESSAGE) @PathVariable final String rowId) {

		normberakningService.deleteRow(municipalityId, namespace, errandId, section, rowId);
		return noContent().build();
	}

	@PostMapping(path = "/normberakning/{section}/{rowId}/restore")
	@Operation(summary = "Restore a soft-deleted row of careM's draft",
		description = "careM's draft only: a beräkning in Lifecare has no soft delete (422).",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful Operation"),
			@ApiResponse(responseCode = "422", description = "Unprocessable Content", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> restoreNormberakningRow(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The section", schema = @Schema(allowableValues = {
			"persons", "incomes", "expenses"
		})) @Pattern(regexp = SECTION_REGEXP, message = SECTION_MESSAGE) @PathVariable final String section,
		@Parameter(description = "The row") @Pattern(regexp = ROW_ID_REGEXP, message = ROW_ID_MESSAGE) @PathVariable final String rowId) {

		normberakningService.restoreRow(municipalityId, namespace, errandId, section, rowId);
		return noContent().build();
	}
}
