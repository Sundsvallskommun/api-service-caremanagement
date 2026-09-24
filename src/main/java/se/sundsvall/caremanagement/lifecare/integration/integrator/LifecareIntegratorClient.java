package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecareintegrator.ActualisationProposal;
import generated.se.sundsvall.lifecareintegrator.CalculationProposal;
import generated.se.sundsvall.lifecareintegrator.Caseworker;
import generated.se.sundsvall.lifecareintegrator.Contact;
import generated.se.sundsvall.lifecareintegrator.CreateActualisationRequest;
import generated.se.sundsvall.lifecareintegrator.CreatedResource;
import generated.se.sundsvall.lifecareintegrator.DecisionsResponse;
import generated.se.sundsvall.lifecareintegrator.PagedActualisationResponse;
import generated.se.sundsvall.lifecareintegrator.PagedCalculationResponse;
import generated.se.sundsvall.lifecareintegrator.PagedDocumentResponse;
import generated.se.sundsvall.lifecareintegrator.PagedPaymentResponse;
import generated.se.sundsvall.lifecareintegrator.PagedServiceResponse;
import generated.se.sundsvall.lifecareintegrator.Person;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDate;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static se.sundsvall.caremanagement.lifecare.integration.integrator.configuration.LifecareIntegratorConfiguration.CLIENT_ID;

/**
 * Reads Lifecare FamilyCare through {@code api-service-lifecare-integrator} rather than from FamilyCare directly.
 *
 * <p>
 * Two differences from {@link se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareClient} are worth
 * knowing when reading this. Everything is keyed on {@code partyId} rather than a personal identity number — the
 * integrator refuses to take or return one — and the period parameters are plain {@link LocalDate}s rather than
 * FamilyCare's RFC 3339 timestamps, so no date rendering is needed on this side.
 *
 * <p>
 * Only the operations the financial assistance flow actually uses are declared. FamilyCare's investigations,
 * executions and resource allocations are on the direct client but called from nowhere, so they are left out rather
 * than carried along.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.lifecare-integrator.url}", configuration = se.sundsvall.caremanagement.lifecare.integration.integrator.configuration.LifecareIntegratorConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface LifecareIntegratorClient {

	@GetMapping(path = "/{municipalityId}/person", produces = APPLICATION_JSON_VALUE)
	Person getPerson(@PathVariable final String municipalityId, @RequestParam final String partyId);

	@GetMapping(path = "/{municipalityId}/calculations", produces = APPLICATION_JSON_VALUE)
	PagedCalculationResponse getCalculations(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/calculations/proposal", produces = APPLICATION_JSON_VALUE)
	CalculationProposal getCalculationProposal(@PathVariable final String municipalityId, @RequestParam final String partyId);

	@GetMapping(path = "/{municipalityId}/decisions", produces = APPLICATION_JSON_VALUE)
	DecisionsResponse getDecisions(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/payments", produces = APPLICATION_JSON_VALUE)
	PagedPaymentResponse getPayments(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/actualisations", produces = APPLICATION_JSON_VALUE)
	PagedActualisationResponse getActualisations(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/services", produces = APPLICATION_JSON_VALUE)
	PagedServiceResponse getServices(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/documents", produces = APPLICATION_JSON_VALUE)
	PagedDocumentResponse getDocuments(
		@PathVariable final String municipalityId,
		@RequestParam final String partyId,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate from,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate to);

	@GetMapping(path = "/{municipalityId}/contacts", produces = APPLICATION_JSON_VALUE)
	List<Contact> getContacts(@PathVariable final String municipalityId, @RequestParam final String partyId);

	/** The only read that is not person-scoped: the caseworker directory, used to resolve a name to a user id. */
	@GetMapping(path = "/{municipalityId}/users", produces = APPLICATION_JSON_VALUE)
	List<Caseworker> getUsers(
		@PathVariable final String municipalityId,
		@RequestParam final Integer limit,
		@RequestParam(required = false) final Integer offset,
		@RequestParam(required = false) final String modifiedAfter,
		@RequestParam(required = false) final String modifiedBefore);

	@GetMapping(path = "/{municipalityId}/documents/{documentId}/content", produces = APPLICATION_PDF_VALUE)
	byte[] getDocumentContent(@PathVariable final String municipalityId, @PathVariable final String documentId);

	@GetMapping(path = "/{municipalityId}/actualisations/proposal", produces = APPLICATION_JSON_VALUE)
	ActualisationProposal getActualisationProposal(@PathVariable final String municipalityId, @RequestParam final String partyId);

	// ---- Writes ------------------------------------------------------------------------------------------------------
	//
	// These three write into Lifecare. The integrator answers 201 with the created id for the two creates and 204 for
	// the upload.

	@PostMapping(path = "/{municipalityId}/actualisations", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	CreatedResource createActualisation(@PathVariable final String municipalityId, @RequestBody final CreateActualisationRequest body);

	/**
	 * The part names are the integrator's own ({@code documentType}, {@code senderType}, {@code title},
	 * {@code senderName}, {@code file}) — not FamilyCare's capitalised {@code InsertDocumentType} / {@code Content}.
	 */
	@PostMapping(path = "/{municipalityId}/actualisations/{actualisationId}/attachments", consumes = MULTIPART_FORM_DATA_VALUE)
	void addActualisationAttachment(
		@PathVariable final String municipalityId,
		@PathVariable final Integer actualisationId,
		@RequestPart("documentType") final String documentType,
		@RequestPart("senderType") final String senderType,
		@RequestPart(value = "title", required = false) final String title,
		@RequestPart(value = "senderName", required = false) final String senderName,
		@RequestPart("file") final MultipartFile file);
}
