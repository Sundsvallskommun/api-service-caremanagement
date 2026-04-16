package se.sundsvall.caremanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.api.model.Attachment;
import se.sundsvall.caremanagement.service.ErrandAttachmentService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/attachments")
@Tag(name = "Errand attachments", description = "Errand attachment operations")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class ErrandAttachmentResource {

	private final ErrandAttachmentService service;

	ErrandAttachmentResource(final ErrandAttachmentService service) {
		this.service = service;
	}

	@PostMapping(consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create attachment", description = "Uploads a new attachment for the errand", responses = {
		@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<Void> createAttachment(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id") @ValidUuid @PathVariable final String errandId,
		@NotNull @RequestPart("file") final MultipartFile file) {

		final var attachmentId = service.createAttachment(municipalityId, namespace, errandId, file);
		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/attachments/{attachmentId}")
			.buildAndExpand(municipalityId, namespace, errandId, attachmentId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List attachments", description = "Fetches all attachment metadata for the errand", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<Attachment>> readAttachments(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id") @ValidUuid @PathVariable final String errandId) {

		return ok(service.readAttachments(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/{attachmentId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read attachment metadata", description = "Fetches the attachment metadata", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Attachment> readAttachment(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "attachmentId", description = "Attachment id") @ValidUuid @PathVariable final String attachmentId) {

		return ok(service.readAttachment(municipalityId, namespace, errandId, attachmentId));
	}

	@GetMapping(path = "/{attachmentId}/file", produces = ALL_VALUE)
	@Operation(summary = "Download attachment file", description = "Streams the attachment file content", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	void streamAttachmentFile(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "attachmentId", description = "Attachment id") @ValidUuid @PathVariable final String attachmentId,
		final HttpServletResponse response) {

		service.streamAttachmentFile(municipalityId, namespace, errandId, attachmentId, response);
	}

	@DeleteMapping(path = "/{attachmentId}", produces = ALL_VALUE)
	@Operation(summary = "Delete attachment", description = "Deletes the attachment", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteAttachment(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", description = "Namespace", example = "MY_NAMESPACE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId", description = "Errand id") @ValidUuid @PathVariable final String errandId,
		@Parameter(name = "attachmentId", description = "Attachment id") @ValidUuid @PathVariable final String attachmentId) {

		service.deleteAttachment(municipalityId, namespace, errandId, attachmentId);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}
}
