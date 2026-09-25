package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareJobStimulusPeriodRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.ErrandLifecareJobStimulusService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare/job-stimulus-periods")
@Tag(name = FinancialAssistanceApiTags.LIFECARE, description = FinancialAssistanceApiTags.LIFECARE_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "409", description = "The errand has no Lifecare insats yet", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceLifecareJobStimulusResource {

	private final ErrandLifecareJobStimulusService service;

	FinancialAssistanceLifecareJobStimulusResource(final ErrandLifecareJobStimulusService service) {
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "The jobbstimulans periods on the errand's insats, read from Lifecare",
		description = "The sökandes and the medsökandes periods, each marked with whose it is. Periods marked for removal are left out. The read is logged on the errand.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<List<LifecareJobStimulusPeriod>> readJobStimulusPeriods(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.periods(municipalityId, namespace, errandId));
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add a jobbstimulans period for the sökande on the errand's insats in Lifecare",
		description = """
			Lifecare's save replaces every period the person has, so the current set is sent back whole with the new one. \
			The end defaults to Lifecare's two-year rule. Refused (422) for a household with a medsökande. Answers with every \
			period after the save.""",
		responses = {
			@ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "422", description = "Unprocessable Content", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareJobStimulusPeriod>> addJobStimulusPeriod(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @RequestBody final LifecareJobStimulusPeriodRequest request) {

		return status(CREATED).body(service.addPeriod(municipalityId, namespace, errandId, request));
	}
}
