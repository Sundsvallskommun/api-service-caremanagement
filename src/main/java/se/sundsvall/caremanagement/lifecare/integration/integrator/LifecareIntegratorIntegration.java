package se.sundsvall.caremanagement.lifecare.integration.integrator;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCare;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

/**
 * Serves the FamilyCare surface through {@code api-service-lifecare-integrator} instead of calling FamilyCare
 * directly. Selected with {@code integration.lifecare-familycare.provider=integrator}; the direct client remains the
 * default.
 *
 * <p>
 * This is the route that works from outside the municipal network — see {@link LifecareFamilyCare} for why the direct
 * one does not. The cost is a translation in both directions: the integrator is keyed on {@code partyId} while this
 * interface speaks FamilyCare's personal identity numbers, so every person-scoped call resolves the number to a party
 * id first, and the responses are mapped back into FamilyCare's DTOs so nothing downstream has to change.
 *
 * <p>
 * Operations that are not translated yet fail loudly with {@code NOT_IMPLEMENTED} rather than returning an empty
 * result. An empty calculation list is indistinguishable from "this person has no calculations", and quietly answering
 * that would put a wrong normberäkning in front of a handläggare.
 */
@Component
@ConditionalOnProperty(name = "integration.lifecare-familycare.provider", havingValue = "integrator")
public class LifecareIntegratorIntegration implements LifecareFamilyCare {

	private static final Logger LOG = LoggerFactory.getLogger(LifecareIntegratorIntegration.class);
	private static final String NOT_PORTED = "Operation '%s' is not available through the lifecare-integrator route yet";
	private static final String NO_PARTY_ID = "No party id could be resolved for the person";

	private final LifecareIntegratorClient client;
	private final CitizenService citizenService;

	public LifecareIntegratorIntegration(final LifecareIntegratorClient client, final CitizenService citizenService) {
		this.client = client;
		this.citizenService = citizenService;
	}

	/** Everything this route hands back identifies a person by {@code partyId}; see {@link IntegratorCaseMapper}. */
	@Override
	public boolean respondsWithPartyId() {
		return true;
	}

	@Override
	public ApiPaginationCompositePersonBasedCalculationDTO getCalculations(final String municipalityId, final String personId, final LocalDate startDate, final LocalDate endDate) {
		final var partyId = resolvePartyId(municipalityId, personId);
		return call("fetching calculations", () -> IntegratorCalculationMapper.toFamilyCare(
			client.getCalculations(municipalityId, partyId, startDate, endDate)));
	}

	@Override
	public ApiPaginationCompositePersonBasedDecisionDTO getDecisions(final String municipalityId, final String personId, final LocalDate startDate, final LocalDate endDate) {
		final var partyId = resolvePartyId(municipalityId, personId);
		return call("fetching decisions", () -> IntegratorCaseMapper.toDecisions(
			client.getDecisions(municipalityId, partyId, startDate, endDate)));
	}

	@Override
	public ApiPaginationCompositePersonBasedAktualiseringDTO getActualisations(final String municipalityId, final String personId, final LocalDate startDate, final LocalDate endDate) {
		final var partyId = resolvePartyId(municipalityId, personId);
		return call("fetching actualisations", () -> IntegratorCaseMapper.toActualisations(
			client.getActualisations(municipalityId, partyId, startDate, endDate), partyId));
	}

	@Override
	public ApiPaginationCompositePersonBasedPaymentDTO getPayments(final String municipalityId, final String personId, final LocalDate startDate, final LocalDate endDate) {
		final var partyId = resolvePartyId(municipalityId, personId);
		return call("fetching payments", () -> IntegratorCaseMapper.toPayments(
			client.getPayments(municipalityId, partyId, startDate, endDate)));
	}

	@Override
	public ApiPaginationCompositePersonBasedServiceDTO getServices(final String municipalityId, final String personId, final LocalDate startDate, final LocalDate endDate) {
		final var partyId = resolvePartyId(municipalityId, personId);
		return call("fetching services", () -> IntegratorCaseMapper.toServices(
			client.getServices(municipalityId, partyId, startDate, endDate)));
	}

	@Override
	public ApiPaginationCompositePersonBasedDocumentDTO getDocuments(final String municipalityId, final String personId, final LocalDate startDate, final LocalDate endDate) {
		final var partyId = resolvePartyId(municipalityId, personId);
		return call("fetching documents", () -> IntegratorCaseMapper.toDocuments(
			client.getDocuments(municipalityId, partyId, startDate, endDate)));
	}

	@Override
	public byte[] getDocumentContent(final String municipalityId, final String id) {
		return call("fetching document content", () -> client.getDocumentContent(municipalityId, id));
	}

	@Override
	public PersonBasedPersonDTO getPerson(final String municipalityId, final String personId) {
		final var partyId = resolvePartyId(municipalityId, personId);
		return call("fetching the person", () -> IntegratorCaseMapper.toPerson(client.getPerson(municipalityId, partyId), partyId));
	}

	@Override
	public List<PersonBasedContactDTO> getContacts(final String municipalityId, final String personId) {
		final var partyId = resolvePartyId(municipalityId, personId);
		return call("fetching contacts", () -> IntegratorCaseMapper.toContacts(client.getContacts(municipalityId, partyId)));
	}

	/** The caseworker directory is the one read that is not person-scoped, so there is no party id to resolve. */
	@Override
	public List<User> getUsers(final String municipalityId, final Integer limit, final Integer offset, final String modifiedAfter, final String modifiedBefore) {
		return call("fetching users", () -> IntegratorCaseMapper.toUsers(
			client.getUsers(municipalityId, limit, offset, modifiedAfter, modifiedBefore)));
	}

	// ---- Not translated yet ------------------------------------------------------------------------------------------
	//
	// Investigations, executions and resource allocations sit on the interface but are called from nowhere in careM,
	// so they are left alone rather than translated on spec. The proposals and the writes are the real remainder.

	@Override
	public ApiPaginationCompositePersonBasedInvestigationDTO getInvestigations(final String municipalityId, final String personId, final LocalDate startDate, final LocalDate endDate) {
		throw notPorted("getInvestigations");
	}

	@Override
	public ApiPaginationCompositePersonBasedExecutionDTO getExecutions(final String municipalityId, final String personId, final LocalDate startDate, final LocalDate endDate) {
		throw notPorted("getExecutions");
	}

	@Override
	public ApiPaginationCompositePersonBasedResourceAllocationDTO getResourceAllocations(final String municipalityId, final String personId, final LocalDate startDate, final LocalDate endDate) {
		throw notPorted("getResourceAllocations");
	}

	@Override
	public PersonBasedAktualiseringProposalDTO getActualisationProposal(final String municipalityId, final String personId) {
		throw notPorted("getActualisationProposal");
	}

	@Override
	public Integer createActualisation(final String municipalityId, final PostAktualiseringsBodyRequest body) {
		throw notPorted("createActualisation");
	}

	@Override
	public PersonBasedCalculationProposalDTO getCalculationProposal(final String municipalityId, final String personId) {
		throw notPorted("getCalculationProposal");
	}

	@Override
	public Integer createCalculation(final String municipalityId, final PostCalculationBodyRequest body) {
		throw notPorted("createCalculation");
	}

	@Override
	public void postActualisationAttachment(final String municipalityId, final Integer actualisationId, final String documentType, final String documentSenderType,
		final String title, final String senderName, final String fileName, final byte[] content) {
		throw notPorted("postActualisationAttachment");
	}

	// ---- Plumbing --------------------------------------------------------------------------------------------------

	private String resolvePartyId(final String municipalityId, final String personId) {
		return citizenService.getPartyId(municipalityId, personId)
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, NO_PARTY_ID));
	}

	private static ThrowableProblem notPorted(final String operation) {
		return Problem.valueOf(NOT_IMPLEMENTED, NOT_PORTED.formatted(operation));
	}

	/**
	 * Runs an integrator call, translating any failure into a {@code BAD_GATEWAY} problem. Mirrors the direct client's
	 * handling, including the reason it drops the cause's message for anything that is not already a clean problem: a
	 * transport failure embeds the request line, which here carries a party id and the query window.
	 */
	private <T> T call(final String action, final Supplier<T> operation) {
		try {
			return operation.get();
		} catch (final Exception e) {
			LOG.warn("lifecare-integrator failed while {}: {}", action, describe(e));
			throw Problem.valueOf(BAD_GATEWAY, "Lifecare integrator failed while %s (%s)".formatted(action, describe(e)));
		}
	}

	private static String describe(final Throwable e) {
		if (e instanceof final ThrowableProblem problem) {
			return ofNullable(problem.getStatus()).map(status -> status.value() + " " + problem.getMessage()).orElseGet(problem::getMessage);
		}
		return e.getClass().getSimpleName();
	}
}
