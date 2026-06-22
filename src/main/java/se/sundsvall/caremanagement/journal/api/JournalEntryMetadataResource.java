package se.sundsvall.caremanagement.journal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.journal.api.model.JournalEntryMetadata;
import se.sundsvall.caremanagement.journal.service.JournalEntryTypes;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/journal-entries")
@Tag(name = "Journal", description = "Journalanteckningar (case-journal entries) attached to an errand")
class JournalEntryMetadataResource {

	@GetMapping(path = "/metadata", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the journal metadata — the provisional catalogue of selectable journal entry types")
	ResponseEntity<JournalEntryMetadata> metadata(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace) {

		return ok(JournalEntryTypes.metadata());
	}
}
