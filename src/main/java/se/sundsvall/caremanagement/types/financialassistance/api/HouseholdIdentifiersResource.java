package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.HouseholdIdentifiers;
import se.sundsvall.caremanagement.types.financialassistance.service.HouseholdIdentifiersService;
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
@RequestMapping("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}")
@Tag(name = "Financial Assistance · Household identifiers",
	description = "The errand number and the household's personal numbers, served on demand to the process's beredning step "
		+ "(prepare-income-basis) so they never become process variables. Every read lands in the errand's event log.")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class HouseholdIdentifiersResource {

	private final HouseholdIdentifiersService service;

	HouseholdIdentifiersResource(final HouseholdIdentifiersService service) {
		this.service = service;
	}

	@GetMapping(path = "/household-identifiers", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the errand number and the household's personal numbers",
		description = "Read per run by the process's prepare-income-basis worker: resolves the errand number and the applicant's (and "
			+ "any co-applicant's) personal number for the lookups.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<HouseholdIdentifiers> getHouseholdIdentifiers(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.get(municipalityId, namespace, errandId));
	}

	/** The name from the retired RPA integration, served until the process worker has moved to household-identifiers. */
	@GetMapping(path = "/rpa-context", produces = APPLICATION_JSON_VALUE)
	@Operation(deprecated = true,
		summary = "Read the errand number and the household's personal numbers (old path)",
		description = "DEPRECATED - the old name of GET .../household-identifiers, kept only until the process worker calls the new path.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<HouseholdIdentifiers> getRpaContext(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.get(municipalityId, namespace, errandId));
	}
}
