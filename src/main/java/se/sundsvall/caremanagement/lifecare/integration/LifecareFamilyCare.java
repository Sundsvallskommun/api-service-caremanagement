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

/**
 * The Lifecare FamilyCare surface careM depends on, separated from who answers it.
 *
 * <p>
 * There are two ways to reach FamilyCare from here, and which one is usable depends entirely on where the service runs.
 * {@code lifecare.sundsvall.se} has no public DNS record at all, so a deployment outside the municipal network — Drakel
 * on its Hetzner host, for one — cannot resolve it, let alone call it. {@code lifecare-ext.sundsvall.se} is reachable
 * from anywhere but answers {@code 401 "No client certificate."} without mTLS, which this client cannot do.
 * {@code api-service-lifecare-integrator} sits inside the network, reaches the internal host without a client
 * certificate, and is published on the WSO2 test gateway — so it is the route that works from outside.
 *
 * <p>
 * The interface exists so that choice stays a configuration decision rather than a code change. The method signatures
 * are FamilyCare's own: an implementation that talks to the integrator is responsible for translating its models back
 * into these, which keeps the ~180 references to FamilyCare DTOs across the {@code lifecare} package untouched. Pick an
 * implementation with {@code integration.lifecare-familycare.provider}.
 *
 * <p>
 * Every method leads with {@code municipalityId}. FamilyCare itself has no notion of one — the direct client talks to
 * a single Lifecare instance — but the integrator is an ordinary multi-tenant dept44 service that takes it in the path,
 * and it is also what {@code CitizenService} needs to turn a personal identity number into a {@code partyId}. Carrying
 * it on the interface keeps the tenant explicit rather than pinned in configuration.
 *
 * <p>
 * Every implementation is expected to hold the same two guarantees the direct one does: failures surface as
 * {@code BAD_GATEWAY} problems, and no {@code personId}, API key or payload is ever logged — FamilyCare carries
 * personal identity numbers and income data.
 */
public interface LifecareFamilyCare {

	/**
	 * Whether the {@code personId} fields in this route's <em>responses</em> hold a {@code partyId} rather than a
	 * personal identity number. Arguments are always personal identity numbers, on both routes; it is what comes back
	 * that differs, because the integrator is keyed on {@code partyId} and never emits a personnummer.
	 *
	 * <p>
	 * Anything that compares a person in a response against one it passed in, or resolves a person in a response
	 * further, has to know which of the two it is holding — see {@code LifecareCaseService.latestRoster}. Getting it
	 * wrong is silent: the comparison simply never matches.
	 */
	default boolean respondsWithPartyId() {
		return false;
	}

	// ---- Person-based reads ------------------------------------------------------------------------------------------

	PersonBasedPersonDTO getPerson(String municipalityId, String personId);

	List<PersonBasedContactDTO> getContacts(String municipalityId, String personId);

	ApiPaginationCompositePersonBasedAktualiseringDTO getActualisations(String municipalityId, String personId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedCalculationDTO getCalculations(String municipalityId, String personId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedDecisionDTO getDecisions(String municipalityId, String personId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedPaymentDTO getPayments(String municipalityId, String personId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedInvestigationDTO getInvestigations(String municipalityId, String personId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedServiceDTO getServices(String municipalityId, String personId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedExecutionDTO getExecutions(String municipalityId, String personId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedResourceAllocationDTO getResourceAllocations(String municipalityId, String personId, LocalDate startDate, LocalDate endDate);

	List<User> getUsers(String municipalityId, Integer limit, Integer offset, String modifiedAfter, String modifiedBefore);

	ApiPaginationCompositePersonBasedDocumentDTO getDocuments(String municipalityId, String personId, LocalDate startDate, LocalDate endDate);

	byte[] getDocumentContent(String municipalityId, String id);

	// ---- Write-back (actualisation + calculation) and the proposals that drive it ----------------------------------

	PersonBasedAktualiseringProposalDTO getActualisationProposal(String municipalityId, String personId);

	Integer createActualisation(String municipalityId, PostAktualiseringsBodyRequest body);

	PersonBasedCalculationProposalDTO getCalculationProposal(String municipalityId, String personId);

	/**
	 * Create a calculation. The body's own {@code personId} is the applicant's personal identity number, as everywhere
	 * else on this interface; the {@code calculationPersons} rows are the documented exception and carry a
	 * {@code partyId}, because that is the identity careM holds for a household member.
	 *
	 * <p>
	 * Each implementation translates what its route needs and nothing more: the integrator resolves the applicant to a
	 * party id, the direct client resolves the household rows to personal identity numbers. Neither ever has to undo
	 * the other's work, and nothing upstream of here has to know which route is wired.
	 */
	Integer createCalculation(String municipalityId, PostCalculationBodyRequest body);

	void postActualisationAttachment(String municipalityId, Integer actualisationId, String documentType, String documentSenderType,
		String title, String senderName, String fileName, byte[] content);
}
