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
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.api.model.WarningCount;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceWarningService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands")
@Tag(name = FinancialAssistanceApiTags.WARNINGS, description = FinancialAssistanceApiTags.WARNINGS_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceWarningResource {

	private final FinancialAssistanceWarningService service;

	FinancialAssistanceWarningResource(final FinancialAssistanceWarningService service) {
		this.service = service;
	}

	@PostMapping(path = "/financial-assistance/{errandId}/warnings", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create a financial assistance income warning on an errand",
		description = "Creates an acknowledgeable income warning directly on the errand — the careM temp stage, with no Lifecare round-trip. The warning is created OPEN; use the PATCH endpoint to acknowledge or close it.",
		responses = {
			@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Warning> createWarning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@Valid @NotNull @RequestBody final CreateWarningRequest request) {

		final var warning = service.createWarning(municipalityId, namespace, errandId, request);
		return created(fromPath("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/warnings/{warningId}")
			.buildAndExpand(municipalityId, namespace, errandId, warning.getId()).toUri())
			.body(warning);
	}

	@GetMapping(path = "/financial-assistance/{errandId}/warnings", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the financial assistance income warnings on an errand",
		description = "The acknowledgeable income warnings the caseworker reviews — unhandled incomes, significant changes, and income types still missing from SSBTEK. The daily prepare step reconciles them.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<Warning>> listWarnings(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId) {

		return ok(service.listWarnings(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/financial-assistance/{errandId}/warnings/count", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Count the active financial assistance income warnings on an errand",
		description = "How many warnings are still active (OPEN or ACKNOWLEDGED) — closed ones are not counted. Not recorded in the event log.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<WarningCount> countWarnings(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId) {

		return ok(new WarningCount(service.countActiveWarnings(municipalityId, namespace, errandId)));
	}

	@PatchMapping(path = "/financial-assistance/{errandId}/warnings/{warningId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the status of a financial assistance income warning",
		description = "A caseworker acknowledges (seen, kept on record), closes (dismisses), or re-opens a warning to OPEN (undoing an earlier acknowledge/close).",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Warning> updateWarning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String warningId,
		@Parameter(description = "The target status", schema = @Schema(allowableValues = {
			"OPEN", "ACKNOWLEDGED", "CLOSED"
		})) @RequestParam final String status) {

		return ok(service.updateWarning(municipalityId, namespace, errandId, warningId, status));
	}
}
