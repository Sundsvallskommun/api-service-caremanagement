package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceDraftRowService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

/**
 * Per-row caseworker edits on the draft calculation. Each touches only the caseworker value / note / soft-delete; the
 * process columns are owned by the daily prepare. 404 when the errand or row is missing in this namespace/municipality.
 */
@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands")
@Tag(name = FinancialAssistanceApiTags.DRAFT_ROWS, description = FinancialAssistanceApiTags.DRAFT_ROWS_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceDraftRowResource {

	private final FinancialAssistanceDraftRowService service;

	FinancialAssistanceDraftRowResource(final FinancialAssistanceDraftRowService service) {
		this.service = service;
	}

	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/incomes", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add an income row to the draft (caseworker)", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormIncomeRow> addDraftIncome(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final NormIncomeInput input) {

		return ok(service.addDraftIncome(municipalityId, namespace, errandId, input));
	}

	@PatchMapping(path = "/financial-assistance/{errandId}/calculation/draft/incomes/{rowId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the caseworker value / note on an income row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormIncomeRow> patchDraftIncome(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String rowId,
		@Valid @NotNull @RequestBody final NormIncomeInput input) {

		return ok(service.patchDraftIncome(municipalityId, namespace, errandId, rowId, input));
	}

	@DeleteMapping(path = "/financial-assistance/{errandId}/calculation/draft/incomes/{rowId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Soft-delete an income row (excluded from the calculation, survives the daily refresh)", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormIncomeRow> deleteDraftIncome(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String rowId) {

		return ok(service.setDraftIncomeDeleted(municipalityId, namespace, errandId, rowId, true));
	}

	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/incomes/{rowId}/restore", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Restore a soft-deleted income row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormIncomeRow> restoreDraftIncome(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String rowId) {

		return ok(service.setDraftIncomeDeleted(municipalityId, namespace, errandId, rowId, false));
	}

	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/expenses", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add an expense row to the draft (caseworker)", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormExpenseRow> addDraftExpense(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final NormExpenseInput input) {

		return ok(service.addDraftExpense(municipalityId, namespace, errandId, input));
	}

	@PatchMapping(path = "/financial-assistance/{errandId}/calculation/draft/expenses/{rowId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the caseworker value / note on an expense row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormExpenseRow> patchDraftExpense(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String rowId,
		@Valid @NotNull @RequestBody final NormExpenseInput input) {

		return ok(service.patchDraftExpense(municipalityId, namespace, errandId, rowId, input));
	}

	@DeleteMapping(path = "/financial-assistance/{errandId}/calculation/draft/expenses/{rowId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Soft-delete an expense row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormExpenseRow> deleteDraftExpense(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String rowId) {

		return ok(service.setDraftExpenseDeleted(municipalityId, namespace, errandId, rowId, true));
	}

	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/expenses/{rowId}/restore", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Restore a soft-deleted expense row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormExpenseRow> restoreDraftExpense(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String rowId) {

		return ok(service.setDraftExpenseDeleted(municipalityId, namespace, errandId, rowId, false));
	}

	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/persons", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add a person row to the draft (caseworker)", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormPersonRow> addDraftPerson(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final NormPersonInput input) {

		return ok(service.addDraftPerson(municipalityId, namespace, errandId, input));
	}

	@PatchMapping(path = "/financial-assistance/{errandId}/calculation/draft/persons/{rowId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the caseworker days / note on a person row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormPersonRow> patchDraftPerson(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String rowId,
		@Valid @NotNull @RequestBody final NormPersonInput input) {

		return ok(service.patchDraftPerson(municipalityId, namespace, errandId, rowId, input));
	}

	@DeleteMapping(path = "/financial-assistance/{errandId}/calculation/draft/persons/{rowId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Soft-delete a person row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormPersonRow> deleteDraftPerson(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String rowId) {

		return ok(service.setDraftPersonDeleted(municipalityId, namespace, errandId, rowId, true));
	}

	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/persons/{rowId}/restore", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Restore a soft-deleted person row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormPersonRow> restoreDraftPerson(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String rowId) {

		return ok(service.setDraftPersonDeleted(municipalityId, namespace, errandId, rowId, false));
	}
}
