package se.sundsvall.caremanagement.types.financialassistance.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.core.api.validation.groups.OnCreate;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CreateFinancialAssistanceRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceData;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinancialAssistanceView;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceErrandService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
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
import static tools.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@RestController
@Validated
@RequestMapping("/{municipalityId}/{namespace}/errands")
@Tag(name = FinancialAssistanceApiTags.ERRANDS, description = FinancialAssistanceApiTags.ERRANDS_DESC)
@ApiResponses(value = {
	@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
		Problem.class, ConstraintViolationProblem.class
	}))),
	@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
})
class FinancialAssistanceErrandResource {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceErrandResource.class);

	/**
	 * Constrains the create path variable to the three financial assistance slugs so it never shadows other errand types.
	 */
	private static final String SLUG_REGEXP = "financial-assistance-new|financial-assistance-renewal|financial-assistance-supplementary";

	private final FinancialAssistanceErrandService service;
	private final ObjectMapper objectMapper;
	private final Validator validator;

	FinancialAssistanceErrandResource(final FinancialAssistanceErrandService service, final ObjectMapper objectMapper, final Validator validator) {
		this.service = service;
		this.objectMapper = objectMapper;
		this.validator = validator;
	}

	@PostMapping(path = "/{typeSlug:" + SLUG_REGEXP + "}", consumes = MULTIPART_FORM_DATA_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Create financial assistance errand",
		description = "Multipart request. The 'request' part carries the application (JSON); the optional 'attachments' part carries the citizen's supporting files (any type); the optional 'caseData' part carries the application snapshot (case data), stored as a single CASE_DATA attachment renamed to {errandNumber}.pdf; the optional 'formSnapshot' part carries the self-describing JSON snapshot of the form exactly as the applicant saw it (every question, help/info/notice text, option label and answer), captured write-once for the legal record and readable back via GET .../form-snapshot. typeSlug is one of financial-assistance-new, financial-assistance-renewal, financial-assistance-supplementary. Each attachment is stored on the errand, and a single combined PDF merging them all is generated and stored alongside.",
		responses = {
			@ApiResponse(responseCode = "201", headers = @Header(name = LOCATION, schema = @Schema(type = "string")), description = "Successful operation", useReturnTypeSchema = true)
		})
	@Validated(OnCreate.class)
	ResponseEntity<Void> createErrand(
		@ValidMunicipalityId @PathVariable final String municipalityId,
		@Pattern(regexp = NAMESPACE_REGEXP, message = NAMESPACE_VALIDATION_MESSAGE) @PathVariable final String namespace,
		@PathVariable final String typeSlug,
		@Parameter(schema = @Schema(implementation = CreateFinancialAssistanceRequest.class)) @NotNull @RequestPart("request") final byte[] requestJson,
		@RequestPart(value = "attachments", required = false) final List<MultipartFile> attachments,
		@RequestPart(value = "caseData", required = false) final MultipartFile caseData,
		@RequestPart(value = "formSnapshot", required = false) final String formSnapshot) {

		// The 'request' part is bound as raw bytes rather than a typed POJO so the endpoint tolerates multipart clients
		// (e.g. axios FormData) that send the JSON part without an explicit 'Content-Type: application/json'; reading
		// bytes also lets Jackson detect the JSON encoding itself instead of relying on a part charset. We then
		// deserialize and bean-validate it by hand, reproducing the @Valid + OnCreate-group behaviour.
		final var request = readCreateRequest(requestJson);

		final var id = service.create(municipalityId, namespace, typeSlug, request, attachments, caseData, formSnapshot);
		return created(fromPath("/{municipalityId}/{namespace}/errands/financial-assistance/{errandId}")
			.buildAndExpand(municipalityId, namespace, id).toUri())
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	private CreateFinancialAssistanceRequest readCreateRequest(final byte[] requestJson) {
		final CreateFinancialAssistanceRequest request;
		try {
			// Read leniently to match the typed @RequestPart binding this replaced: ignore unknown properties and accept a
			// single scalar where the model declares a list (e.g. normType), the same coercions Spring's converters allow.
			request = objectMapper.readerFor(CreateFinancialAssistanceRequest.class)
				.without(FAIL_ON_UNKNOWN_PROPERTIES)
				.with(ACCEPT_SINGLE_VALUE_AS_ARRAY)
				.readValue(requestJson);
		} catch (final JacksonException e) {
			// Do NOT echo Jackson's message — it can embed the offending field value (e.g. the rejected string), and the
			// 'request' part is the financial assistance application carrying applicant data. Log the technical reason server-side
			// (this
			// service already keeps payloads out of logs elsewhere) and return a value-free message to the caller.
			LOG.warn("Could not bind the 'request' part to a financial-assistance application: {}", e.getClass().getSimpleName());
			throw Problem.valueOf(BAD_REQUEST, "The 'request' part could not be read as a financial-assistance application — check that it is valid JSON matching the schema.");
		}

		// Validate with both groups to mirror the framework method-validation this part used to get: OnCreate carries
		// the create-only constraints (@NotBlank title, @NotNull data) and Default carries the nested @OneOf checks.
		final var violations = validator.validate(request, OnCreate.class, Default.class);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
		return request;
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

	@PatchMapping(path = "/financial-assistance/{errandId}/data", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	@Operation(summary = "Update financial assistance data",
		description = "PATCH semantics: non-null fields replace the stored values, null fields are left untouched. Server-owned fields (applicationType, lastDailyRunAt, timestamps) are never written from client data.",
		responses = {
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
