package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentCreated;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentOptions;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentStatus;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRegisteredPayment;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.ErrandLifecarePaymentService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentRegistrationService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/lifecare")
@Tag(name = FinancialAssistanceApiTags.LIFECARE, description = FinancialAssistanceApiTags.LIFECARE_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceLifecarePaymentResource {

	private final ErrandLifecarePaymentService paymentService;
	private final LifecarePaymentRegistrationService registrationService;

	FinancialAssistanceLifecarePaymentResource(final ErrandLifecarePaymentService paymentService, final LifecarePaymentRegistrationService registrationService) {
		this.paymentService = paymentService;
		this.registrationService = registrationService;
	}

	@GetMapping(path = "/payment-options", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "The betalsätt, betalningsmottagare, ändamål, saldon and months an utbetalning on the errand's insats can use, read from Lifecare",
		description = """
			Everything the utbetalning form needs, read from Lifecare's underlag for a new utbetalning on the errand's insats, \
			plus a proposal to start from (Lifecare's date and first open month, what is left on the saldon, and the payee the \
			latest standing utbetalning went to). The read is logged on the errand.""",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "The errand has no Lifecare insats yet", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<LifecarePaymentOptions> readPaymentOptions(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(paymentService.paymentOptions(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/payment-status", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Whether the utbetalning for the errand's application month has been registered, read from Lifecare",
		description = """
			Looks for a standing (not makulerad) utbetalning on the errand's insats concerning the errand's application month. \
			Never fails on Lifecare: unavailable is true when the errand has no application month or insats, or Lifecare \
			could not be read.""",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<LifecarePaymentStatus> readPaymentStatus(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(paymentService.paymentStatus(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/payments", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "The utbetalningar registered on the errand's insats, read from Lifecare",
		description = "Lifecare's latest utbetalningar on the insats, newest payment date first. The read is logged on the errand.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "The errand has no Lifecare insats yet", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareRegisteredPayment>> readPayments(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(paymentService.registeredPayments(municipalityId, namespace, errandId));
	}

	@PostMapping(path = "/payments", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Register an utbetalning on the errand's insats in Lifecare",
		description = """
			Registers the utbetalning with Lifecare's Payment/Create and links it to the errand. Refused with the reason (422) \
			when it cannot be made safely: no amount, a betalsätt or month Lifecare does not offer, no ändamål among several \
			konteringsrader, other than one saldo, a saldo that does not cover the amount, a blocking maximum amount, a payee \
			Lifecare does not have, or no hushåll on the payment date. 409 when an identical standing utbetalning (amount, \
			month, date and account) is already registered. 502 when Lifecare did not answer: whether it paid is then \
			unknown, so the caseworker checks Lifecare before trying again. Never retried.""",
		responses = {
			@ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "422", description = "Unprocessable Content", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<LifecarePaymentCreated> registerPayment(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @RequestBody final LifecarePaymentRequest request) {

		return status(CREATED).body(registrationService.register(municipalityId, namespace, errandId, request));
	}

	@PostMapping(path = "/payees", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add a betalningsmottagare in Lifecare",
		description = """
			Adds a betalningsmottagare for the person the errand's insats belongs to. An active payee with the same betalsätt, \
			account and clearing is returned instead of being created a second time. 400 when the betalsätt is not in use on \
			the insats.""",
		responses = {
			@ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "The errand has no Lifecare insats yet", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<LifecarePayee> createPayee(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @RequestBody final LifecarePayeeRequest request) {

		return status(CREATED).body(paymentService.createPayee(municipalityId, namespace, errandId, request));
	}
}
