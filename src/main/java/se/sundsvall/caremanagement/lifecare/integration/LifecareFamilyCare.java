package se.sundsvall.caremanagement.lifecare.integration;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDocumentDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefamilycare.PostCalculationBodyRequest;
import generated.se.sundsvall.lifecarefamilycare.User;
import java.time.LocalDate;
import java.util.List;
import se.sundsvall.caremanagement.lifecare.service.AttachmentUpload;

/**
 * The Lifecare FamilyCare surface careM depends on, served through lifecare-integrator. The types are FamilyCare's own
 * DTOs, so the {@code lifecare} services do not depend on the integrator's models. Every person-scoped argument, and
 * every {@code personId} in a response, is a {@code partyId}.
 *
 * <p>
 * Failures surface as {@code BAD_GATEWAY} problems, and no {@code personId} or payload is ever logged — FamilyCare
 * carries income data.
 */
public interface LifecareFamilyCare {

	// ---- Person-based reads ------------------------------------------------------------------------------------------

	/**
	 * The person's FamilyCare master data.
	 *
	 * @return the person, or {@code null} when Lifecare holds no such person (a first-time applicant). Every other failure
	 *         is a {@code BAD_GATEWAY} problem.
	 */
	PersonBasedPersonDTO getPerson(String municipalityId, String partyId);

	ApiPaginationCompositePersonBasedAktualiseringDTO getActualisations(String municipalityId, String partyId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedCalculationDTO getCalculations(String municipalityId, String partyId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedDecisionDTO getDecisions(String municipalityId, String partyId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedPaymentDTO getPayments(String municipalityId, String partyId, LocalDate startDate, LocalDate endDate);

	ApiPaginationCompositePersonBasedServiceDTO getServices(String municipalityId, String partyId, LocalDate startDate, LocalDate endDate);

	List<User> getUsers(String municipalityId, Integer limit, Integer offset, String modifiedAfter, String modifiedBefore);

	ApiPaginationCompositePersonBasedDocumentDTO getDocuments(String municipalityId, String partyId, LocalDate startDate, LocalDate endDate);

	byte[] getDocumentContent(String municipalityId, String id);

	// ---- Write-back (actualisation + calculation) and the proposals that drive it ----------------------------------

	PersonBasedAktualiseringProposalDTO getActualisationProposal(String municipalityId, String partyId);

	Integer createActualisation(String municipalityId, PostAktualiseringsBodyRequest body);

	PersonBasedCalculationProposalDTO getCalculationProposal(String municipalityId, String partyId);

	/**
	 * Create a calculation. The body's own {@code personId} and every {@code calculationPersons} row carry a
	 * {@code partyId}, as everywhere else on this interface.
	 */
	Integer createCalculation(String municipalityId, PostCalculationBodyRequest body);

	void postActualisationAttachment(String municipalityId, Integer actualisationId, AttachmentUpload attachment);
}
