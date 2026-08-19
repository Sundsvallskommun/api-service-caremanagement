package se.sundsvall.caremanagement.conversation.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.conversation.api.model.CreateMessage;
import se.sundsvall.caremanagement.conversation.api.model.MarkMessagesRead;
import se.sundsvall.caremanagement.conversation.api.model.Message;
import se.sundsvall.caremanagement.conversation.api.model.UnreadCount;
import se.sundsvall.caremanagement.conversation.service.MessageReadService;
import se.sundsvall.caremanagement.conversation.service.MessageService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.support.Identifier;

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
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/messages")
@Tag(name = "Conversation", description = "Messages between caseworker and applicant on an errand")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class MessageResource {

	private final MessageService service;
	private final MessageReadService readService;

	MessageResource(final MessageService service, final MessageReadService readService) {
		this.service = service;
		this.readService = readService;
	}

	@PostMapping(consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Post a message on the errand",
		description = "Multipart request. The 'message' part carries the message (JSON) including the sender ('direction' and 'author'); the optional 'attachments' part carries the files to attach (any type).",
		responses = {
			@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
		})
	ResponseEntity<Void> createMessage(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestPart("message") final CreateMessage message,
		@RequestPart(value = "attachments", required = false) final List<MultipartFile> attachments) {

		final var messageId = service.post(municipalityId, namespace, errandId, message, attachments);
		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/messages/{messageId}")
			.buildAndExpand(municipalityId, namespace, errandId, messageId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the errand's messages (chronological)", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true)
	})
	ResponseEntity<List<Message>> list(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.listForErrand(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/unread-count", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Count unread messages for the calling side",
		description = "Returns how many messages addressed to the caller are unread. The caller's side is derived from the '" + Identifier.HEADER_NAME
			+ "' header (type=adAccount → caseworker, type=partyId → applicant). A read-only poll that is not recorded in the event log.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true)
		})
	ResponseEntity<UnreadCount> unreadCount(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(name = Identifier.HEADER_NAME,
			description = "Caller identity (type=adAccount → caseworker, type=partyId → applicant)",
			example = "joe001doe; type=adAccount") @RequestHeader(Identifier.HEADER_NAME) final String xSentBy) {

		return ok(new UnreadCount(readService.unreadCount(municipalityId, namespace, errandId, Identifier.parse(xSentBy))));
	}

	@PostMapping(path = "/read", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Mark messages as read for the calling side",
		description = "Marks the given messages as read for the caller's side (derived from the '" + Identifier.HEADER_NAME
			+ "' header). Idempotent, and not recorded in the event log.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful operation")
		})
	ResponseEntity<Void> markRead(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(name = Identifier.HEADER_NAME, description = "Caller identity (type=adAccount → caseworker, type=partyId → applicant)", example = "joe001doe; type=adAccount") @RequestHeader(Identifier.HEADER_NAME) final String xSentBy,
		@Valid @NotNull @RequestBody final MarkMessagesRead request) {

		readService.markRead(municipalityId, namespace, errandId, Identifier.parse(xSentBy), request.messageIds());
		return noContent().build();
	}

	@GetMapping(path = "/{messageId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read a message", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Message> read(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String messageId) {

		return ok(service.read(municipalityId, namespace, errandId, messageId));
	}

	@GetMapping(path = "/{messageId}/attachments/{attachmentId}/file", produces = ALL_VALUE)
	@Operation(summary = "Download a message attachment", description = "Streams the attachment file content", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	void streamAttachmentFile(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String messageId,
		@Parameter(name = "attachmentId", description = "Attachment id") @ValidUuid @PathVariable final String attachmentId,
		final HttpServletResponse response) {

		service.streamAttachmentFile(municipalityId, namespace, errandId, messageId, attachmentId, response);
	}
}
