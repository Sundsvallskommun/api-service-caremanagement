package se.sundsvall.caremanagement.statushistory.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.statushistory.api.model.StatusHistoryEntry;
import se.sundsvall.caremanagement.statushistory.service.StatusHistoryService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/status-history")
@Tag(name = "Status History", description = "Status transition log for an errand")
class StatusHistoryResource {

	private final StatusHistoryService service;

	StatusHistoryResource(final StatusHistoryService service) {
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List status transitions for errand")
	ResponseEntity<List<StatusHistoryEntry>> list(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.listForErrand(errandId));
	}
}
