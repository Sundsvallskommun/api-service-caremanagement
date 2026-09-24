package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeLifecareResult;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeOption;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.service.PayeeService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/payees")
@Tag(name = "Financial Assistance · Payees",
	description = "The betalningsmottagare selectable for an errand's payment form: the payees seen on the applicant's Lifecare payments in "
		+ "the last 12 months, plus the ones added by hand here when the history does not contain the right one. FamilyCare exposes no payee "
		+ "register, so a past payment is the only evidence a payee exists. Reading is side-effect free.")
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class PayeeResource {

	private final PayeeService service;

	PayeeResource(final PayeeService service) {
		this.service = service;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the selectable betalningsmottagare for an errand",
		description = "The payees derived from the applicant's Lifecare payments in the last 12 months (most recently paid first, "
			+ "source=LIFECARE, no id), followed by the ones added by hand on this errand (source=MANUAL) that are not already among them. "
			+ "A manually added payee collapses into its Lifecare twin once a payment has gone to it, so the list never offers the same "
			+ "account twice. The Lifecare read is best-effort: an outage degrades the list to the manual rows rather than failing.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<List<PayeeOption>> listPayees(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.list(municipalityId, namespace, errandId));
	}

	@PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add a betalningsmottagare by hand",
		description = "Stores the payee on the errand so it is selectable immediately. It is created in Lifecare by Draken's BFF, which "
			+ "reports back through POST .../{payeeId}/lifecare-result; until then the payee is lifecareStatus=PENDING. An identical "
			+ "payee already on the errand is returned as-is instead of being duplicated.",
		responses = {
			@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
		})
	ResponseEntity<PayeeOption> createPayee(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final PayeeRequest request) {

		final var payee = service.create(municipalityId, namespace, errandId, request);
		return created(fromPath("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/payees/{payeeId}")
			.buildAndExpand(municipalityId, namespace, errandId, payee.getId()).toUri())
			.body(payee);
	}

	@GetMapping(path = "/{payeeId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read a manually added betalningsmottagare",
		description = "The payee's name, payment method, clearing and account number — read by Draken's BFF when it creates the "
			+ "payee in Lifecare.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<PayeeOption> getPayee(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String payeeId) {

		return ok(service.get(municipalityId, namespace, errandId, payeeId));
	}

	@PostMapping(path = "/{payeeId}/lifecare-result", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Report the outcome of creating the payee in Lifecare",
		description = "ADDED and ALREADY_EXISTS both set lifecareStatus=SYNCED — a payee already in Lifecare is not created twice, and the "
			+ "caseworker's intent is satisfied either way. FAILED requires detail, which must be Lifecare's own message since it is shown "
			+ "to the caseworker as-is. Re-reporting the same outcome is idempotent; reporting FAILED on an already SYNCED payee is a 409.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "409", description = "Conflict", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<PayeeOption> reportLifecareResult(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String payeeId,
		@Valid @NotNull @RequestBody final PayeeLifecareResult result) {

		return ok(service.recordLifecareResult(municipalityId, namespace, errandId, payeeId, result));
	}

	@DeleteMapping(path = "/{payeeId}")
	@Operation(summary = "Remove a manually added betalningsmottagare",
		description = "Removes the payee from the errand. Only manually added payees exist as rows — a LIFECARE-derived option has no id "
			+ "and cannot be removed here. Does not remove anything already written into Lifecare.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true)
		})
	ResponseEntity<Void> deletePayee(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@ValidUuid @PathVariable final String payeeId) {

		service.delete(municipalityId, namespace, errandId, payeeId);
		return noContent().build();
	}
}
