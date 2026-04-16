package se.sundsvall.caremanagement.api;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.api.model.StakeholderParameter;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;
import se.sundsvall.caremanagement.service.StakeholderParameterService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
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
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/stakeholders/{stakeholderId}/parameters")
@Tag(name = "Stakeholder parameters", description = "Stakeholder parameter operations")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class StakeholderParameterResource {

	private final StakeholderParameterService service;

	StakeholderParameterResource(final StakeholderParameterService service) {
		this.service = service;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create stakeholder parameter", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
	})
	@Validated(OnCreate.class)
	ResponseEntity<Void> createParameter(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "stakeholderId") @ValidUuid @PathVariable final String stakeholderId,
		@Valid @NotNull @RequestBody final StakeholderParameter parameter) {

		final var id = service.create(municipalityId, namespace, errandId, stakeholderId, parameter);
		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/stakeholders/{stakeholderId}/parameters/{parameterId}")
			.buildAndExpand(municipalityId, namespace, errandId, stakeholderId, id).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List stakeholder parameters", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<StakeholderParameter>> readParameters(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "stakeholderId") @ValidUuid @PathVariable final String stakeholderId) {

		return ok(service.readAll(municipalityId, namespace, errandId, stakeholderId));
	}

	@GetMapping(path = "/{parameterId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read stakeholder parameter", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<StakeholderParameter> readParameter(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "stakeholderId") @ValidUuid @PathVariable final String stakeholderId,
		@Parameter(name = "parameterId") @PathVariable final Long parameterId) {

		return ok(service.read(municipalityId, namespace, errandId, stakeholderId, parameterId));
	}

	@PatchMapping(path = "/{parameterId}", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Update stakeholder parameter", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateParameter(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "stakeholderId") @ValidUuid @PathVariable final String stakeholderId,
		@Parameter(name = "parameterId") @PathVariable final Long parameterId,
		@Valid @NotNull @RequestBody final StakeholderParameter parameter) {

		service.update(municipalityId, namespace, errandId, stakeholderId, parameterId, parameter);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@DeleteMapping(path = "/{parameterId}", produces = ALL_VALUE)
	@Operation(summary = "Delete stakeholder parameter", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteParameter(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "stakeholderId") @ValidUuid @PathVariable final String stakeholderId,
		@Parameter(name = "parameterId") @PathVariable final Long parameterId) {

		service.delete(municipalityId, namespace, errandId, stakeholderId, parameterId);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}
}
