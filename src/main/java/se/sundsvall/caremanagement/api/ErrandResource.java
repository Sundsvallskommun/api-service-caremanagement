package se.sundsvall.caremanagement.api;

import com.turkraft.springfilter.boot.Filter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
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
import se.sundsvall.caremanagement.api.model.Errand;
import se.sundsvall.caremanagement.api.model.PatchErrand;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.service.ErrandService;
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
@RequestMapping("/{municipalityId}/{namespace}/errands")
@Tag(name = "Errands", description = "Errand operations")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class ErrandResource {

	private final ErrandService service;

	ErrandResource(final ErrandService service) {
		this.service = service;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create errand", description = "Creates a new errand", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
	})
	@Validated(OnCreate.class)
	ResponseEntity<Void> createErrand(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final Errand errand) {

		final var errandId = service.createErrand(municipalityId, namespace, errand);
		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}")
			.buildAndExpand(municipalityId, namespace, errandId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(path = "/{errandId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read errand", description = "Fetches the errand that matches the provided id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Errand> readErrand(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id") @ValidUuid @PathVariable final String errandId) {

		return ok(service.readErrand(municipalityId, namespace, errandId));
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Find errands", description = "Query for errands with optional spring-filter expression", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Page<Errand>> findErrands(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "Syntax description: [spring-filter](https://github.com/turkraft/spring-filter/blob/85730f950a5f8623159cc0eb4d737555f9382bb7/README.md#syntax)",
			example = "status:'NEW' and created>'2024-01-01T00:00:00.000+01:00'",
			schema = @Schema(implementation = String.class)) @Nullable @Filter final Specification<ErrandEntity> filter,
		@ParameterObject @PageableDefault(sort = "touched", direction = Sort.Direction.DESC) final Pageable pageable) {

		return ok(service.findErrands(municipalityId, namespace, filter, pageable));
	}

	@PatchMapping(path = "/{errandId}", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Update errand", description = "Patches the errand with the supplied attributes", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateErrand(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id") @ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final PatchErrand patch) {

		service.updateErrand(municipalityId, namespace, errandId, patch);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@DeleteMapping(path = "/{errandId}", produces = ALL_VALUE)
	@Operation(summary = "Delete errand", description = "Deletes the errand", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteErrand(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id") @ValidUuid @PathVariable final String errandId) {

		service.deleteErrand(municipalityId, namespace, errandId);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}
}
