package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.core.api.validation.groups.OnCreate;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Actualisation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ArchiveActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceMetadata;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareCalculation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDocument;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RenewalPrefill;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovalRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.api.model.WarningCount;
import se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceTypes;
import se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceService;
import se.sundsvall.caremanagement.types.financialassistance.service.RenewalPrefillService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_REGEXP;
import static se.sundsvall.caremanagement.Constants.NAMESPACE_VALIDATION_MESSAGE;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands")
class FinancialAssistanceResource {

	/** Constrains the create path variable to the three EB slugs so it never shadows other errand types. */
	private static final String SLUG_REGEXP = "financial-assistance-new|financial-assistance-renewal|financial-assistance-supplementary";

	// Swagger tag groups. The EB surface is large (~29 operations), so each phase of the workflow is its own
	// navigable sub-section. They share the "Financial Assistance ·" prefix, so springdoc's alpha tagsSorter
	// keeps them clustered together while still splitting the flat list.
	private static final String TAG_ERRANDS = "Financial Assistance · Errands";
	private static final String TAG_ERRANDS_DESC = "Create, read and replace EB errands. Create against one of the three application-type slugs (financial-assistance-new / -renewal / -supplementary); read and replace the typed data via the shared financial-assistance path.";
	private static final String TAG_INTAKE = "Financial Assistance · Intake";
	private static final String TAG_INTAKE_DESC = "Pre-application and case-intake calls: eligibility routing (common entry point), renewal pre-fill from Lifecare, the income/cost type metadata catalogue, and Lifecare actualisation (case intake).";
	private static final String TAG_CALCULATION = "Financial Assistance · Calculation";
	private static final String TAG_CALCULATION_DESC = "The normberäkning: prepare the calculation each daily loop (no Lifecare write), commit it to Lifecare after a decision, and read or edit the draft header.";
	private static final String TAG_DRAFT_ROWS = "Financial Assistance · Draft rows";
	private static final String TAG_DRAFT_ROWS_DESC = "Caseworker edits to the draft calculation rows — add, edit, soft-delete and restore income, expense and person rows. Each touches only the caseworker value / note / soft-delete; the process columns are owned by the daily prepare.";
	private static final String TAG_WARNINGS = "Financial Assistance · Warnings";
	private static final String TAG_WARNINGS_DESC = "Acknowledgeable EB income warnings on an errand — create, list and set status (OPEN / ACKNOWLEDGED / CLOSED). The daily prepare step reconciles them.";
	private static final String TAG_APPROVALS = "Financial Assistance · Approvals";
	private static final String TAG_APPROVALS_DESC = "Caseworker approval state of the three EB view sections (CALCULATION / PAYMENT / DECISION) — read all three, or set/withdraw one.";
	private static final String TAG_PAYMENT = "Financial Assistance · Payment";
	private static final String TAG_PAYMENT_DESC = "Read whether the manual Lifecare payment for the applicant and application month has been effectuated. caremanagement makes no payment itself.";
	private static final String TAG_LIFECARE = "Financial Assistance · Lifecare history";
	private static final String TAG_LIFECARE_DESC = "Read the applicant's case history straight from Lifecare — the normberäkningar (calculations), beslut (decisions) and documents — plus a single document's PDF content. Keyed by partyId (resolved to a personnummer via the citizen service); the period defaults to the last 24 months. caremanagement only forwards the reads.";

	private final FinancialAssistanceService service;
	private final EligibilityService eligibilityService;
	private final RenewalPrefillService renewalPrefillService;
	private final ObjectMapper objectMapper;
	private final Validator validator;

	FinancialAssistanceResource(final FinancialAssistanceService service, final EligibilityService eligibilityService,
		final RenewalPrefillService renewalPrefillService, final ObjectMapper objectMapper, final Validator validator) {
		this.service = service;
		this.eligibilityService = eligibilityService;
		this.renewalPrefillService = renewalPrefillService;
		this.objectMapper = objectMapper;
		this.validator = validator;
	}

	@Tag(name = TAG_ERRANDS, description = TAG_ERRANDS_DESC)
	@PostMapping(path = "/{typeSlug:" + SLUG_REGEXP + "}", consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create financial assistance errand",
		description = "Multipart request. The 'request' part carries the application (JSON); the optional 'attachments' part carries the citizen's supporting files (any type); the optional 'caseData' part carries the application snapshot (ärendeuppgifter), stored as a single CASE_DATA attachment renamed to {errandNumber}.pdf; the optional 'formSnapshot' part carries the self-describing JSON snapshot of the form exactly as the applicant saw it (every question, help/info/notice text, option label and answer), captured write-once for the legal record and readable back via GET .../form-snapshot. typeSlug is one of financial-assistance-new, financial-assistance-renewal, financial-assistance-supplementary. Each attachment is stored on the errand, and a single combined PDF merging them all is generated and stored alongside.",
		responses = {
			@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
		})
	@Validated(OnCreate.class)
	ResponseEntity<Void> createErrand(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String typeSlug,
		@Parameter(schema = @Schema(implementation = CreateFinancialAssistanceRequest.class)) @NotNull @RequestPart("request") final String requestJson,
		@RequestPart(value = "attachments", required = false) final List<MultipartFile> attachments,
		@RequestPart(value = "caseData", required = false) final MultipartFile caseData,
		@RequestPart(value = "formSnapshot", required = false) final String formSnapshot) {

		// The 'request' part is bound as a raw String rather than a typed POJO so the endpoint tolerates multipart
		// clients (e.g. axios FormData) that send the JSON part without an explicit 'Content-Type: application/json'.
		// We then deserialize and bean-validate it by hand, reproducing the @Valid + OnCreate-group behaviour.
		final var request = readCreateRequest(requestJson);

		final var id = service.create(municipalityId, namespace, typeSlug, request, attachments, caseData, formSnapshot);
		return created(fromPath("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}")
			.buildAndExpand(municipalityId, namespace, id).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	private CreateFinancialAssistanceRequest readCreateRequest(final String requestJson) {
		final CreateFinancialAssistanceRequest request;
		try {
			request = objectMapper.readValue(requestJson, CreateFinancialAssistanceRequest.class);
		} catch (final JacksonException e) {
			throw Problem.valueOf(BAD_REQUEST, "The 'request' part is not valid JSON");
		}

		// Validate with both groups to mirror the framework method-validation this part used to get: OnCreate carries
		// the create-only constraints (@NotBlank title, @NotNull data) and Default carries the nested @OneOf checks.
		final var violations = validator.validate(request, OnCreate.class, Default.class);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
		return request;
	}

	@Tag(name = TAG_INTAKE, description = TAG_INTAKE_DESC)
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

	@Tag(name = TAG_CALCULATION, description = TAG_CALCULATION_DESC)
	@PostMapping(path = "/financial-assistance/calculation/prepare", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Prepare the calculation (no Lifecare write)",
		description = "Reports whether this month's classified incomes cover every income type the previous calculation had (informationComplete + missingIncomeTypes), records the income warnings on the errand as a single Decision(RECOMMENDATION), and reflects completeness in the errand status (SUPPLEMENT_REQUESTED ⇄ AWAITING_DECISION). Does NOT create a calculation in Lifecare — the EB process calls this each daily loop. Use /commit after a decision to create it in Lifecare.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<CalculationResponse> prepareCalculation(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final CalculationRequest request) {

		return ok(service.prepareCalculation(municipalityId, namespace, request));
	}

	@Tag(name = TAG_CALCULATION, description = TAG_CALCULATION_DESC)
	@PostMapping(path = "/financial-assistance/calculation/commit", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create the calculation in Lifecare (after decision)",
		description = "Builds the calculation from the classified incomes and creates it in Lifecare FC, returning the created calculation id. Called once a decision is taken — never during the daily SSBTEK loop.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<CalculationResponse> commitCalculation(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final CalculationRequest request) {

		return ok(service.commitCalculation(municipalityId, namespace, request));
	}

	@Tag(name = TAG_CALCULATION, description = TAG_CALCULATION_DESC)
	@PostMapping(path = "/financial-assistance/calculation/from-application", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create the calculation in Lifecare straight from the application (nyansökan)",
		description = "Builds the calculation from the data the citizen declared in the application — incomes resolved to FC types by name, expenses and household from the same feeder the renewal path uses — and creates it in Lifecare FC in one shot, returning the created calculation id. No SSBTEK, no daily loop, no caseworker draft. Used by the nyansökan process.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<CalculationResponse> commitFromApplication(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final CalculationRequest request) {

		return ok(service.commitFromApplication(municipalityId, namespace, request));
	}

	@Tag(name = TAG_WARNINGS, description = TAG_WARNINGS_DESC)
	@PostMapping(path = "/financial-assistance/{errandId}/warnings", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create an EB income warning on an errand",
		description = "Creates an acknowledgeable income warning directly on the errand — the careM temp stage, with no Lifecare round-trip. The warning is created OPEN; use the PATCH endpoint to acknowledge or close it.",
		responses = {
			@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Warning> createWarning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@Valid @NotNull @RequestBody final CreateWarningRequest request) {

		final var warning = service.createWarning(municipalityId, namespace, errandId, request);
		return created(fromPath("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}/warnings/{warningId}")
			.buildAndExpand(municipalityId, namespace, errandId, warning.getId()).toUri())
			.body(warning);
	}

	@Tag(name = TAG_WARNINGS, description = TAG_WARNINGS_DESC)
	@GetMapping(path = "/financial-assistance/{errandId}/warnings", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the EB income warnings on an errand",
		description = "The acknowledgeable income warnings the caseworker reviews — unhandled incomes, significant changes, and income types still missing from SSBTEK. The daily prepare step reconciles them.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<Warning>> listWarnings(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId) {

		return ok(service.listWarnings(municipalityId, namespace, errandId));
	}

	@Tag(name = TAG_WARNINGS, description = TAG_WARNINGS_DESC)
	@GetMapping(path = "/financial-assistance/{errandId}/warnings/count", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Count the active EB income warnings on an errand",
		description = "How many warnings are still active (OPEN or ACKNOWLEDGED) — closed ones are not counted. Not recorded in the event log.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<WarningCount> countWarnings(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId) {

		return ok(new WarningCount(service.countActiveWarnings(municipalityId, namespace, errandId)));
	}

	@Tag(name = TAG_WARNINGS, description = TAG_WARNINGS_DESC)
	@PatchMapping(path = "/financial-assistance/{errandId}/warnings/{warningId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the status of an EB income warning",
		description = "A caseworker acknowledges (seen, kept on record), closes (dismisses), or re-opens a warning to OPEN (undoing an earlier acknowledge/close).",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Warning> updateWarning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String warningId,
		@Parameter(description = "The target status", schema = @Schema(allowableValues = {
			"OPEN", "ACKNOWLEDGED", "CLOSED"
		})) @RequestParam final String status) {

		return ok(service.updateWarning(municipalityId, namespace, errandId, warningId, status));
	}

	@Tag(name = TAG_APPROVALS, description = TAG_APPROVALS_DESC)
	@GetMapping(path = "/financial-assistance/{errandId}/sections/approvals", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the section approvals on an errand",
		description = "The caseworker approval state of the three EB view sections (CALCULATION = calculation, PAYMENT = payment, DECISION = decision). Always returns all three — a section never approved is present with approved=false. The same object is embedded in the errand view.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<SectionApprovals> getSectionApprovals(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId) {

		return ok(service.getSectionApprovals(municipalityId, namespace, errandId));
	}

	@Tag(name = TAG_APPROVALS, description = TAG_APPROVALS_DESC)
	@PatchMapping(path = "/financial-assistance/{errandId}/sections/{section}/approval", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set a section's approval (caseworker)",
		description = "A caseworker verifies one of the EB view sections (CALCULATION / PAYMENT / DECISION) as approved, or withdraws an earlier approval. Approving stamps who/when; withdrawing clears them.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<SectionApproval> setSectionApproval(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@Parameter(description = "The section to approve", schema = @Schema(allowableValues = {
			"CALCULATION", "PAYMENT", "DECISION"
		})) @PathVariable final String section,
		@Valid @NotNull @RequestBody final SectionApprovalRequest request) {

		return ok(service.setSectionApproval(municipalityId, namespace, errandId, section, request.getApproved(), request.getApprovedBy()));
	}

	@Tag(name = TAG_CALCULATION, description = TAG_CALCULATION_DESC)
	@GetMapping(path = "/financial-assistance/{errandId}/calculation/draft", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the draft calculation",
		description = "The FC income rows the EB process prepared (not yet created in Lifecare) for the caseworker to review and edit before a decision. 404 when no draft exists yet.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<CalculationDraft> getDraft(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId) {

		return ok(service.getDraft(municipalityId, namespace, errandId));
	}

	@Tag(name = TAG_CALCULATION, description = TAG_CALCULATION_DESC)
	@PatchMapping(path = "/financial-assistance/{errandId}/calculation/draft/header", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Edit the draft header (caseworker)",
		description = "Set the norm, the calculation date window (from/to/date) and the custom household size (common costs). 404 when no draft exists.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<CalculationDraft> patchDraftHeader(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@Valid @NotNull @RequestBody final NormHeaderInput input) {

		return ok(service.patchDraftHeader(municipalityId, namespace, errandId, input));
	}

	// --- per-row caseworker edits on the draft. Each touches only the caseworker value / note / soft-delete; the
	// process columns are owned by the daily prepare. 404 when the errand or row is missing in this namespace/municipality.
	// ---

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/incomes", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add an income row to the draft (caseworker)", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormIncomeRow> addDraftIncome(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@Valid @NotNull @RequestBody final NormIncomeInput input) {

		return ok(service.addDraftIncome(municipalityId, namespace, errandId, input));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@PatchMapping(path = "/financial-assistance/{errandId}/calculation/draft/incomes/{rowId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the caseworker value / note on an income row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormIncomeRow> patchDraftIncome(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String rowId,
		@Valid @NotNull @RequestBody final NormIncomeInput input) {

		return ok(service.patchDraftIncome(municipalityId, namespace, errandId, rowId, input));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@DeleteMapping(path = "/financial-assistance/{errandId}/calculation/draft/incomes/{rowId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Soft-delete an income row (excluded from the calculation, survives the daily refresh)", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormIncomeRow> deleteDraftIncome(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String rowId) {

		return ok(service.setDraftIncomeDeleted(municipalityId, namespace, errandId, rowId, true));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/incomes/{rowId}/restore", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Restore a soft-deleted income row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormIncomeRow> restoreDraftIncome(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String rowId) {

		return ok(service.setDraftIncomeDeleted(municipalityId, namespace, errandId, rowId, false));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/expenses", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add an expense row to the draft (caseworker)", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormExpenseRow> addDraftExpense(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@Valid @NotNull @RequestBody final NormExpenseInput input) {

		return ok(service.addDraftExpense(municipalityId, namespace, errandId, input));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@PatchMapping(path = "/financial-assistance/{errandId}/calculation/draft/expenses/{rowId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the caseworker value / note on an expense row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormExpenseRow> patchDraftExpense(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String rowId,
		@Valid @NotNull @RequestBody final NormExpenseInput input) {

		return ok(service.patchDraftExpense(municipalityId, namespace, errandId, rowId, input));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@DeleteMapping(path = "/financial-assistance/{errandId}/calculation/draft/expenses/{rowId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Soft-delete an expense row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormExpenseRow> deleteDraftExpense(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String rowId) {

		return ok(service.setDraftExpenseDeleted(municipalityId, namespace, errandId, rowId, true));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/expenses/{rowId}/restore", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Restore a soft-deleted expense row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormExpenseRow> restoreDraftExpense(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String rowId) {

		return ok(service.setDraftExpenseDeleted(municipalityId, namespace, errandId, rowId, false));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/persons", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add a person row to the draft (caseworker)", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormPersonRow> addDraftPerson(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@Valid @NotNull @RequestBody final NormPersonInput input) {

		return ok(service.addDraftPerson(municipalityId, namespace, errandId, input));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@PatchMapping(path = "/financial-assistance/{errandId}/calculation/draft/persons/{rowId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the caseworker days / note on a person row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormPersonRow> patchDraftPerson(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String rowId,
		@Valid @NotNull @RequestBody final NormPersonInput input) {

		return ok(service.patchDraftPerson(municipalityId, namespace, errandId, rowId, input));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@DeleteMapping(path = "/financial-assistance/{errandId}/calculation/draft/persons/{rowId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Soft-delete a person row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormPersonRow> deleteDraftPerson(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String rowId) {

		return ok(service.setDraftPersonDeleted(municipalityId, namespace, errandId, rowId, true));
	}

	@Tag(name = TAG_DRAFT_ROWS, description = TAG_DRAFT_ROWS_DESC)
	@PostMapping(path = "/financial-assistance/{errandId}/calculation/draft/persons/{rowId}/restore", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Restore a soft-deleted person row", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<NormPersonRow> restoreDraftPerson(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId,
		@PathVariable final String rowId) {

		return ok(service.setDraftPersonDeleted(municipalityId, namespace, errandId, rowId, false));
	}

	@Tag(name = TAG_INTAKE, description = TAG_INTAKE_DESC)
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

	@Tag(name = TAG_INTAKE, description = TAG_INTAKE_DESC)
	@GetMapping(path = "/financial-assistance/actualisations", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the applicant's Lifecare actualisations",
		description = "The Lifecare actualisations (case intakes) registered on the applicant — what the frontend lists so a caseworker can pick which actualisation to archive a supplementary application (tilläggsansökan) to. The applicant is identified by partyId (resolved to a personnummer via the citizen service). The period defaults to the last 24 months up to today when from/to are omitted.",
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

	@Tag(name = TAG_LIFECARE, description = TAG_LIFECARE_DESC)
	@GetMapping(path = "/financial-assistance/calculations", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the applicant's Lifecare calculations (normberäkningar)",
		description = "The applicant's Lifecare normberäkningar over the period — the full breakdown (header sums plus income, expense, special-expense and household-member rows) the frontend renders straight from Lifecare. The applicant is identified by partyId (resolved to a personnummer via the citizen service). The period defaults to the last 24 months up to today when from/to are omitted.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareCalculation>> listCalculations(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The applicant's partyId (personId GUID)") @ValidUuid @RequestParam final String partyId,
		@Parameter(description = "Inclusive start of the listing period (ISO date). Defaults to 24 months before 'to'.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@Parameter(description = "Inclusive end of the listing period (ISO date). Defaults to today.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to) {

		return ok(service.listCalculations(municipalityId, partyId, from, to));
	}

	@Tag(name = TAG_LIFECARE, description = TAG_LIFECARE_DESC)
	@GetMapping(path = "/financial-assistance/decisions", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the applicant's Lifecare decisions (beslut)",
		description = "The applicant's Lifecare beslut over the period — the decision header plus the persons each concerned — served straight from Lifecare. The applicant is identified by partyId (resolved to a personnummer via the citizen service). The period defaults to the last 24 months up to today when from/to are omitted.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareDecision>> listDecisions(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The applicant's partyId (personId GUID)") @ValidUuid @RequestParam final String partyId,
		@Parameter(description = "Inclusive start of the listing period (ISO date). Defaults to 24 months before 'to'.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@Parameter(description = "Inclusive end of the listing period (ISO date). Defaults to today.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to) {

		return ok(service.listDecisions(municipalityId, partyId, from, to));
	}

	@Tag(name = TAG_LIFECARE, description = TAG_LIFECARE_DESC)
	@GetMapping(path = "/financial-assistance/documents", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the applicant's Lifecare documents (metadata)",
		description = "The applicant's Lifecare documents over the period — metadata only (id, title, date, type, owner). Fetch a single document's content via the /documents/{documentId}/content endpoint. The applicant is identified by partyId (resolved to a personnummer via the citizen service). The period defaults to the last 24 months up to today when from/to are omitted.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<List<LifecareDocument>> listDocuments(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The applicant's partyId (personId GUID)") @ValidUuid @RequestParam final String partyId,
		@Parameter(description = "Inclusive start of the listing period (ISO date). Defaults to 24 months before 'to'.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@Parameter(description = "Inclusive end of the listing period (ISO date). Defaults to today.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to) {

		return ok(service.listDocuments(municipalityId, partyId, from, to));
	}

	@Tag(name = TAG_LIFECARE, description = TAG_LIFECARE_DESC)
	@GetMapping(path = "/financial-assistance/documents/{documentId}/content", produces = APPLICATION_PDF_VALUE)
	@Operation(summary = "Read a Lifecare document's content (PDF)",
		description = "Streams a single Lifecare document's content (the generated PDF) by its document id, straight from Lifecare. caremanagement only forwards the bytes. 502 when Lifecare is unreachable; Lifecare answers 404 when the document has no generated PDF (content exists only for PDF-backed document types).",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", content = @Content(mediaType = APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<byte[]> readDocumentContent(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The Lifecare document id") @PathVariable final String documentId) {

		return ok().header(CONTENT_TYPE, APPLICATION_PDF_VALUE).body(service.readDocumentContent(documentId));
	}

	@Tag(name = TAG_INTAKE, description = TAG_INTAKE_DESC)
	@PostMapping(path = "/financial-assistance/actualisations/{actualisationId}/archive", consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Archive a document to a Lifecare actualisation",
		description = "Binds an uploaded document (e.g. a supplementary application — tilläggsansökan) as an attachment to a specific Lifecare actualisation. Multipart request: the 'file' part carries the document; the optional 'request' part carries the metadata (errandId, title, documentType, documentSenderType, senderName) — title/documentType/documentSenderType/senderName each fall back to a server default, and the title defaults to the uploaded file name. caremanagement only forwards the bytes to Lifecare. When 'request.errandId' is set, the target actualisation id is recorded on that errand as a Decision(ACTUALISATION), setting the errand's Lifecare actualisation to the one archived to.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<Void> archiveToActualisation(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Parameter(description = "The target Lifecare actualisation id") @PathVariable final Integer actualisationId,
		@RequestPart("file") final MultipartFile file,
		@Valid @RequestPart(value = "request", required = false) final ArchiveActualisationRequest request) {

		service.archiveToActualisation(municipalityId, namespace, actualisationId, file, request);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}

	@Tag(name = TAG_PAYMENT, description = TAG_PAYMENT_DESC)
	@PostMapping(path = "/financial-assistance/payment-status", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read whether the Lifecare payment has been effectuated",
		description = "Reads whether the manual Lifecare payment for the applicant and application month has been registered, returning the effectuated flag and (when effectuated) the payment date. caremanagement makes no payment — payment is a manual caseworker step in Lifecare; the process polls this to detect when it is done.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<PaymentStatusResponse> checkPaymentStatus(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final PaymentStatusRequest request) {

		return ok(service.checkPaymentStatus(municipalityId, request));
	}

	@Tag(name = TAG_INTAKE, description = TAG_INTAKE_DESC)
	@GetMapping(path = "/financial-assistance/metadata", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read EB type metadata (income / cost dropdowns)",
		description = "The complete income and cost type catalogue the frontend feeds its EB dropdowns from. Each type carries a code, the citizen Mina-sidor label (externalDisplayName), the matching Lifecare handläggare-dropdown label (internalDisplayName), the Mina-sidor form group as a stable code (HOUSING / WORK_AND_STUDIES / HEALTH / OTHER — null for income) and citizenReportable. citizenReportable=true types are the Mina-sidor form (code = the Income.incomeType / Cost.costType value, externalDisplayName set); citizenReportable=false types are handläggare-only Lifecare dropdowns (internalDisplayName only, externalDisplayName null, their codes are NOT citizen payload values). Static; a label/grouping layer that never changes the payload codes.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<FinancialAssistanceMetadata> getMetadata(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace) {

		return ok(FinancialAssistanceTypes.metadata());
	}

	@Tag(name = TAG_INTAKE, description = TAG_INTAKE_DESC)
	@GetMapping(path = "/financial-assistance/prefill", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Renewal pre-fill from Lifecare",
		description = "Returns the household children from the applicant's most recent Lifecare calculation to pre-fill an EB renewal. The applicant is identified by partyId (resolved to a personnummer via the citizen service). Only children are pre-filled — the applicant is the logged-in citizen and the co-applicant comes from the portal. Best-effort — degrades to an empty result (lifecareChecked=false) when the partyId cannot be resolved or Lifecare is unreachable.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<RenewalPrefill> prefill(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @RequestParam final String partyId) {

		return ok(renewalPrefillService.prefill(municipalityId, partyId));
	}

	@Tag(name = TAG_ERRANDS, description = TAG_ERRANDS_DESC)
	@GetMapping(path = "/financial-assistance/{errandId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read financial assistance errand", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<FinancialAssistanceView> readErrand(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.read(municipalityId, namespace, errandId));
	}

	@Tag(name = TAG_ERRANDS, description = TAG_ERRANDS_DESC)
	@GetMapping(path = "/financial-assistance/{errandId}/form-snapshot", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the application form snapshot",
		description = "The immutable, self-describing snapshot of the citizen-facing application form exactly as the applicant saw and answered it — every section, question label, help/info/notice text, option label and answer — captured at submission for the legal record. A generic renderer can walk it without the frontend that produced it. 404 when no snapshot was captured for the errand.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<FormSnapshot> readFormSnapshot(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId) {

		return ok(service.readFormSnapshot(municipalityId, namespace, errandId));
	}

	@Tag(name = TAG_ERRANDS, description = TAG_ERRANDS_DESC)
	@PutMapping(path = "/financial-assistance/{errandId}/data", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Replace financial assistance data", responses = {
		@ApiResponse(responseCode = "204", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> updateData(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @PathVariable final String errandId,
		@Valid @NotNull @RequestBody final FinancialAssistanceData data) {

		service.updateData(municipalityId, namespace, errandId, data);
		return noContent().header(CONTENT_TYPE, ALL_VALUE).build();
	}
}
