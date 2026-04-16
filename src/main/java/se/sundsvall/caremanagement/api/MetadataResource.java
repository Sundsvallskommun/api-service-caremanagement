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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
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
import se.sundsvall.caremanagement.api.model.Lookup;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;
import se.sundsvall.caremanagement.service.MetadataService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
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
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.CATEGORY;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.CONTACT_REASON;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.ROLE;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.STATUS;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.TYPE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/metadata")
@Tag(name = "Metadata", description = "Metadata (lookup) operations")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class MetadataResource {

	private static final String CATEGORIES = "/categories";
	private static final String STATUSES = "/statuses";
	private static final String TYPES = "/types";
	private static final String ROLES = "/roles";
	private static final String CONTACT_REASONS = "/contact-reasons";

	private final MetadataService service;

	MetadataResource(final MetadataService service) {
		this.service = service;
	}

	// --- CATEGORIES ---

	@PostMapping(path = CATEGORIES, consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create category", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
	})
	@Validated(OnCreate.class)
	ResponseEntity<Void> createCategory(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final Lookup lookup) {
		return createLookup(municipalityId, namespace, CATEGORY, "categories", lookup);
	}

	@GetMapping(path = CATEGORIES, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List categories", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<Lookup>> readCategories(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace) {
		return ok(service.readAll(municipalityId, namespace, CATEGORY));
	}

	@GetMapping(path = CATEGORIES + "/{name}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read category", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Lookup> readCategory(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "name") @NotBlank @PathVariable final String name) {
		return ok(service.read(municipalityId, namespace, CATEGORY, name));
	}

	@PatchMapping(path = CATEGORIES + "/{name}", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Update category", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateCategory(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name,
		@Valid @NotNull @RequestBody final Lookup lookup) {
		service.update(municipalityId, namespace, CATEGORY, name, lookup);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@DeleteMapping(path = CATEGORIES + "/{name}", produces = ALL_VALUE)
	@Operation(summary = "Delete category", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteCategory(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name) {
		service.delete(municipalityId, namespace, CATEGORY, name);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	// --- STATUSES ---

	@PostMapping(path = STATUSES, consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create status", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
	})
	@Validated(OnCreate.class)
	ResponseEntity<Void> createStatus(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final Lookup lookup) {
		return createLookup(municipalityId, namespace, STATUS, "statuses", lookup);
	}

	@GetMapping(path = STATUSES, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List statuses", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<Lookup>> readStatuses(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace) {
		return ok(service.readAll(municipalityId, namespace, STATUS));
	}

	@GetMapping(path = STATUSES + "/{name}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read status", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Lookup> readStatus(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name) {
		return ok(service.read(municipalityId, namespace, STATUS, name));
	}

	@PatchMapping(path = STATUSES + "/{name}", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Update status", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateStatus(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name,
		@Valid @NotNull @RequestBody final Lookup lookup) {
		service.update(municipalityId, namespace, STATUS, name, lookup);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@DeleteMapping(path = STATUSES + "/{name}", produces = ALL_VALUE)
	@Operation(summary = "Delete status", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteStatus(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name) {
		service.delete(municipalityId, namespace, STATUS, name);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	// --- TYPES ---

	@PostMapping(path = TYPES, consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create type", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
	})
	@Validated(OnCreate.class)
	ResponseEntity<Void> createType(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final Lookup lookup) {
		return createLookup(municipalityId, namespace, TYPE, "types", lookup);
	}

	@GetMapping(path = TYPES, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List types", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<Lookup>> readTypes(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace) {
		return ok(service.readAll(municipalityId, namespace, TYPE));
	}

	@GetMapping(path = TYPES + "/{name}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read type", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Lookup> readType(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name) {
		return ok(service.read(municipalityId, namespace, TYPE, name));
	}

	@PatchMapping(path = TYPES + "/{name}", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Update type", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateType(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name,
		@Valid @NotNull @RequestBody final Lookup lookup) {
		service.update(municipalityId, namespace, TYPE, name, lookup);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@DeleteMapping(path = TYPES + "/{name}", produces = ALL_VALUE)
	@Operation(summary = "Delete type", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteType(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name) {
		service.delete(municipalityId, namespace, TYPE, name);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	// --- ROLES ---

	@PostMapping(path = ROLES, consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create role", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
	})
	@Validated(OnCreate.class)
	ResponseEntity<Void> createRole(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final Lookup lookup) {
		return createLookup(municipalityId, namespace, ROLE, "roles", lookup);
	}

	@GetMapping(path = ROLES, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List roles", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<Lookup>> readRoles(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace) {
		return ok(service.readAll(municipalityId, namespace, ROLE));
	}

	@GetMapping(path = ROLES + "/{name}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read role", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Lookup> readRole(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name) {
		return ok(service.read(municipalityId, namespace, ROLE, name));
	}

	@PatchMapping(path = ROLES + "/{name}", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Update role", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateRole(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name,
		@Valid @NotNull @RequestBody final Lookup lookup) {
		service.update(municipalityId, namespace, ROLE, name, lookup);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@DeleteMapping(path = ROLES + "/{name}", produces = ALL_VALUE)
	@Operation(summary = "Delete role", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteRole(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name) {
		service.delete(municipalityId, namespace, ROLE, name);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	// --- CONTACT REASONS ---

	@PostMapping(path = CONTACT_REASONS, consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create contact reason", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
	})
	@Validated(OnCreate.class)
	ResponseEntity<Void> createContactReason(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final Lookup lookup) {
		return createLookup(municipalityId, namespace, CONTACT_REASON, "contact-reasons", lookup);
	}

	@GetMapping(path = CONTACT_REASONS, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List contact reasons", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<Lookup>> readContactReasons(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace) {
		return ok(service.readAll(municipalityId, namespace, CONTACT_REASON));
	}

	@GetMapping(path = CONTACT_REASONS + "/{name}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read contact reason", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Lookup> readContactReason(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name) {
		return ok(service.read(municipalityId, namespace, CONTACT_REASON, name));
	}

	@PatchMapping(path = CONTACT_REASONS + "/{name}", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Update contact reason", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateContactReason(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name,
		@Valid @NotNull @RequestBody final Lookup lookup) {
		service.update(municipalityId, namespace, CONTACT_REASON, name, lookup);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@DeleteMapping(path = CONTACT_REASONS + "/{name}", produces = ALL_VALUE)
	@Operation(summary = "Delete contact reason", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteContactReason(@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@NotBlank @PathVariable final String name) {
		service.delete(municipalityId, namespace, CONTACT_REASON, name);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	// --- helper ---

	private ResponseEntity<Void> createLookup(final String municipalityId, final String namespace, final LookupKind kind, final String pathSegment, final Lookup lookup) {
		final var name = service.create(municipalityId, namespace, kind, lookup);
		final URI location = fromPath("/{municipalityId}/{namespace}/metadata/" + pathSegment + "/{name}")
			.buildAndExpand(municipalityId, namespace, name).toUri();
		return created(location).header(CONTENT_TYPE, ALL_VALUE).build();
	}
}
