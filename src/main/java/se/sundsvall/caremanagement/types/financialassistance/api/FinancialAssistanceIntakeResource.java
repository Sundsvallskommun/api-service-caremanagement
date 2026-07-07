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
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Actualisation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceMetadata;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RenewalPrefill;
import se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceTypes;
import se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceService;
import se.sundsvall.caremanagement.types.financialassistance.service.RenewalPrefillService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands")
@Tag(name = FinancialAssistanceApiTags.INTAKE, description = FinancialAssistanceApiTags.INTAKE_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceIntakeResource {

	private final FinancialAssistanceService service;
	private final EligibilityService eligibilityService;
	private final RenewalPrefillService renewalPrefillService;

	FinancialAssistanceIntakeResource(final FinancialAssistanceService service, final EligibilityService eligibilityService,
		final RenewalPrefillService renewalPrefillService) {
		this.service = service;
		this.eligibilityService = eligibilityService;
		this.renewalPrefillService = renewalPrefillService;
	}

	@PostMapping(path = "/financial-assistance/eligibility", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Check application eligibility (common entry point)",
		description = "Given an applicant (and an optional co-applicant) suggests which application — new application / renewal / supplementary application — to offer, checking this system and Lifecare (best-effort).",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<EligibilityResponse> checkEligibility(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final EligibilityRequest request) {

		return ok(eligibilityService.evaluate(municipalityId, namespace, request));
	}

	@PostMapping(path = "/financial-assistance/actualisation", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create the Lifecare actualisation (case intake)",
		description = "Builds the actualisation against the applicant's Lifecare FC actualisation proposal and creates it in Lifecare, returning the created actualisation id. When the request carries an errandId, the creation is recorded on the errand as a Decision(ACTUALISATION) for the audit trail.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<ActualisationResponse> createActualisation(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final ActualisationRequest request) {

		return ok(service.createActualisation(municipalityId, namespace, request));
	}

	@GetMapping(path = "/financial-assistance/actualisations", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the applicant's Lifecare actualisations",
		description = "The Lifecare actualisations (case intakes) registered on the applicant — what the frontend lists so a caseworker can pick which actualisation to archive a supplementary application to. The applicant is identified by partyId (resolved to a personnummer via the citizen service). The period defaults to the last 24 months up to today when from/to are omitted.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<Actualisation>> listActualisations(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The applicant's partyId (personId GUID)") @ValidUuid @RequestParam final String partyId,
		@Parameter(description = "Inclusive start of the listing period (ISO date). Defaults to 24 months before 'to'.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@Parameter(description = "Inclusive end of the listing period (ISO date). Defaults to today.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to) {

		return ok(service.listActualisations(municipalityId, partyId, from, to));
	}

	@PostMapping(path = "/financial-assistance/actualisations/{actualisationId}/archive", consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Archive a document to a Lifecare actualisation",
		description = "Binds an uploaded document (e.g. a supplementary application) as an attachment to a specific Lifecare actualisation. The target actualisation must belong to the given applicant (partyId) — a foreign actualisation id yields 404 before anything is uploaded. Multipart request: the 'file' part carries the document; the optional 'request' part carries the metadata (errandId, title, documentType, documentSenderType, senderName) — title/documentType/documentSenderType/senderName each fall back to a server default, and the title defaults to the uploaded file name. caremanagement only forwards the bytes to Lifecare. When 'request.errandId' is set, the target actualisation id is recorded on that errand as a Decision(ACTUALISATION), setting the errand's Lifecare actualisation to the one archived to.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> archiveToActualisation(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The target Lifecare actualisation id") @PathVariable final Integer actualisationId,
		@Parameter(description = "The applicant's partyId (personId GUID) that owns the target actualisation") @ValidUuid @RequestParam final String partyId,
		@RequestPart("file") final MultipartFile file,
		@Valid @RequestPart(value = "request", required = false) final ArchiveActualisationRequest request) {

		service.archiveToActualisation(municipalityId, namespace, partyId, actualisationId, file, request);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@GetMapping(path = "/financial-assistance/metadata", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read financial assistance type metadata (income / cost dropdowns)",
		description = "The complete income and cost type catalogue the frontend feeds its financial assistance dropdowns from. Each type carries a code, the citizen Mina-sidor label (externalDisplayName), the matching Lifecare caseworker dropdown label (internalDisplayName), the Mina-sidor form group as a stable code (HOUSING / WORK_AND_STUDIES / HEALTH / OTHER — null for income) and citizenReportable. citizenReportable=true types are the Mina-sidor form (code = the Income.incomeType / Cost.costType value, externalDisplayName set); citizenReportable=false types are caseworker-only Lifecare dropdowns (internalDisplayName only, externalDisplayName null, their codes are NOT citizen payload values). Static; a label/grouping layer that never changes the payload codes.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<FinancialAssistanceMetadata> getMetadata(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace) {

		return ok(FinancialAssistanceTypes.metadata());
	}

	@GetMapping(path = "/financial-assistance/prefill", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Renewal pre-fill from Lifecare",
		description = "Returns the household children from the applicant's most recent Lifecare calculation to pre-fill a financial assistance renewal. The applicant is identified by partyId (resolved to a personnummer via the citizen service). Only children are pre-filled — the applicant is the logged-in citizen and the co-applicant comes from the portal. Best-effort — degrades to an empty result (lifecareChecked=false) when the partyId cannot be resolved or Lifecare is unreachable.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<RenewalPrefill> prefill(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @RequestParam final String partyId) {

		return ok(renewalPrefillService.prefill(municipalityId, partyId));
	}
}
