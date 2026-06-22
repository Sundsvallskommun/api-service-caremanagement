package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Bevakning;
import se.sundsvall.caremanagement.types.financialassistance.api.model.BevakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.BevakningService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/bevakningar")
@Tag(name = "Financial Assistance Bevakningar",
	description = "Ekonomiskt bistånd (EB) bevakningar — date-bound watch/reminder objects on an errand, with full CRUD. Unlike the income warnings they carry no acknowledge lifecycle.")
class BevakningResource {

	private final BevakningService service;

	BevakningResource(final BevakningService service) {
		this.service = service;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create a bevakning on an errand",
		description = "Creates a date-bound bevakning on the errand. Returns the created bevakning and its Location.",
		responses = {
			@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Bevakning> createBevakning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final BevakningRequest request) {

		final var bevakning = service.create(municipalityId, namespace, errandId, request);
		return created(fromPath("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/bevakningar/{bevakningId}")
			.buildAndExpand(municipalityId, namespace, errandId, bevakning.getId()).toUri())
			.body(bevakning);
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the bevakningar on an errand", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<List<Bevakning>> listBevakningar(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.list(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/{bevakningId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read a bevakning on an errand", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Bevakning> getBevakning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String bevakningId) {

		return ok(service.get(municipalityId, namespace, errandId, bevakningId));
	}

	@PutMapping(path = "/{bevakningId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Replace a bevakning on an errand", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Bevakning> updateBevakning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String bevakningId,
		@Valid @NotNull @RequestBody final BevakningRequest request) {

		return ok(service.update(municipalityId, namespace, errandId, bevakningId, request));
	}

	@DeleteMapping(path = "/{bevakningId}")
	@Operation(summary = "Delete a bevakning on an errand", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteBevakning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String bevakningId) {

		service.delete(municipalityId, namespace, errandId, bevakningId);
		return noContent().build();
	}
}
