package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.DecisionProposal;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentProposal;
import se.sundsvall.caremanagement.types.financialassistance.service.DecisionProposalService;
import se.sundsvall.caremanagement.types.financialassistance.service.PaymentProposalService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands")
@Tag(name = FinancialAssistanceApiTags.PROPOSALS, description = FinancialAssistanceApiTags.PROPOSALS_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceProposalResource {

	private final DecisionProposalService decisionProposalService;
	private final PaymentProposalService paymentProposalService;

	FinancialAssistanceProposalResource(final DecisionProposalService decisionProposalService, final PaymentProposalService paymentProposalService) {
		this.decisionProposalService = decisionProposalService;
		this.paymentProposalService = paymentProposalService;
	}

	@GetMapping(path = "/financial-assistance/{errandId}/decision-proposal", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the decision proposal (beslutsförslag)",
		description = """
			The proposed decision for the DECISION tab, derived on every read from the calculation draft and the applicant's most recent \
			Lifecare decision — never stored. Outcome rule: estimatedAmount <= 0 → AVSLAG; > 0 and every expense fully approved → BIFALL; \
			> 0 and any expense approved below its applied amount → DELAVSLAG. The amount is an estimate: normSum (the applicant's previous \
			Lifecare calculation's norm) + expenseSum + specialExpenseSum − incomeSum from the draft; when no previous norm is known the \
			outcome and amount are null and 'explanation' says why. Period = the calculation's period; reason = the previous decision's \
			orsak (with a list of alternatives); phraseText = 'Bifall månad med/utan barn' on an approved outcome. Reading also reconciles \
			the DECISION-section warnings (previous decision was förskott på förmån; one per expense not approved in full) and returns \
			them. 404 when the errand has no calculation draft.""",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<DecisionProposal> getDecisionProposal(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(decisionProposalService.get(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/financial-assistance/{errandId}/payment-proposal", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the payment proposal (utbetalningsförslag)",
		description = """
			The proposed payment for the PAYMENT tab, derived on every read from the calculation draft, the application and the applicant's \
			previous Lifecare payments — never stored. Returned as a 'payments' list with one entry so the frontend can split it into several \
			payments. Payment date = the 27th of the calculation's month, moved to the Friday before when the 27th is a Saturday (26th) or \
			Sunday (25th); public holidays are not considered. Amount = the whole estimated bistånd (same estimate as the decision proposal; \
			null with an 'explanation' when no norm is known). Payee = the account the applicant stated in the application when \
			paymentSameAsPrevious=false, else the previous Lifecare payment's payee ('payeeSource' says which); 'payeeOptions' lists every \
			distinct payee on the applicant's payments in the last 12 months (FamilyCare has no payee register). accountingCode (kontering) \
			is always null — not available from FamilyCare. Reading also reconciles the PAYMENT-section warnings (a medsökande exists → check \
			for delad utbetalning) and returns them. 404 when the errand has no calculation draft.""",
		responses = @ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true))
	ResponseEntity<PaymentProposal> getPaymentProposal(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(paymentProposalService.get(municipalityId, namespace, errandId));
	}
}
