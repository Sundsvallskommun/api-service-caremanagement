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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionReason;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionSaveRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionView;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands/financial-assistance")
@Tag(name = FinancialAssistanceApiTags.LIFECARE, description = FinancialAssistanceApiTags.LIFECARE_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
	@ApiResponse(responseCode = "502",
		description = "Bad Gateway - Lifecare could not be reached or answered with an error",
		content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
			schema = @Schema(
				implementation = Problem.class)))
})
class FinancialAssistanceLifecareDecisionResource {

	private final LifecareDecisionService service;

	FinancialAssistanceLifecareDecisionResource(final LifecareDecisionService service) {
		this.service = service;
	}

	@GetMapping(path = "/{errandId}/lifecare/decision", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the errand's beslut as it stands in Lifecare",
		description = """
			The beslut the errand is linked to in Lifecare (lifecareDecisionId), read from Lifecare. 204 while no beslut has \
			been saved. The read is logged in the errand's Lifecare access log.""",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "204", description = "No beslut saved in Lifecare yet"),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<LifecareDecisionView> readDecision(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", example = "FINANCIAL_ASSISTANCE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId") @ValidUuid @PathVariable final String errandId) {

		return service.read(municipalityId, namespace, errandId)
			.map(ResponseEntity::ok)
			.orElseGet(() -> noContent().build());
	}

	@PutMapping(path = "/{errandId}/lifecare/decision", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Save the errand's beslut in Lifecare - created the first time, changed after that",
		description = """
			Writes the beslut to Lifecare: Decision/Create the first time, after which the errand is linked to it \
			(lifecareDecisionId) in the same call, and Decision/Update of that same beslut every time after. The \
			beslutsfattare is the caller (X-Sent-By), who must be one of Lifecare's beslutsfattare for the insats. Refused \
			with 422, worded for the caseworker, when the beslutstyp is not active on the insats or cannot be registered from \
			careM, the household has a medsökande, the caller is not a beslutsfattare, a period the beslutstyp requires is \
			missing, the beslut is locked in Lifecare, or an existing beslut would change beslutstyp; Lifecare's own refusals \
			are 422 with Lifecare's message. 502 when Lifecare did not answer the create (check Lifecare before saving again) \
			or when the beslut was created but could not be linked to the errand (do not save again). Answers with the beslut \
			as it stands in Lifecare after the save.""",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "409",
				description = "Conflict - the errand has no Lifecare insats yet",
				content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(
						implementation = Problem.class))),
			@ApiResponse(responseCode = "422",
				description = "Unprocessable - the beslut cannot be saved as it stands",
				content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(
						implementation = Problem.class)))
		})
	ResponseEntity<LifecareDecisionView> saveDecision(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", example = "FINANCIAL_ASSISTANCE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId") @ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final LifecareDecisionSaveRequest request) {

		return ok(service.save(municipalityId, namespace, errandId, request));
	}

	@GetMapping(path = "/{errandId}/lifecare/decision/types", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the beslutstyper the errand's insats offers in Lifecare",
		description = """
			The active beslutstyper of the errand's insats, read from Lifecare's underlag for a new beslut, each with careM's \
			outcome (BIFALL or AVSLAG; absent for a beslutstyp that cannot be registered from careM).""",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "409",
				description = "Conflict - the errand has no Lifecare insats yet",
				content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(
						implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareDecisionType>> readDecisionTypes(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", example = "FINANCIAL_ASSISTANCE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId") @ValidUuid @PathVariable final String errandId) {

		return ok(service.types(municipalityId, namespace, errandId));
	}

	@GetMapping(path = "/lifecare/decision-types/{decisionCode}/reasons", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the orsaker a beslut of a Lifecare beslutstyp can carry",
		description = """
			Lifecare's orsak catalogue for the beslutstyp, flattened to its choosable orsaker, each with the heading it sits \
			under. The catalogue belongs to the beslutstyp, not to an applicant, so it is not errand-scoped and not logged in \
			an access log.""",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<List<LifecareDecisionReason>> readDecisionReasons(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", example = "FINANCIAL_ASSISTANCE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "decisionCode", description = "Lifecare's beslutstyp code", example = "153") @Positive @PathVariable final Integer decisionCode) {

		return ok(service.reasons(decisionCode));
	}

	@GetMapping(path = "/{errandId}/lifecare/decision/pdf", produces = APPLICATION_PDF_VALUE)
	@Operation(summary = "Read the errand's beslut as Lifecare prints it (PDF)",
		description = """
			The beslut the errand is linked to, rendered by Lifecare's own decision print template - for the preview and for \
			what is sent to the applicant. 404 while no beslut is saved in Lifecare. The read is logged in the errand's \
			Lifecare access log.""",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", content = @Content(mediaType = APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<byte[]> readDecisionPdf(
		@Parameter(name = "municipalityId", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "namespace", example = "FINANCIAL_ASSISTANCE") @Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(name = "errandId") @ValidUuid @PathVariable final String errandId) {

		return ok().header(CONTENT_TYPE, APPLICATION_PDF_VALUE).body(service.pdf(municipalityId, namespace, errandId));
	}
}
