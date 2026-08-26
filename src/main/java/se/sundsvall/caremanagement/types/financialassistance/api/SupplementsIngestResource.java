package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareSupplements;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SupplementsIngestResult;
import se.sundsvall.caremanagement.types.financialassistance.service.SupplementsIngestService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

/**
 * The RPA robot's single delivery endpoint. After a {@code FETCH_SUPPLEMENTS} queue item, the robot reads bevakningar,
 * the document list (journal notes included) and jobbstimulans out of Lifecare Professional Web and POSTs one near-raw
 * envelope here; CareManagement routes, maps and upserts, and answers with a per-item receipt. Partial success by
 * design — a broken item is reported {@code FAILED} without failing the batch, and re-deliveries are idempotent.
 */
@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare-supplements")
@Tag(name = "Financial Assistance · Lifecare supplements",
	description = "The RPA robot's single delivery endpoint — one near-raw Lifecare dump per errand (bevakningar, document list, jobbstimulans), routed and upserted by CareManagement, answered with a per-item receipt.")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class SupplementsIngestResource {

	private final SupplementsIngestService service;

	SupplementsIngestResource(final SupplementsIngestService service) {
		this.service = service;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Deliver a Lifecare supplements dump for an errand",
		description = "Ingests one near-raw dump of the Lifecare Professional Web responses for the errand's client. "
			+ "Sections: reminders[] (upserted as LIFECARE-sourced monitorings per reminderId), documents[] (routed on documentType — 3 to journal, 0 to documents — and upserted per row id), "
			+ "jobStimulus (the errand's full period set is replaced). Omit a section to leave that area untouched; deliver it empty to mean 'fetched, nothing there'. "
			+ "Returns 200 with a per-item receipt even when individual items fail — only a structurally unreadable envelope is rejected.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Delivery processed — see the per-item receipt", useReturnTypeSchema = true)
		})
	ResponseEntity<SupplementsIngestResult> deliverSupplements(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@NotNull @RequestBody final LifecareSupplements supplements) {

		return ok(service.ingest(municipalityId, namespace, errandId, supplements));
	}
}
