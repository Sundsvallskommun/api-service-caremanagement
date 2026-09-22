package se.sundsvall.caremanagement.eventlog.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.eventlog.api.model.ActorEventLog;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

/**
 * Logguppföljning: what one user has done, across every errand.
 *
 * <p>
 * Sits at the namespace root rather than under the per-errand events route because the question is not about an
 * errand — the errand is the answer, not the input. Verksamhetens regelverk (revision 2026-09-22) asks for it in as
 * many words: <em>”Måste kunna söka upp loggar på en användare, inte bara per ärende”</em>.
 * </p>
 *
 * <p>
 * Not itself recorded in the event log: the errand interceptor only matches {@code /errands/**}. That is a gap worth
 * naming rather than hiding — reading someone's follow-up log is itself a disclosure, and who may run this is an open
 * question with verksamheten (D2 in the 2026-09-22 question round). Once answered, this endpoint needs both an
 * authorisation rule and a log of its own.
 * </p>
 */
@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/events")
@Tag(name = "Event Log", description = "Who/what/when activity log for an errand — every read and write, with the acting user")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class ActorEventResource {

	private final ErrandEventService service;

	ActorEventResource(final ErrandEventService service) {
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List one actor's activity across all errands (newest first)",
		description = "Logguppföljning for a single user: every errand they read or changed in this namespace, newest first. "
			+ "Optionally filter by action (READ/CREATE/UPDATE/DELETE), source (HTTP access log or EVENT change log) and a "
			+ "[from, to) time window. Reads are included — a follow-up is mostly about what someone looked at. The listing is "
			+ "capped; compare the returned events against 'total' to see whether it was truncated.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true)
		})
	ResponseEntity<ActorEventLog> listForActor(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @RequestParam final String actor,
		@RequestParam(required = false) final String action,
		@RequestParam(required = false) final String source,
		@RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) final OffsetDateTime from,
		@RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) final OffsetDateTime to) {

		return ok(service.listForActor(municipalityId, namespace, actor, action, source, from, to));
	}
}
