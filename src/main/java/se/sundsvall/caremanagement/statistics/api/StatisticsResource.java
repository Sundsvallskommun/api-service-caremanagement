package se.sundsvall.caremanagement.statistics.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.statistics.api.model.StatisticsResponse;
import se.sundsvall.caremanagement.statistics.service.StatisticsService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/statistics")
@Tag(name = "Statistics", description = "Read-only errand statistics for the caseworker interface")
class StatisticsResource {

	private final StatisticsService service;

	StatisticsResource(final StatisticsService service) {
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Errand statistics", description = "Aggregates the number of errands per status and per assigned user, optionally filtered by errand type and creation date.")
	ResponseEntity<StatisticsResponse> getStatistics(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "Filter on errand type (module). Omit for statistics across all types.") @RequestParam(required = false) final String typeSlug,
		@Parameter(description = "Count only errands created at or after this point in time (ISO-8601).") @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) final OffsetDateTime from,
		@Parameter(description = "Count only errands created at or before this point in time (ISO-8601).") @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) final OffsetDateTime to) {

		return ok(service.compute(municipalityId, namespace, typeSlug, from, to));
	}
}
