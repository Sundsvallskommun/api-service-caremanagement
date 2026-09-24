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
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeResponse;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceFinalizeService;
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
@Tag(name = FinancialAssistanceApiTags.FINALIZE, description = FinancialAssistanceApiTags.FINALIZE_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceFinalizeResource {

	private final FinancialAssistanceFinalizeService finalizeService;

	FinancialAssistanceFinalizeResource(final FinancialAssistanceFinalizeService finalizeService) {
		this.finalizeService = finalizeService;
	}

	@PostMapping(path = "/financial-assistance/{errandId}/finalize", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Besluta och utbetala — finalize the errand (caseworker)",
		description = "Records the caseworker's decision as a PAYMENT decision on the errand, stores the chosen communication channels and household-size flag, creates one PENDING_REGISTRATION payment per decided payment, and correlates PaymentDecisionReceived to the process (paymentDecision APPROVED for BIFALL/DELAVSLAG, REJECTED otherwise) — which sets GRANTED/REJECTED and polls the payment. A BIFALL/DELAVSLAG requires the normberäkning to be saved in Lifecare first: Draken saves it and sets lifecareCalculationId on the errand (PATCH .../financial-assistance/{errandId}/data) before the decision, otherwise 409; an AVSLAG does not. Does not send anything to the applicant: the response echoes the communication channels for the frontend to act on. Requires the errand in AWAITING_DECISION with all three sections approved and an identified caller (X-Sent-By); a second finalize is rejected with 409. Process failures and payees not yet in Lifecare are reported in the response, not as errors. The Lifecare writes (decision, payments, bevakningar, journal, documents) are Draken's BFF's, reported back through the lifecare-result endpoints.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409",
				description = "Conflict - wrong status, sections not approved, already finalized, or a granting decision without lifecareCalculationId",
				content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<FinalizeResponse> finalize(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final FinalizeRequest request) {

		// The decider is the authenticated caseworker (X-Sent-By), not a client-supplied field, so it can't be spoofed.
		final var decidedBy = Optional.ofNullable(Identifier.get()).map(Identifier::getValue).orElse(null);
		return ok(finalizeService.finalize(municipalityId, namespace, errandId, request, decidedBy));
	}
}
