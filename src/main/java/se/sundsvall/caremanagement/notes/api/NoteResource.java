package se.sundsvall.caremanagement.notes.api;

import io.swagger.v3.oas.annotations.Operation;
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
import se.sundsvall.caremanagement.notes.api.model.CreateNote;
import se.sundsvall.caremanagement.notes.api.model.Note;
import se.sundsvall.caremanagement.notes.api.model.NoteCount;
import se.sundsvall.caremanagement.notes.api.model.UpdateNote;
import se.sundsvall.caremanagement.notes.service.NoteService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/notes")
@Tag(name = "Notes", description = "Notes attached to an errand")
class NoteResource {

	private final NoteService service;

	NoteResource(final NoteService service) {
		this.service = service;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Add note to errand")
	ResponseEntity<Void> createNote(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final CreateNote request) {

		final var noteId = service.add(municipalityId, namespace, errandId, request);
		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/notes/{noteId}")
			.buildAndExpand(municipalityId, namespace, errandId, noteId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List notes for errand")
	ResponseEntity<List<Note>> list(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.listForErrand(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/count", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Count the notes on an errand", description = "How many notes are attached to the errand. Not recorded in the event log.")
	ResponseEntity<NoteCount> count(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(new NoteCount(service.countForErrand(municipalityId, namespace, errandId)));
	}

	@GetMapping(path = "/{noteId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read note")
	ResponseEntity<Note> read(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String noteId) {

		return ok(service.read(municipalityId, namespace, errandId, noteId));
	}

	@PatchMapping(path = "/{noteId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Update note")
	ResponseEntity<Note> update(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String noteId,
		@Valid @NotNull @RequestBody final UpdateNote request) {

		return ok(service.update(municipalityId, namespace, errandId, noteId, request));
	}

	@DeleteMapping(path = "/{noteId}", produces = ALL_VALUE)
	@Operation(summary = "Delete note")
	ResponseEntity<Void> delete(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String noteId) {

		service.delete(municipalityId, namespace, errandId, noteId);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}
}
