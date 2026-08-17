package se.sundsvall.caremanagement.lifecare.integration;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDocumentDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedExecutionDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedInvestigationDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedResourceAllocationDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedContactDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefamilycare.PostCalculationBodyRequest;
import generated.se.sundsvall.lifecarefamilycare.User;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Thin wrapper over {@link LifecareFamilyCareClient}. Every call goes through {@link #call(String, Supplier)}, which
 * translates any transport/FamilyCare failure into a {@code BAD_GATEWAY} problem carrying the upstream status into the
 * problem detail. Deliberately logs no {@code personId} or request/response payloads — FamilyCare carries personal
 * identity number and income data (sprint privacy rule, vof-ekonomiskt-bistand/CLAUDE.md).
 */
@Component
public class LifecareFamilyCareIntegration {

	/** Everything uploaded to an actualisation is a generated or uploaded PDF. */
	private static final String PDF_MIME_TYPE = "application/pdf";

	private final LifecareFamilyCareClient lifecareFamilyCareClient;

	public LifecareFamilyCareIntegration(final LifecareFamilyCareClient lifecareFamilyCareClient) {
		this.lifecareFamilyCareClient = lifecareFamilyCareClient;
	}

	// ---- Person-based reads ------------------------------------------------------------------------------------------

	/**
	 * Short upstream descriptor (HTTP status when available) to make failures self-diagnosing without leaking payloads.
	 * For {@link ThrowableProblem} causes the (already-clean) status + detail is used; for any other cause only the
	 * exception class name is emitted — transport failures (e.g. Feign {@code RetryableException}) embed the full
	 * request line in their message, which carries personal identity number and the FamilyCare API key, so the message is
	 * deliberately dropped.
	 */
	private static String describe(final Throwable e) {
		if (e instanceof final ThrowableProblem problem) {
			return ofNullable(problem.getStatus()).map(status -> status.value() + " " + problem.getMessage()).orElseGet(problem::getMessage);
		}
		return e.getClass().getSimpleName();
	}

	public PersonBasedPersonDTO getPerson(final String personId) {
		return call("fetching person", () -> lifecareFamilyCareClient.getPerson(personId));
	}

	public List<PersonBasedContactDTO> getContacts(final String personId) {
		return call("fetching contacts", () -> lifecareFamilyCareClient.getContacts(personId));
	}

	public ApiPaginationCompositePersonBasedAktualiseringDTO getActualisations(final String personId, final String startDate, final String endDate) {
		return call("fetching actualisations", () -> lifecareFamilyCareClient.getActualisations(personId, startDate, endDate, null, null, false));
	}

	public ApiPaginationCompositePersonBasedCalculationDTO getCalculations(final String personId, final String startDate, final String endDate) {
		return call("fetching calculations", () -> lifecareFamilyCareClient.getCalculations(personId, startDate, endDate, null, null, false));
	}

	public ApiPaginationCompositePersonBasedDecisionDTO getDecisions(final String personId, final String startDate, final String endDate) {
		return call("fetching decision", () -> lifecareFamilyCareClient.getDecisions(personId, startDate, endDate, null, null, false));
	}

	public ApiPaginationCompositePersonBasedPaymentDTO getPayments(final String personId, final String startDate, final String endDate) {
		return call("fetching payments", () -> lifecareFamilyCareClient.getPayments(personId, startDate, endDate, null, null, false));
	}

	public ApiPaginationCompositePersonBasedInvestigationDTO getInvestigations(final String personId, final String startDate, final String endDate) {
		return call("fetching investigations", () -> lifecareFamilyCareClient.getInvestigations(personId, startDate, endDate, null, null, false));
	}

	public ApiPaginationCompositePersonBasedServiceDTO getServices(final String personId, final String startDate, final String endDate) {
		return call("fetching services", () -> lifecareFamilyCareClient.getServices(personId, startDate, endDate, null, null, false));
	}

	public ApiPaginationCompositePersonBasedExecutionDTO getExecutions(final String personId, final String startDate, final String endDate) {
		return call("fetching executions", () -> lifecareFamilyCareClient.getExecutions(personId, startDate, endDate, null, null, false));
	}

	public ApiPaginationCompositePersonBasedResourceAllocationDTO getResourceAllocations(final String personId, final String startDate, final String endDate) {
		return call("fetching resource allocations", () -> lifecareFamilyCareClient.getResourceAllocations(personId, startDate, endDate, null, null, false));
	}

	public List<User> getUsers(final Integer limit, final Integer offset, final String modifiedAfter, final String modifiedBefore) {
		return call("fetching users", () -> lifecareFamilyCareClient.getUsers(limit, offset, modifiedAfter, modifiedBefore));
	}

	public ApiPaginationCompositePersonBasedDocumentDTO getDocuments(final String personId, final String startDate, final String endDate) {
		return call("fetching documents", () -> lifecareFamilyCareClient.getDocuments(personId, startDate, endDate, null, null, false));
	}

	// ---- Write-back (actualisation + calculation) and the proposals that drive it ----------------------------------

	public byte[] getDocumentContent(final String id) {
		return call("fetching document content", () -> lifecareFamilyCareClient.getDocumentContent(id));
	}

	public PersonBasedAktualiseringProposalDTO getActualisationProposal(final String personId) {
		return call("fetching actualisation proposal", () -> lifecareFamilyCareClient.getActualisationProposal(personId));
	}

	public Integer createActualisation(final PostAktualiseringsBodyRequest body) {
		return call("creating actualisation", () -> lifecareFamilyCareClient.createActualisation(body));
	}

	public PersonBasedCalculationProposalDTO getCalculationProposal(final String personId) {
		return call("fetching calculation proposal", () -> lifecareFamilyCareClient.getCalculationProposal(personId));
	}

	public Integer createCalculation(final PostCalculationBodyRequest body) {
		return call("creating calculation", () -> lifecareFamilyCareClient.createCalculation(body));
	}

	/**
	 * Upload a document and bind it to a Lifecare actualisation. The raw bytes are wrapped in an in-memory multipart
	 * {@code Content} part named after the file. Everything sent this way is a generated or uploaded PDF, so the part is
	 * typed as {@code application/pdf}. No payload is logged.
	 */
	public void postActualisationAttachment(final Integer actualisationId, final String documentType, final String documentSenderType,
		final String title, final String senderName, final String fileName, final byte[] content) {

		final var file = new ByteArrayMultipartFile("Content", fileName, PDF_MIME_TYPE, content);
		call("uploading actualisation attachment", () -> {
			lifecareFamilyCareClient.postActualisationAttachment(actualisationId, documentType, documentSenderType, title, senderName, file);
			return null;
		});
	}

	/**
	 * Runs a FamilyCare call, translating any failure into a {@code BAD_GATEWAY} problem. The {@code action} is a short
	 * verb phrase ("creating actualisation") used only for the log/problem detail — never a personId or payload.
	 */
	private <T> T call(final String action, final Supplier<T> operation) {
		try {
			return operation.get();
		} catch (final Exception e) {
			// Do not log the raw exception: transport failures embed the request URL, which carries the personId. The
			// thrown Problem's detail carries the (payload-free) upstream descriptor and is logged by the framework.
			throw Problem.valueOf(BAD_GATEWAY, "Error %s in Lifecare FamilyCare: %s".formatted(action, describe(e)));
		}
	}
}
