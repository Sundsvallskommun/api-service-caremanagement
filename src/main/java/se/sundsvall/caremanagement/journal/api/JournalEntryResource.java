package se.sundsvall.caremanagement.journal.api;

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
import se.sundsvall.caremanagement.journal.api.model.CreateJournalEntry;
import se.sundsvall.caremanagement.journal.api.model.JournalEntry;
import se.sundsvall.caremanagement.journal.api.model.LockJournalEntry;
import se.sundsvall.caremanagement.journal.api.model.UpdateJournalEntry;
import se.sundsvall.caremanagement.journal.service.JournalEntryService;
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
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/journal-entries")
@Tag(name = "Journal", description = "Journalanteckningar (case-journal entries) attached to an errand")
class JournalEntryResource {

	private final JournalEntryService service;

	JournalEntryResource(final JournalEntryService service) {
		this.service = service;
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Add a journal entry to an errand")
	ResponseEntity<Void> add(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final CreateJournalEntry request) {

		final var journalEntryId = service.add(municipalityId, namespace, errandId, request);
		return created(fromPath("/{municipalityId}/{namespace}/errands/{errandId}/journal-entries/{journalEntryId}")
			.buildAndExpand(municipalityId, namespace, errandId, journalEntryId).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List journal entries for an errand (most recent first)")
	ResponseEntity<List<JournalEntry>> list(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.listForErrand(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/{journalEntryId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read a journal entry")
	ResponseEntity<JournalEntry> read(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String journalEntryId) {

		return ok(service.read(municipalityId, namespace, errandId, journalEntryId));
	}

	@PatchMapping(path = "/{journalEntryId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Update a journal entry (only while WORKING; a LOCKED entry returns 409)")
	ResponseEntity<JournalEntry> update(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String journalEntryId,
		@Valid @NotNull @RequestBody final UpdateJournalEntry request) {

		return ok(service.update(municipalityId, namespace, errandId, journalEntryId, request));
	}

	@PostMapping(path = "/{journalEntryId}/lock", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Lock a journal entry (skrivskydd) — it becomes an immutable upprättad handling")
	ResponseEntity<JournalEntry> lock(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String journalEntryId,
		@Valid @RequestBody(required = false) final LockJournalEntry request) {

		return ok(service.lock(municipalityId, namespace, errandId, journalEntryId, request));
	}

	@DeleteMapping(path = "/{journalEntryId}", produces = ALL_VALUE)
	@Operation(summary = "Delete a journal entry (only while WORKING; a LOCKED entry returns 409)")
	ResponseEntity<Void> delete(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String journalEntryId) {

		service.delete(municipalityId, namespace, errandId, journalEntryId);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}
}
