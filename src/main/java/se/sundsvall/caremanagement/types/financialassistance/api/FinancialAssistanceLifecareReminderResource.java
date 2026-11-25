package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminder;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderOptions;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareReminderRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareReminderService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare/reminders")
@Tag(name = FinancialAssistanceApiTags.LIFECARE, description = FinancialAssistanceApiTags.LIFECARE_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "409", description = "Conflict - the errand has no Lifecare insats yet", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceLifecareReminderResource {

	private final LifecareReminderService service;

	FinancialAssistanceLifecareReminderResource(final LifecareReminderService service) {
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the bevakningar on the errand's insats",
		description = "The bevakningar on the errand's Lifecare insats (and on the beslut and aktualiseringar under it), read live from Lifecare, soonest first. The personnummer on the Lifecare rows is left out. The read is logged in the errand's access log.",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<List<LifecareReminder>> listReminders(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.list(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/options", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "The priorities and statuses a bevakning can have",
		description = "Lifecare's own priority and status lists for a bevakning on the errand's insats, and the ones Lifecare proposes for a new one.",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<LifecareReminderOptions> readReminderOptions(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.options(municipalityId, namespace, errandId));
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Add a bevakning on the errand's insats",
		description = "Creates a manual bevakning on the errand's Lifecare insats, for the applicant, bevakad av the insats's caseworker. The date may not be in the past (Swedish time). Not idempotent. 400 when Lifecare offers no manual insats bevakning, the insats has no caseworker, or the priority or status is not one of Lifecare's.",
		responses = @ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true))
	ResponseEntity<Void> createReminder(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final LifecareReminderRequest request) {

		service.create(municipalityId, namespace, errandId, request);
		return status(CREATED).build();
	}

	@PutMapping(path = "/{reminderId}", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Change a bevakning on the errand's insats",
		description = "Changes the date, text, priority or status of a bevakning (also how it is marked done). A changed date may not be in the past; an unchanged one may. 404 when Lifecare does not list the bevakning on this errand's insats.",
		responses = @ApiResponse(responseCode = "204", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<Void> updateReminder(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The Lifecare reminder id") @Positive @PathVariable final Integer reminderId,
		@Valid @NotNull @RequestBody final LifecareReminderRequest request) {

		service.update(municipalityId, namespace, errandId, reminderId, request);
		return noContent().build();
	}

	@DeleteMapping(path = "/{reminderId}", produces = ALL_VALUE)
	@Operation(summary = "Remove a bevakning from the errand's insats",
		description = "Removes a bevakning in Lifecare. 404 when Lifecare does not list the bevakning on this errand's insats.",
		responses = @ApiResponse(responseCode = "204", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<Void> removeReminder(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The Lifecare reminder id") @Positive @PathVariable final Integer reminderId) {

		service.remove(municipalityId, namespace, errandId, reminderId);
		return noContent().build();
	}
}
