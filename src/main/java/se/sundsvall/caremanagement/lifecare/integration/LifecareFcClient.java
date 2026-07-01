package se.sundsvall.caremanagement.lifecare.integration;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDocumentDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedExecutionDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedInvestigationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedResourceAllocationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedContactDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedPersonDTO;
import generated.se.sundsvall.lifecarefc.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefc.PostCalculationBodyRequest;
import generated.se.sundsvall.lifecarefc.User;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.lifecare.integration.configuration.LifecareFcConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static se.sundsvall.caremanagement.lifecare.integration.configuration.LifecareFcConfiguration.CLIENT_ID;

/**
 * Feign contract for the financial assistance subset of the Tieto/Lifecare FamilyCare (FC) API: the person-based
 * case-data reads
 * (person, contacts, and the date-ranged lists that make up the financial assistance lifecycle — actualisations,
 * calculations,
 * decision, payments, investigations, services, executions, resource allocations), the write-back (create
 * actualisation + calculation), and the two proposal lookups that supply the code lists those POST bodies reference.
 * The mandatory {@code domain} + {@code key} auth (and the {@code X-API-Key} header) are added globally by
 * {@link LifecareFcConfiguration}, so they are not part of these method signatures. The list reads share
 * {@code startDate}/{@code endDate} (required) and optional {@code pageSize}/{@code pageNr}/{@code ascending}
 * pagination. Full API documented in vof-ekonomiskt-bistand/architecture/lifecare-fc-api.md.
 */
@FeignClient(name = CLIENT_ID, url = "${integration.lifecare-fc.url}", configuration = LifecareFcConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface LifecareFcClient {

	// ---- Person-based reads ------------------------------------------------------------------------------------------

	/**
	 * Read the FC master data for a person.
	 *
	 * @param  personId the full personal identity number
	 * @return          the person's FC master data
	 */
	@GetMapping(path = "/apifc/v1/Persons", produces = APPLICATION_JSON_VALUE)
	PersonBasedPersonDTO getPerson(
		@RequestParam("personId") final String personId);

	/**
	 * List the contacts registered on a person.
	 *
	 * @param  personId the full personal identity number
	 * @return          the person's contacts
	 */
	@GetMapping(path = "/apifc/v1/Contacts", produces = APPLICATION_JSON_VALUE)
	List<PersonBasedContactDTO> getContacts(
		@RequestParam("personId") final String personId);

	/**
	 * List the actualisations (case intakes) registered on a person in the given period.
	 */
	@GetMapping(path = "/apifc/v1/Actualisations", produces = APPLICATION_JSON_VALUE)
	ApiPaginationCompositePersonBasedAktualiseringDTO getActualisations(
		@RequestParam("personId") final String personId,
		@RequestParam("startDate") final String startDate,
		@RequestParam("endDate") final String endDate,
		@RequestParam(value = "pageSize", required = false) final Integer pageSize,
		@RequestParam(value = "pageNr", required = false) final Integer pageNr,
		@RequestParam(value = "ascending", required = false) final Boolean ascending);

	/**
	 * List the calculations registered on a person in the given period.
	 */
	@GetMapping(path = "/apifc/v1/Calculations", produces = APPLICATION_JSON_VALUE)
	ApiPaginationCompositePersonBasedCalculationDTO getCalculations(
		@RequestParam("personId") final String personId,
		@RequestParam("startDate") final String startDate,
		@RequestParam("endDate") final String endDate,
		@RequestParam(value = "pageSize", required = false) final Integer pageSize,
		@RequestParam(value = "pageNr", required = false) final Integer pageNr,
		@RequestParam(value = "ascending", required = false) final Boolean ascending);

	/**
	 * List the decision registered on a person in the given period.
	 */
	@GetMapping(path = "/apifc/v1/Decisions", produces = APPLICATION_JSON_VALUE)
	ApiPaginationCompositePersonBasedDecisionDTO getDecisions(
		@RequestParam("personId") final String personId,
		@RequestParam("startDate") final String startDate,
		@RequestParam("endDate") final String endDate,
		@RequestParam(value = "pageSize", required = false) final Integer pageSize,
		@RequestParam(value = "pageNr", required = false) final Integer pageNr,
		@RequestParam(value = "ascending", required = false) final Boolean ascending);

	/**
	 * List the payments registered on a person in the given period.
	 */
	@GetMapping(path = "/apifc/v1/Payments", produces = APPLICATION_JSON_VALUE)
	ApiPaginationCompositePersonBasedPaymentDTO getPayments(
		@RequestParam("personId") final String personId,
		@RequestParam("startDate") final String startDate,
		@RequestParam("endDate") final String endDate,
		@RequestParam(value = "pageSize", required = false) final Integer pageSize,
		@RequestParam(value = "pageNr", required = false) final Integer pageNr,
		@RequestParam(value = "ascending", required = false) final Boolean ascending);

	/**
	 * List the investigations registered on a person in the given period.
	 */
	@GetMapping(path = "/apifc/v1/Investigations", produces = APPLICATION_JSON_VALUE)
	ApiPaginationCompositePersonBasedInvestigationDTO getInvestigations(
		@RequestParam("personId") final String personId,
		@RequestParam("startDate") final String startDate,
		@RequestParam("endDate") final String endDate,
		@RequestParam(value = "pageSize", required = false) final Integer pageSize,
		@RequestParam(value = "pageNr", required = false) final Integer pageNr,
		@RequestParam(value = "ascending", required = false) final Boolean ascending);

	/**
	 * List the services registered on a person in the given period.
	 */
	@GetMapping(path = "/apifc/v1/Services", produces = APPLICATION_JSON_VALUE)
	ApiPaginationCompositePersonBasedServiceDTO getServices(
		@RequestParam("personId") final String personId,
		@RequestParam("startDate") final String startDate,
		@RequestParam("endDate") final String endDate,
		@RequestParam(value = "pageSize", required = false) final Integer pageSize,
		@RequestParam(value = "pageNr", required = false) final Integer pageNr,
		@RequestParam(value = "ascending", required = false) final Boolean ascending);

	/**
	 * List the executions registered on a person in the given period.
	 */
	@GetMapping(path = "/apifc/v1/Executions", produces = APPLICATION_JSON_VALUE)
	ApiPaginationCompositePersonBasedExecutionDTO getExecutions(
		@RequestParam("personId") final String personId,
		@RequestParam("startDate") final String startDate,
		@RequestParam("endDate") final String endDate,
		@RequestParam(value = "pageSize", required = false) final Integer pageSize,
		@RequestParam(value = "pageNr", required = false) final Integer pageNr,
		@RequestParam(value = "ascending", required = false) final Boolean ascending);

	/**
	 * List the resource allocations registered on a person in the given period.
	 */
	@GetMapping(path = "/apifc/v1/ResourceAllocations", produces = APPLICATION_JSON_VALUE)
	ApiPaginationCompositePersonBasedResourceAllocationDTO getResourceAllocations(
		@RequestParam("personId") final String personId,
		@RequestParam("startDate") final String startDate,
		@RequestParam("endDate") final String endDate,
		@RequestParam(value = "pageSize", required = false) final Integer pageSize,
		@RequestParam(value = "pageNr", required = false) final Integer pageNr,
		@RequestParam(value = "ascending", required = false) final Boolean ascending);

	/**
	 * List the documents registered on a person in the given period — metadata only (id, title, date, type, owner). The
	 * content of a single document is fetched separately via {@link #getDocumentContent(String)}.
	 */
	@GetMapping(path = "/apifc/v1/Documents", produces = APPLICATION_JSON_VALUE)
	ApiPaginationCompositePersonBasedDocumentDTO getDocuments(
		@RequestParam("personId") final String personId,
		@RequestParam("startDate") final String startDate,
		@RequestParam("endDate") final String endDate,
		@RequestParam(value = "pageSize", required = false) final Integer pageSize,
		@RequestParam(value = "pageNr", required = false) final Integer pageNr,
		@RequestParam(value = "ascending", required = false) final Boolean ascending);

	/**
	 * Fetch a single document's content (the generated PDF) by its document id. FC answers {@code 404} when the document
	 * has no generated PDF (content exists only for PDF-backed document types).
	 *
	 * @param  id the document id ({@code PersonBasedDocumentDTO.Id})
	 * @return    the raw document bytes (PDF)
	 */
	@GetMapping(path = "/apifc/v1/DocumentContent", produces = APPLICATION_PDF_VALUE)
	byte[] getDocumentContent(
		@RequestParam("id") final String id);

	/**
	 * List FC users (caseworkers). Used to resolve a Service's caseworker display name to the user's FC {@code Id}
	 * (the actualisation {@code CaseworkerId}) and {@code NetworkUserId} (the careM errand {@code assignedUserId}).
	 *
	 * @param  limit          the maximum number of users to return (required by FC)
	 * @param  offset         the starting point within the result set (optional)
	 * @param  modifiedAfter  only users modified after this UTC time (optional)
	 * @param  modifiedBefore only users modified before this UTC time (optional)
	 * @return                the matching users
	 */
	@GetMapping(path = "/apifc/v1/Users/GetUsers", produces = APPLICATION_JSON_VALUE)
	List<User> getUsers(
		@RequestParam("limit") final Integer limit,
		@RequestParam(value = "offset", required = false) final Integer offset,
		@RequestParam(value = "modifiedAfter", required = false) final String modifiedAfter,
		@RequestParam(value = "modifiedBefore", required = false) final String modifiedBefore);

	// ---- Write-back (actualisation + calculation) and the proposals that drive it ----------------------------------

	/**
	 * Fetch the proposal (valid code lists: types, reasons, fromWho, organisations, working status, investigation/service
	 * types, attachment types) needed to build a {@link PostAktualiseringsBodyRequest} for the given person.
	 *
	 * @param  personId the full personal identity number the actualisation concerns
	 * @return          the actualisation proposal lookups
	 */
	@GetMapping(path = "/apifc/v1/Actualisations/Proposals", produces = APPLICATION_JSON_VALUE)
	PersonBasedAktualiseringProposalDTO getActualisationProposal(
		@RequestParam("personId") final String personId);

	/**
	 * Create an actualisation (case intake) in Lifecare FC.
	 *
	 * @param  body the actualisation to create (codes resolved from {@link #getActualisationProposal(String)})
	 * @return      the id of the created actualisation
	 */
	@PostMapping(path = "/apifc/v1/Actualisations", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	Integer createActualisation(
		@RequestBody final PostAktualiseringsBodyRequest body);

	/**
	 * Fetch the proposal (norms, household members, income/expense/special-expense types, linkable
	 * investigations/services/actualisations) needed to build a {@link PostCalculationBodyRequest} for the given person.
	 *
	 * @param  personId the full personal identity number the calculation concerns
	 * @return          the calculation proposal lookups
	 */
	@GetMapping(path = "/apifc/v1/Calculations/Proposals", produces = APPLICATION_JSON_VALUE)
	PersonBasedCalculationProposalDTO getCalculationProposal(
		@RequestParam("personId") final String personId);

	/**
	 * Create a calculation (calculation) in Lifecare FC.
	 *
	 * @param  body the calculation to create (codes resolved from {@link #getCalculationProposal(String)})
	 * @return      the id of the created calculation
	 */
	@PostMapping(path = "/apifc/v1/Calculations", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	Integer createCalculation(
		@RequestBody final PostCalculationBodyRequest body);

	/**
	 * Upload a document and bind it to an actualisation. Multipart {@code form-data} (the {@code domain}/{@code key}
	 * query auth is added globally by {@link LifecareFcConfiguration}); FC answers {@code 204 No Content} on success.
	 *
	 * @param id                 the Lifecare actualisation id the document is bound to
	 * @param documentType       the {@code InsertDocumentType} code for the document
	 * @param documentSenderType the {@code InsertDocumentSenderType} code for the document
	 * @param title              the document title (optional)
	 * @param senderName         the sender name (optional)
	 * @param content            the file to attach
	 */
	@PostMapping(path = "/apifc/v1/Actualisations/{id}/Attachments", consumes = MULTIPART_FORM_DATA_VALUE)
	void postActualisationAttachment(
		@PathVariable final Integer id,
		@RequestPart("InsertDocumentType") final String documentType,
		@RequestPart("InsertDocumentSenderType") final String documentSenderType,
		@RequestPart(value = "Title", required = false) final String title,
		@RequestPart(value = "SenderName", required = false) final String senderName,
		@RequestPart("Content") final MultipartFile content);
}
