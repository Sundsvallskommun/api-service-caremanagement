package se.sundsvall.caremanagement.types.financialassistance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;

/**
 * The errand-scoped facade for the per-row caseworker edits on the draft calculation (income / expense / person rows):
 * every call first scope-checks the errand (throws {@code 404} when it is missing in this namespace/municipality)
 * before
 * delegating to {@link DraftService}. Each edit touches only the caseworker value / note / soft-delete; the process
 * columns are owned by the daily prepare. Split out of the former FinancialAssistanceService god-service.
 */
@Service
@Transactional
public class FinancialAssistanceDraftRowService {

	private final ErrandService errandService;
	private final DraftService draftService;

	FinancialAssistanceDraftRowService(final ErrandService errandService, final DraftService draftService) {
		this.errandService = errandService;
		this.draftService = draftService;
	}

	public NormIncomeRow addDraftIncome(final String municipalityId, final String namespace, final String errandId, final NormIncomeInput input) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return draftService.addIncome(errandId, input);
	}

	public NormIncomeRow patchDraftIncome(final String municipalityId, final String namespace, final String errandId, final String rowId, final NormIncomeInput input) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.patchIncome(errandId, rowId, input);
	}

	public NormIncomeRow setDraftIncomeDeleted(final String municipalityId, final String namespace, final String errandId, final String rowId, final boolean deleted) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.setIncomeDeleted(errandId, rowId, deleted);
	}

	public NormExpenseRow addDraftExpense(final String municipalityId, final String namespace, final String errandId, final NormExpenseInput input) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.addExpense(errandId, input);
	}

	public NormExpenseRow patchDraftExpense(final String municipalityId, final String namespace, final String errandId, final String rowId, final NormExpenseInput input) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.patchExpense(errandId, rowId, input);
	}

	public NormExpenseRow setDraftExpenseDeleted(final String municipalityId, final String namespace, final String errandId, final String rowId, final boolean deleted) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.setExpenseDeleted(errandId, rowId, deleted);
	}

	public NormPersonRow addDraftPerson(final String municipalityId, final String namespace, final String errandId, final NormPersonInput input) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.addPerson(errandId, input);
	}

	public NormPersonRow patchDraftPerson(final String municipalityId, final String namespace, final String errandId, final String rowId, final NormPersonInput input) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.patchPerson(errandId, rowId, input);
	}

	public NormPersonRow setDraftPersonDeleted(final String municipalityId, final String namespace, final String errandId, final String rowId, final boolean deleted) {
		errandService.readErrand(municipalityId, namespace, errandId);
		return draftService.setPersonDeleted(errandId, rowId, deleted);
	}
}
