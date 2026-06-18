package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
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
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ActualisationResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateWarningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RenewalPrefill;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceService;
import se.sundsvall.caremanagement.types.financialassistance.service.RenewalPrefillService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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
@Tag(name = "Financial Assistance",
	description = "Ekonomiskt bistånd (EB) errands. Create against one of the three application-type slugs (financial-assistance-new / -renewal / -supplementary); read and replace the typed data via the shared financial-assistance path.")
class FinancialAssistanceResource {

	/** Constrains the create path variable to the three EB slugs so it never shadows other errand types. */
	private static final String SLUG_REGEXP = "financial-assistance-new|financial-assistance-renewal|financial-assistance-supplementary";

	private final FinancialAssistanceService service;
	private final EligibilityService eligibilityService;
	private final RenewalPrefillService renewalPrefillService;

	FinancialAssistanceResource(final FinancialAssistanceService service, final EligibilityService eligibilityService,
		final RenewalPrefillService renewalPrefillService) {
		this.service = service;
		this.eligibilityService = eligibilityService;
		this.renewalPrefillService = renewalPrefillService;
	}

	@PostMapping(path = "/{typeSlug:" + SLUG_REGEXP + "}", consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create financial assistance errand",
		description = "Multipart request. The 'request' part carries the application (JSON); the optional 'attachments' part carries the citizen's supporting files (any type). typeSlug is one of financial-assistance-new, financial-assistance-renewal, financial-assistance-supplementary. Each attachment is stored on the errand, and a single combined PDF merging them all is generated and stored alongside.",
		responses = {
			@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
		})
	@Validated(OnCreate.class)
	ResponseEntity<Void> createErrand(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String typeSlug,
		@Valid @NotNull @RequestPart("request") final CreateFinancialAssistanceRequest request,
		@RequestPart(value = "attachments", required = false) final List<MultipartFile> attachments) {

		final var id = service.create(municipalityId, namespace, typeSlug, request, attachments);
		return created(fromPath("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}")
			.buildAndExpand(municipalityId, namespace, id).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@PostMapping(path = "/financial-assistance/eligibility", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Check application eligibility (gemensam ingång)",
		description = "Given an applicant (and an optional co-applicant) suggests which application — nyansökan / återansökan / tilläggsansökan — to offer, checking this system and Lifecare (best-effort).",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<EligibilityResponse> checkEligibility(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final EligibilityRequest request) {

		return ok(eligibilityService.evaluate(municipalityId, namespace, request));
	}

	@PostMapping(path = "/financial-assistance/normberakning/prepare", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Prepare the normberäkning (no Lifecare write)",
		description = "Reports whether this month's classified incomes cover every income type the previous normberäkning had (informationComplete + missingIncomeTypes), records the income warnings on the errand as a single Decision(RECOMMENDATION), and reflects completeness in the errand status (KOMPLETTERING ⇄ VANTAR_PA_BESLUT). Does NOT create a normberäkning in Lifecare — the EB process calls this each daily loop. Use /commit after a beslut to create it in Lifecare.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<NormberakningResponse> prepareNormberakning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final NormberakningRequest request) {

		return ok(service.prepareNormberakning(municipalityId, namespace, request));
	}

	@PostMapping(path = "/financial-assistance/normberakning/commit", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create the normberäkning in Lifecare (after beslut)",
		description = "Builds the normberäkning from the classified incomes and creates it in Lifecare FC, returning the created calculation id. Called once a beslut is taken — never during the daily SSBTEK loop.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<NormberakningResponse> commitNormberakning(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@Valid @NotNull @RequestBody final NormberakningRequest request) {

		return ok(service.commitNormberakning(municipalityId, namespace, request));
	}

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

	@GetMapping(path = "/financial-assistance/{errandId}/warnings", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "List the EB income warnings on an errand",
		description = "The acknowledgeable income warnings the handläggare reviews — unhandled incomes, significant changes, and income types still missing from SSBTEK. The daily prepare step reconciles them.",
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

	@PatchMapping(path = "/financial-assistance/{errandId}/warnings/{warningId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Acknowledge or close an EB income warning",
		description = "A handläggare acknowledges (seen, kept on record) or closes (dismisses) a warning. Re-opening to OPEN is not allowed.",
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
			"ACKNOWLEDGED", "CLOSED"
		})) @RequestParam final String status) {

		return ok(service.updateWarning(municipalityId, namespace, errandId, warningId, status));
	}

	@GetMapping(path = "/financial-assistance/{errandId}/normberakning/draft", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read the draft normberäkning",
		description = "The FC income rows the EB process prepared (not yet created in Lifecare) for the handläggare to review and edit before a beslut. 404 when no draft exists yet.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	ResponseEntity<NormberakningDraft> getDraft(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String errandId) {

		return ok(service.getDraft(municipalityId, namespace, errandId));
	}

	// --- per-row handläggare edits on the draft. Each touches only the handläggare value / note / soft-delete; the
	// process columns are owned by the daily prepare. 404 when the errand or row is missing in this namespace/municipality.
	// ---

	@PostMapping(path = "/financial-assistance/{errandId}/normberakning/draft/incomes", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add an income row to the draft (handläggare)", responses = {
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

	@PatchMapping(path = "/financial-assistance/{errandId}/normberakning/draft/incomes/{rowId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the handläggare value / note on an income row", responses = {
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

	@DeleteMapping(path = "/financial-assistance/{errandId}/normberakning/draft/incomes/{rowId}", produces = APPLICATION_JSON_VALUE)
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

	@PostMapping(path = "/financial-assistance/{errandId}/normberakning/draft/incomes/{rowId}/restore", produces = APPLICATION_JSON_VALUE)
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

	@PostMapping(path = "/financial-assistance/{errandId}/normberakning/draft/expenses", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add an expense row to the draft (handläggare)", responses = {
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

	@PatchMapping(path = "/financial-assistance/{errandId}/normberakning/draft/expenses/{rowId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the handläggare value / note on an expense row", responses = {
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

	@DeleteMapping(path = "/financial-assistance/{errandId}/normberakning/draft/expenses/{rowId}", produces = APPLICATION_JSON_VALUE)
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

	@PostMapping(path = "/financial-assistance/{errandId}/normberakning/draft/expenses/{rowId}/restore", produces = APPLICATION_JSON_VALUE)
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

	@PostMapping(path = "/financial-assistance/{errandId}/normberakning/draft/persons", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Add a person row to the draft (handläggare)", responses = {
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

	@PatchMapping(path = "/financial-assistance/{errandId}/normberakning/draft/persons/{rowId}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Set the handläggare days / note on a person row", responses = {
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

	@DeleteMapping(path = "/financial-assistance/{errandId}/normberakning/draft/persons/{rowId}", produces = APPLICATION_JSON_VALUE)
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

	@PostMapping(path = "/financial-assistance/{errandId}/normberakning/draft/persons/{rowId}/restore", produces = APPLICATION_JSON_VALUE)
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

	@PostMapping(path = "/financial-assistance/actualisation", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Create the Lifecare aktualisering (case intake)",
		description = "Builds the aktualisering against the applicant's Lifecare FC aktualisering proposal and creates it in Lifecare, returning the created aktualisering id. When the request carries an errandId, the creation is recorded on the errand as a Decision(ACTUALISATION) for the audit trail.",
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

	@PostMapping(path = "/financial-assistance/payment-status", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read whether the Lifecare utbetalning has been effectuated",
		description = "Reads whether the manual Lifecare utbetalning for the applicant and application month has been registered, returning the effectuated flag and (when effectuated) the payment date. caremanagement makes no payment — utbetalning is a manual handläggare step in Lifecare; the process polls this to detect when it is done.",
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

	@GetMapping(path = "/financial-assistance/prefill", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Renewal pre-fill from Lifecare",
		description = "Returns the household children from the applicant's most recent Lifecare normberäkning to pre-fill an EB återansökan. The applicant is identified by partyId (resolved to a personnummer via the citizen service). Only children are pre-filled — the sökande is the logged-in citizen and the medsökande comes from the portal. Best-effort — degrades to an empty result (lifecareChecked=false) when the partyId cannot be resolved or Lifecare is unreachable.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
		})
	ResponseEntity<RenewalPrefill> prefill(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@ValidUuid @RequestParam final String partyId) {

		return ok(renewalPrefillService.prefill(municipalityId, partyId));
	}

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
