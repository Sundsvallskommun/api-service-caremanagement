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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.AppliedSsbtekChanges;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SsbtekChanges;
import se.sundsvall.caremanagement.types.financialassistance.service.CalculationSyncService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/calculation/ssbtek-changes")
@Tag(name = FinancialAssistanceApiTags.CALCULATION, description = FinancialAssistanceApiTags.CALCULATION_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceCalculationSyncResource {

	private final CalculationSyncService calculationSyncService;

	FinancialAssistanceCalculationSyncResource(final CalculationSyncService calculationSyncService) {
		this.calculationSyncService = calculationSyncService;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read where the normberäkning in Lifecare no longer matches SSBTEK",
		description = "Compares the latest SSBTEK amounts the daily prepare recorded with the errand's linked normberäkning, read live from Lifecare. Each change says whether Draken may write it without asking (AUTO: the calculation still holds what the system last wrote there) or must show it to the caseworker (CONFIRM, with a reason). Nothing is written, in Lifecare or in careM. 404 when no normberäkning is linked (lifecareCalculationId), after finalize, or when the calculation is not found in the calculation period; 502 when Lifecare cannot be read.",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<SsbtekChanges> getChanges(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(calculationSyncService.changes(municipalityId, namespace, errandId));
	}

	@PostMapping(path = "/applied", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Acknowledge SSBTEK changes written into the normberäkning in Lifecare",
		description = "Draken's BFF reports the incomes it wrote into the linked normberäkning (amount null = taken out). careM records them as what the system last wrote, so the next SSBTEK change to the same income is again recognised as untouched by a caseworker, and brings the SSBTEK_CALCULATION_DIFF warnings up to date (best-effort). 409 when calculationId is not the errand's lifecareCalculationId.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict - not the linked calculation", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> applied(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final AppliedSsbtekChanges request) {

		calculationSyncService.applied(municipalityId, namespace, errandId, request);
		return noContent().build();
	}
}
