package se.sundsvall.caremanagement.rpa.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.rpa.api.model.RpaTaskRequest;
import se.sundsvall.caremanagement.rpa.service.RpaService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.ResponseEntity.accepted;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

/**
 * Inbound trigger for RPA tasks — lets the EB process (an Operaton topic worker) and Draken enqueue a UiPath robot job
 * for an errand. The actual Lifecare GUI work happens out of band; this endpoint only drops the queue item and returns
 * {@code 202 Accepted}.
 */
@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/rpa-tasks")
@Tag(name = "RPA", description = "Enqueue UiPath RPA tasks (Lifecare write-backs / supplement fetches) for an errand")
class RpaResource {

	private final RpaService service;

	RpaResource(final RpaService service) {
		this.service = service;
	}

	@PostMapping(consumes = "application/json", produces = "application/json")
	@Operation(summary = "Enqueue an RPA task for an errand")
	@ApiResponse(responseCode = "202", description = "Accepted - the RPA task was enqueued")
	@ResponseStatus(ACCEPTED)
	ResponseEntity<Void> enqueue(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final RpaTaskRequest request) {

		service.enqueue(municipalityId, namespace, errandId, request.getAction(), request.getParameters());
		return accepted().build();
	}
}
