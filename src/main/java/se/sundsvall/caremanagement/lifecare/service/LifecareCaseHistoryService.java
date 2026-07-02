package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDocumentDTO;
import generated.se.sundsvall.lifecarefc.CommonCalculationExpenseDTO;
import generated.se.sundsvall.lifecarefc.CommonCalculationIncomeDTO;
import generated.se.sundsvall.lifecarefc.CommonCalculationSpecialExpenseDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationPersonDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDecisionPersonDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDocumentDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationExpenseView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationIncomeView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.lifecare.service.model.DocumentView;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Optional.ofNullable;

/**
 * Reads a person's Lifecare FC case history — calculations, decisions and documents — for the caseworker-facing case
 * view, and fetches a single document's content. Wraps {@link LifecareFcIntegration}, reducing the generated FC DTOs to
 * the display projections in {@code lifecare.service.model} so the generated types never leave the integration
 * boundary.
 *
 * <p>
 * The list reads bound the FC query with ISO local dates; an empty/absent FC page maps to an empty list. Calls
 * propagate the integration's {@code BAD_GATEWAY} problem on failure — the caller decides whether to treat the lookup
 * as
 * best-effort.
 * </p>
 */
@Service
public class LifecareCaseHistoryService {

	private final LifecareFcIntegration lifecareFcIntegration;

	public LifecareCaseHistoryService(final LifecareFcIntegration lifecareFcIntegration) {
		this.lifecareFcIntegration = lifecareFcIntegration;
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
		return ofNullable(lifecareFcIntegration.getCalculations(personId, fromDate.format(ISO_LOCAL_DATE), toDate.format(ISO_LOCAL_DATE)))
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
		return ofNullable(lifecareFcIntegration.getDecisions(personId, fromDate.format(ISO_LOCAL_DATE), toDate.format(ISO_LOCAL_DATE)))
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
		return ofNullable(lifecareFcIntegration.getDocuments(personId, fromDate.format(ISO_LOCAL_DATE), toDate.format(ISO_LOCAL_DATE)))
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
		return lifecareFcIntegration.getDocumentContent(id);
	}

	private static CalculationView toCalculation(final PersonBasedCalculationDTO dto) {
		return new CalculationView(
			dto.getId(),
			dto.getNorm(),
			dto.getFromDate(),
			dto.getToDate(),
			dto.getIncomeSum(),
			dto.getExpenseSum(),
			dto.getSpecialExpenseSum(),
			dto.getNormSum(),
			dto.getCommonHouseholdCost(),
			dto.getFamilyCost(),
			dto.getBalance(),
			dto.getTotalSum(),
			dto.getFinal(),
			ofNullable(dto.getCalculationPersonDTOs()).orElseGet(List::of).stream().map(LifecareCaseHistoryService::toCalculationPerson).toList(),
			ofNullable(dto.getCalculationIncomesDTOs()).orElseGet(List::of).stream().map(LifecareCaseHistoryService::toCalculationIncome).toList(),
			ofNullable(dto.getCalculationExpensesDTOs()).orElseGet(List::of).stream().map(LifecareCaseHistoryService::toCalculationExpense).toList(),
			ofNullable(dto.getCalculationSpecialExpensesDTOs()).orElseGet(List::of).stream().map(LifecareCaseHistoryService::toSpecialExpense).toList());
	}

	private static CalculationIncomeView toCalculationIncome(final CommonCalculationIncomeDTO dto) {
		return new CalculationIncomeView(dto.getType(), dto.getAmountApplicant(), dto.getApplicantSearchDate(), dto.getAmountCoApplicant(), dto.getCoApplicantSearchDate());
	}

	private static CalculationExpenseView toCalculationExpense(final CommonCalculationExpenseDTO dto) {
		return new CalculationExpenseView(dto.getType(), dto.getAppliedAmount(), dto.getApprovedAmount());
	}

	private static CalculationExpenseView toSpecialExpense(final CommonCalculationSpecialExpenseDTO dto) {
		return new CalculationExpenseView(dto.getType(), dto.getAppliedAmount(), dto.getApprovedAmount());
	}

	private static CalculationPersonView toCalculationPerson(final PersonBasedCalculationPersonDTO dto) {
		return new CalculationPersonView(dto.getPersonId(), dto.getName(), dto.getAmount(), dto.getDeviationFromDate(), dto.getDeviationToDate());
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
			dto.getAmount(),
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
