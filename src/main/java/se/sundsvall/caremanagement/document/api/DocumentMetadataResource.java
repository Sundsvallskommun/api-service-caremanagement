package se.sundsvall.caremanagement.document.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.document.api.model.DocumentMetadata;
import se.sundsvall.caremanagement.document.service.DocumentMetadataService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/documents")
@Tag(name = "Documents", description = "Dokument (formal case documents) attached to an errand")
class DocumentMetadataResource {

	private final DocumentMetadataService service;

	DocumentMetadataResource(final DocumentMetadataService service) {
		this.service = service;
	}

	@GetMapping(path = "/metadata", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the document metadata — the catalogue of selectable document types (seeded DOCUMENT_TYPE lookups for the namespace, or a built-in provisional set as fallback)")
	ResponseEntity<DocumentMetadata> metadata(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace) {

		return ok(service.metadata(municipalityId, namespace));
	}
}
