package se.sundsvall.caremanagement.eventlog.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.eventlog.api.model.ErrandEventCount;
import se.sundsvall.caremanagement.eventlog.api.model.ErrandEventEntry;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
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
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/events")
@Tag(name = "Event Log", description = "Who/what/when activity log for an errand — every read and write, with the acting user")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class ErrandEventResource {

	private final ErrandEventService service;

	ErrandEventResource(final ErrandEventService service) {
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List activity events for an errand (newest first)",
		description = "Optionally filter by action (READ/CREATE/UPDATE/DELETE), actor (the X-Sent-By value, e.g. an AD account) "
			+ "and source (HTTP access log or EVENT change log). Set includeReads=false for a clean 'what changed' timeline without the read noise.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true)
		})
	ResponseEntity<List<ErrandEventEntry>> list(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@RequestParam(required = false) final String action,
		@RequestParam(required = false) final String actor,
		@RequestParam(required = false) final String source,
		@RequestParam(defaultValue = "true") final boolean includeReads) {

		return ok(service.listForErrand(municipalityId, namespace, errandId, action, actor, source, includeReads));
	}

	@GetMapping(path = "/count", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Count activity events for an errand",
		description = "Honours the same filters as the list. With the defaults this is the total event count; set includeReads=false "
			+ "for the 'what changed' count without the read noise. Not recorded in the event log.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true)
		})
	ResponseEntity<ErrandEventCount> count(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@RequestParam(required = false) final String action,
		@RequestParam(required = false) final String actor,
		@RequestParam(required = false) final String source,
		@RequestParam(defaultValue = "true") final boolean includeReads) {

		return ok(new ErrandEventCount(service.countForErrand(municipalityId, namespace, errandId, action, actor, source, includeReads)));
	}
}
