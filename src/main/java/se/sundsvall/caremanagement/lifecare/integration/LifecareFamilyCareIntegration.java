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
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.lifecare.integration.FamilyCareDates.endOfDay;
import static se.sundsvall.caremanagement.lifecare.integration.FamilyCareDates.startOfDay;

/**
 * Thin wrapper over {@link LifecareFamilyCareClient}. Every call goes through {@link #call(String, Supplier)}, which
 * translates any transport/FamilyCare failure into a {@code BAD_GATEWAY} problem carrying the upstream status into the
 * problem detail. Deliberately logs no {@code personId} or request/response payloads — FamilyCare carries personal
 * identity number and income data (sprint privacy rule, vof-ekonomiskt-bistand/CLAUDE.md).
 *
 * <p>
 * Arguments are party ids ({@link LifecareFamilyCare}); FamilyCare keys on the personal identity number, so every
 * person-scoped call resolves it through the citizen service first — the one place {@code municipalityId} is used
 * here, since FamilyCare has no tenant in its API.
 *
 * <p>
 * The period reads take the window as {@link LocalDate}s and render them here, through {@link FamilyCareDates}, so no
 * caller can hand FamilyCare a date in a format it rejects. The window is inclusive in both ends: the start date
 * becomes start of day and the end date end of day.
 */
@Component
@ConditionalOnProperty(name = "integration.lifecare-familycare.provider", havingValue = "familycare", matchIfMissing = true)
public class LifecareFamilyCareIntegration implements LifecareFamilyCare {

	/** Everything uploaded to an actualisation is a generated or uploaded PDF. */
	private static final String PDF_MIME_TYPE = "application/pdf";

	private static final String NO_PERSONAL_NUMBER = "No personal identity number could be resolved for a person on the calculation";
	private static final String NO_CITIZEN = "No citizen found for partyId %s";

	private final LifecareFamilyCareClient lifecareFamilyCareClient;
	private final CitizenService citizenService;

	public LifecareFamilyCareIntegration(final LifecareFamilyCareClient lifecareFamilyCareClient, final CitizenService citizenService) {
		this.lifecareFamilyCareClient = lifecareFamilyCareClient;
		this.citizenService = citizenService;
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

	@Override
	public PersonBasedPersonDTO getPerson(final String municipalityId, final String partyId) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching person", () -> lifecareFamilyCareClient.getPerson(personalNumber));
	}

	@Override
	public List<PersonBasedContactDTO> getContacts(final String municipalityId, final String partyId) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching contacts", () -> lifecareFamilyCareClient.getContacts(personalNumber));
	}

	@Override
	public ApiPaginationCompositePersonBasedAktualiseringDTO getActualisations(final String municipalityId, final String partyId, final LocalDate startDate, final LocalDate endDate) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching actualisations", () -> lifecareFamilyCareClient.getActualisations(personalNumber, startOfDay(startDate), endOfDay(endDate), null, null, false));
	}

	@Override
	public ApiPaginationCompositePersonBasedCalculationDTO getCalculations(final String municipalityId, final String partyId, final LocalDate startDate, final LocalDate endDate) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching calculations", () -> lifecareFamilyCareClient.getCalculations(personalNumber, startOfDay(startDate), endOfDay(endDate), null, null, false));
	}

	@Override
	public ApiPaginationCompositePersonBasedDecisionDTO getDecisions(final String municipalityId, final String partyId, final LocalDate startDate, final LocalDate endDate) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching decision", () -> lifecareFamilyCareClient.getDecisions(personalNumber, startOfDay(startDate), endOfDay(endDate), null, null, false));
	}

	@Override
	public ApiPaginationCompositePersonBasedPaymentDTO getPayments(final String municipalityId, final String partyId, final LocalDate startDate, final LocalDate endDate) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching payments", () -> lifecareFamilyCareClient.getPayments(personalNumber, startOfDay(startDate), endOfDay(endDate), null, null, false));
	}

	@Override
	public ApiPaginationCompositePersonBasedInvestigationDTO getInvestigations(final String municipalityId, final String partyId, final LocalDate startDate, final LocalDate endDate) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching investigations", () -> lifecareFamilyCareClient.getInvestigations(personalNumber, startOfDay(startDate), endOfDay(endDate), null, null, false));
	}

	@Override
	public ApiPaginationCompositePersonBasedServiceDTO getServices(final String municipalityId, final String partyId, final LocalDate startDate, final LocalDate endDate) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching services", () -> lifecareFamilyCareClient.getServices(personalNumber, startOfDay(startDate), endOfDay(endDate), null, null, false));
	}

	@Override
	public ApiPaginationCompositePersonBasedExecutionDTO getExecutions(final String municipalityId, final String partyId, final LocalDate startDate, final LocalDate endDate) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching executions", () -> lifecareFamilyCareClient.getExecutions(personalNumber, startOfDay(startDate), endOfDay(endDate), null, null, false));
	}

	@Override
	public ApiPaginationCompositePersonBasedResourceAllocationDTO getResourceAllocations(final String municipalityId, final String partyId, final LocalDate startDate, final LocalDate endDate) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching resource allocations", () -> lifecareFamilyCareClient.getResourceAllocations(personalNumber, startOfDay(startDate), endOfDay(endDate), null, null, false));
	}

	@Override
	public List<User> getUsers(final String municipalityId, final Integer limit, final Integer offset, final String modifiedAfter, final String modifiedBefore) {
		return call("fetching users", () -> lifecareFamilyCareClient.getUsers(limit, offset, modifiedAfter, modifiedBefore));
	}

	@Override
	public ApiPaginationCompositePersonBasedDocumentDTO getDocuments(final String municipalityId, final String partyId, final LocalDate startDate, final LocalDate endDate) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching documents", () -> lifecareFamilyCareClient.getDocuments(personalNumber, startOfDay(startDate), endOfDay(endDate), null, null, false));
	}

	// ---- Write-back (actualisation + calculation) and the proposals that drive it ----------------------------------

	@Override
	public byte[] getDocumentContent(final String municipalityId, final String id) {
		return call("fetching document content", () -> lifecareFamilyCareClient.getDocumentContent(id));
	}

	@Override
	public PersonBasedAktualiseringProposalDTO getActualisationProposal(final String municipalityId, final String partyId) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching actualisation proposal", () -> lifecareFamilyCareClient.getActualisationProposal(personalNumber));
	}

	@Override
	public Integer createActualisation(final String municipalityId, final PostAktualiseringsBodyRequest body) {
		body.setPersonId(personalNumber(municipalityId, body.getPersonId()));
		return call("creating actualisation", () -> lifecareFamilyCareClient.createActualisation(body));
	}

	@Override
	public PersonBasedCalculationProposalDTO getCalculationProposal(final String municipalityId, final String partyId) {
		final var personalNumber = personalNumber(municipalityId, partyId);
		return call("fetching calculation proposal", () -> lifecareFamilyCareClient.getCalculationProposal(personalNumber));
	}

	/**
	 * Create the calculation, first resolving the applicant and every household row from a party id to the personal
	 * identity number FamilyCare keys on (confirmed with Tieto 2026-09-22 — every {@code PersonId} in the FamilyCare
	 * API is a personal identity number, including the one on {@code CalculationPersons}).
	 *
	 * <p>
	 * The body is careM's own, built per call in {@code CalculationService.commitEffective} and used nowhere else, so
	 * it is rewritten in place rather than copied.
	 *
	 * <p>
	 * A row that does not resolve fails the whole calculation. Skipping it would silently shrink the household the
	 * norm is computed from — a family of four paid as three, with nothing on the errand saying why.
	 */
	@Override
	public Integer createCalculation(final String municipalityId, final PostCalculationBodyRequest body) {
		body.setPersonId(personalNumber(municipalityId, body.getPersonId()));
		resolveHouseholdPersonIds(municipalityId, body);
		return call("creating calculation", () -> lifecareFamilyCareClient.createCalculation(body));
	}

	private void resolveHouseholdPersonIds(final String municipalityId, final PostCalculationBodyRequest body) {
		ofNullable(body.getCalculationPersons()).orElseGet(List::of)
			.forEach(person -> person.setPersonId(resolvePersonalNumber(municipalityId, person.getPersonId())));
	}

	/**
	 * The personal identity number behind a person-scoped argument. Resolved before the FamilyCare call rather than
	 * inside it, so a citizen-service failure surfaces as itself instead of being reported as a Lifecare one.
	 */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NO_CITIZEN.formatted(partyId)));
	}

	private String resolvePersonalNumber(final String municipalityId, final String partyId) {
		if (!hasText(partyId)) {
			throw Problem.valueOf(BAD_GATEWAY, NO_PERSONAL_NUMBER);
		}
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, NO_PERSONAL_NUMBER));
	}

	/**
	 * Upload a document and bind it to a Lifecare actualisation. The raw bytes are wrapped in an in-memory multipart
	 * {@code Content} part named after the file. Everything sent this way is a generated or uploaded PDF, so the part is
	 * typed as {@code application/pdf}. No payload is logged.
	 */
	@Override
	public void postActualisationAttachment(final String municipalityId, final Integer actualisationId, final String documentType, final String documentSenderType,
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
