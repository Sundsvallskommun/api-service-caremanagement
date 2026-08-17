package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceCalculationService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands")
@Tag(name = FinancialAssistanceApiTags.CALCULATION, description = FinancialAssistanceApiTags.CALCULATION_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceCalculationResource {

	private final FinancialAssistanceCalculationService calculationService;

	FinancialAssistanceCalculationResource(final FinancialAssistanceCalculationService calculationService) {
		this.calculationService = calculationService;
	}

	@PostMapping(path = "/financial-assistance/calculation/prepare", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Prepare the calculation (no Lifecare write)",
		description = "Reports whether this month's classified incomes cover every income type the previous calculation had (informationComplete + missingIncomeTypes), records the income warnings on the errand as a single Decision(RECOMMENDATION), and reflects completeness in the errand status (SUPPLEMENT_REQUESTED ⇄ AWAITING_DECISION). Does NOT create a calculation in Lifecare — the financial assistance process calls this each daily loop. Use /commit after a decision to create it in Lifecare.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<CalculationResponse> prepareCalculation(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final CalculationRequest request) {

		return ok(calculationService.prepareCalculation(municipalityId, namespace, request));
	}

	@PostMapping(path = "/financial-assistance/calculation/commit", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create the calculation in Lifecare (after decision)",
		description = "Builds the calculation from the classified incomes and creates it in Lifecare FamilyCare, returning the created calculation id. Called once a decision is taken — never during the daily SSBTEK loop.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<CalculationResponse> commitCalculation(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final CalculationRequest request) {

		return ok(calculationService.commitCalculation(municipalityId, namespace, request));
	}

	@PostMapping(path = "/financial-assistance/calculation/from-application", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create the calculation in Lifecare straight from the application (new application)",
		description = "Builds the calculation from the data the citizen declared in the application — incomes resolved to FamilyCare types by name, expenses and household from the same feeder the renewal path uses — and creates it in Lifecare FamilyCare in one shot, returning the created calculation id. No SSBTEK, no daily loop, no caseworker draft. Used by the new application process.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<CalculationResponse> commitFromApplication(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final CalculationRequest request) {

		return ok(calculationService.commitFromApplication(municipalityId, namespace, request));
	}

	@GetMapping(path = "/financial-assistance/{errandId}/calculation/draft", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the draft calculation",
		description = "The FamilyCare income rows the financial assistance process prepared (not yet created in Lifecare) for the caseworker to review and edit before a decision. 404 when no draft exists yet.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<CalculationDraft> getDraft(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(calculationService.getDraft(municipalityId, namespace, errandId));
	}

	@PatchMapping(path = "/financial-assistance/{errandId}/calculation/draft/header", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Edit the draft header (caseworker)",
		description = "Set the norm, the calculation date window (from/to/date) and the custom household size (common costs). 404 when no draft exists.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<CalculationDraft> patchDraftHeader(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final NormHeaderInput input) {

		return ok(calculationService.patchDraftHeader(municipalityId, namespace, errandId, input));
	}
}
