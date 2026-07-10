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
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovalRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceApprovalService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.support.Identifier;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands")
@Tag(name = FinancialAssistanceApiTags.APPROVALS, description = FinancialAssistanceApiTags.APPROVALS_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceApprovalResource {

	private final FinancialAssistanceApprovalService service;

	FinancialAssistanceApprovalResource(final FinancialAssistanceApprovalService service) {
		this.service = service;
	}

	@GetMapping(path = "/financial-assistance/{errandId}/sections/approvals", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the section approvals on an errand",
		description = "The caseworker approval state of the three financial assistance view sections (CALCULATION = calculation, PAYMENT = payment, DECISION = decision). Always returns all three — a section never approved is present with approved=false. The same object is embedded in the errand view.",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<SectionApprovals> getSectionApprovals(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.getSectionApprovals(municipalityId, namespace, errandId));
	}

	@PatchMapping(path = "/financial-assistance/{errandId}/sections/{section}/approval", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set a section's approval (caseworker)",
		description = "A caseworker verifies one of the financial assistance view sections (CALCULATION / PAYMENT / DECISION) as approved, or withdraws an earlier approval. Approving stamps who/when; withdrawing clears them.",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<SectionApproval> setSectionApproval(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Parameter(description = "The section to approve", schema = @Schema(allowableValues = {
			"CALCULATION", "PAYMENT", "DECISION"
		})) @PathVariable final String section,
		@Valid @NotNull @RequestBody final SectionApprovalRequest request) {

		// The approver is the authenticated caseworker (X-Sent-By), not a client-supplied field, so it can't be spoofed.
		final var approvedBy = Optional.ofNullable(Identifier.get()).map(Identifier::getValue).orElse(null);
		return ok(service.setSectionApproval(municipalityId, namespace, errandId, section, request.getApproved(), approvedBy));
	}
}
