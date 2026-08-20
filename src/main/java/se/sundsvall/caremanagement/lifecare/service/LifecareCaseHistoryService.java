package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDocumentDTO;
import generated.se.sundsvall.lifecarefamilycare.CommonCalculationExpenseDTO;
import generated.se.sundsvall.lifecarefamilycare.CommonCalculationIncomeDTO;
import generated.se.sundsvall.lifecarefamilycare.CommonCalculationSpecialExpenseDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDocumentDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationExpenseView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationIncomeView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.lifecare.service.model.DocumentView;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.lifecare.service.mapper.MapperUtil.toAmount;

/**
 * Reads a person's Lifecare FamilyCare case history — calculations, decisions and documents — for the
 * caseworker-facing case view, and fetches a single document's content. Wraps {@link LifecareFamilyCareIntegration},
 * reducing the generated FamilyCare DTOs to the display projections in {@code lifecare.service.model} so the generated
 * types never leave the integration boundary.
 *
 * <p>
 * The list reads bound the FamilyCare query with ISO local dates; an empty/absent FamilyCare page maps to an empty
 * list. Calls propagate the integration's {@code BAD_GATEWAY} problem on failure — the caller decides whether to
 * treat the lookup as best-effort.
 * </p>
 */
@Service
public class LifecareCaseHistoryService {

	private final LifecareFamilyCareIntegration lifecareFamilyCareIntegration;

	public LifecareCaseHistoryService(final LifecareFamilyCareIntegration lifecareFamilyCareIntegration) {
		this.lifecareFamilyCareIntegration = lifecareFamilyCareIntegration;
	}

	/**
	 * List the calculations registered on a person in the given period, newest-first as Lifecare returns them.
	 *
	 * @param  personId the person's personal identity number (the calculation owner)
	 * @param  fromDate the inclusive start of the listing period
	 * @param  toDate   the inclusive end of the listing period
	 * @return          the person's calculations in the period (empty when none)
	 */
	public List<CalculationView> listCalculations(final String personId, final LocalDate fromDate, final LocalDate toDate) {
		return ofNullable(lifecareFamilyCareIntegration.getCalculations(personId, fromDate, toDate))
			.map(ApiPaginationCompositePersonBasedCalculationDTO::getResult)
			.orElseGet(List::of)
			.stream()
			.map(LifecareCaseHistoryService::toCalculation)
			.toList();
	}

	/**
	 * List the decisions registered on a person in the given period, newest-first as Lifecare returns them.
	 *
	 * @param  personId the person's personal identity number (the decision owner)
	 * @param  fromDate the inclusive start of the listing period
	 * @param  toDate   the inclusive end of the listing period
	 * @return          the person's decisions in the period (empty when none)
	 */
	public List<DecisionView> listDecisions(final String personId, final LocalDate fromDate, final LocalDate toDate) {
		return ofNullable(lifecareFamilyCareIntegration.getDecisions(personId, fromDate, toDate))
			.map(ApiPaginationCompositePersonBasedDecisionDTO::getResult)
			.orElseGet(List::of)
			.stream()
			.map(LifecareCaseHistoryService::toDecision)
			.toList();
	}

	/**
	 * List the documents registered on a person in the given period (metadata only), newest-first as Lifecare returns
	 * them.
	 *
	 * @param  personId the person's personal identity number (the document owner)
	 * @param  fromDate the inclusive start of the listing period
	 * @param  toDate   the inclusive end of the listing period
	 * @return          the person's documents in the period (empty when none)
	 */
	public List<DocumentView> listDocuments(final String personId, final LocalDate fromDate, final LocalDate toDate) {
		return ofNullable(lifecareFamilyCareIntegration.getDocuments(personId, fromDate, toDate))
			.map(ApiPaginationCompositePersonBasedDocumentDTO::getResult)
			.orElseGet(List::of)
			.stream()
			.map(LifecareCaseHistoryService::toDocument)
			.toList();
	}

	/**
	 * Fetch a single document's content (the generated PDF) by its document id.
	 *
	 * @param  id the document id ({@code DocumentView.id})
	 * @return    the raw document bytes (PDF)
	 */
	public byte[] documentContent(final String id) {
		return lifecareFamilyCareIntegration.getDocumentContent(id);
	}

	private static CalculationView toCalculation(final PersonBasedCalculationDTO dto) {
		return new CalculationView(
			dto.getId(),
			dto.getNorm(),
			dto.getFromDate(),
			dto.getToDate(),
			toAmount(dto.getIncomeSum()),
			toAmount(dto.getExpenseSum()),
			toAmount(dto.getSpecialExpenseSum()),
			toAmount(dto.getNormSum()),
			toAmount(dto.getCommonHouseholdCost()),
			toAmount(dto.getFamilyCost()),
			toAmount(dto.getBalance()),
			toAmount(dto.getTotalSum()),
			dto.getFinal(),
			ofNullable(dto.getCalculationPersonDTOs()).orElseGet(List::of).stream().map(LifecareCaseHistoryService::toCalculationPerson).toList(),
			ofNullable(dto.getCalculationIncomesDTOs()).orElseGet(List::of).stream().map(LifecareCaseHistoryService::toCalculationIncome).toList(),
			ofNullable(dto.getCalculationExpensesDTOs()).orElseGet(List::of).stream().map(LifecareCaseHistoryService::toCalculationExpense).toList(),
			ofNullable(dto.getCalculationSpecialExpensesDTOs()).orElseGet(List::of).stream().map(LifecareCaseHistoryService::toSpecialExpense).toList());
	}

	private static CalculationIncomeView toCalculationIncome(final CommonCalculationIncomeDTO dto) {
		return new CalculationIncomeView(dto.getType(), toAmount(dto.getAmountApplicant()), dto.getApplicantSearchDate(), toAmount(dto.getAmountCoApplicant()), dto.getCoApplicantSearchDate());
	}

	private static CalculationExpenseView toCalculationExpense(final CommonCalculationExpenseDTO dto) {
		return new CalculationExpenseView(dto.getType(), toAmount(dto.getAppliedAmount()), toAmount(dto.getApprovedAmount()));
	}

	private static CalculationExpenseView toSpecialExpense(final CommonCalculationSpecialExpenseDTO dto) {
		return new CalculationExpenseView(dto.getType(), toAmount(dto.getAppliedAmount()), toAmount(dto.getApprovedAmount()));
	}

	private static CalculationPersonView toCalculationPerson(final PersonBasedCalculationPersonDTO dto) {
		return new CalculationPersonView(dto.getPersonId(), dto.getName(), toAmount(dto.getAmount()), dto.getDeviationFromDate(), dto.getDeviationToDate());
	}

	private static DecisionView toDecision(final PersonBasedDecisionDTO dto) {
		return new DecisionView(
			dto.getId(),
			dto.getDate(),
			dto.getType(),
			dto.getFromDate(),
			dto.getToDate(),
			dto.getReason(),
			dto.getDecisionMaker(),
			dto.getOrganization(),
			toAmount(dto.getAmount()),
			dto.getCoApplicant(),
			dto.getReasonCoApplicant(),
			ofNullable(dto.getDecisionPersonDTOs()).orElseGet(List::of).stream().map(LifecareCaseHistoryService::toDecisionPerson).toList());
	}

	private static DecisionPersonView toDecisionPerson(final PersonBasedDecisionPersonDTO dto) {
		return new DecisionPersonView(dto.getPersonId(), dto.getName(), dto.getIsCoApplicant());
	}

	private static DocumentView toDocument(final PersonBasedDocumentDTO dto) {
		return new DocumentView(dto.getId(), dto.getTitle(), dto.getDate(), dto.getDocumentType(), dto.getOwnerId(), dto.getOwnerType());
	}
}
