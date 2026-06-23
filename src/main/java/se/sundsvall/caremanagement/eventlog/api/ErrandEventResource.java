package se.sundsvall.caremanagement.eventlog.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.eventlog.api.model.ErrandEvent;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/{errandId}/events")
@Tag(name = "Event Log", description = "Who/what/when activity log for an errand — every read and write, with the acting user")
class ErrandEventResource {

	private final ErrandEventService service;

	ErrandEventResource(final ErrandEventService service) {
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List activity events for an errand (newest first)", description = "Optionally filter by action (READ/CREATE/UPDATE/DELETE) and/or actor (the X-Sent-By value, e.g. an AD account).")
	ResponseEntity<List<ErrandEvent>> list(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@RequestParam(required = false) final String action,
		@RequestParam(required = false) final String actor) {

		return ok(service.listForErrand(errandId, action, actor));
	}
}
