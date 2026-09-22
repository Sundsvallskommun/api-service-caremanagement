package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SsbtekBasis;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceSsbtekService;
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
@Tag(name = FinancialAssistanceApiTags.SSBTEK, description = FinancialAssistanceApiTags.SSBTEK_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceSsbtekResource {

	private final FinancialAssistanceSsbtekService service;

	FinancialAssistanceSsbtekResource(final FinancialAssistanceSsbtekService service) {
		this.service = service;
	}

	@GetMapping(path = "/financial-assistance/ssbtek", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the applicant's SSBTEK basis",
		description = """
			The applicant's SSBTEK basis, fetched live via api-service-financial-aid and forwarded verbatim — the answer \
			per responding agency (af, csn, fk, skv, so, tns, miv), so a caseworker can see what the composite service \
			actually said rather than only the classified result. The applicant is identified by partyId (resolved to a \
			personnummer via the citizen service); no personnummer is accepted or returned at this edge, and nothing is \
			stored. The period is resolved in whole months and echoed on the response: it defaults to the three SSBTEK \
			rule periods (jämförelseperiod M−2 through ansökningsperiod M), the same window the process asks for. \
			Agency payload shapes are heterogeneous and follow the SSBTEK contract, so they are not modelled here.""",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<SsbtekBasis> getBasis(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The applicant's partyId (personId GUID)") @ValidUuid @RequestParam final String partyId,
		@Parameter(description = "Inclusive start of the period (ISO date). Defaults to the first day of month M−2.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@Parameter(description = "Inclusive end of the period (ISO date). Defaults to the last day of the current month.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to) {

		return ok(service.getBasis(municipalityId, partyId, from, to));
	}
}
