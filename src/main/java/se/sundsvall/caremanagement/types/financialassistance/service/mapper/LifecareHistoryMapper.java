package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.util.List;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationExpenseView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationIncomeView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.lifecare.service.model.DocumentView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareCalculation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareCalculationExpense;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareCalculationIncome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareCalculationPerson;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDecisionPerson;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDocument;

import static java.util.Optional.ofNullable;

/**
 * Projects the lifecare-module case-history records ({@code CalculationView}, {@code DecisionView},
 * {@code DocumentView} and their nested rows) onto the financial assistance API read models the frontend consumes.
 * Null-safe; nested
 * lists default to empty.
 */
public final class LifecareHistoryMapper {

	private LifecareHistoryMapper() {}

	public static LifecareCalculation toCalculation(final CalculationView view) {
		return LifecareCalculation.create()
			.withId(view.id())
			.withNorm(view.norm())
			.withFromDate(view.fromDate())
			.withToDate(view.toDate())
			.withIncomeSum(view.incomeSum())
			.withExpenseSum(view.expenseSum())
			.withSpecialExpenseSum(view.specialExpenseSum())
			.withNormSum(view.normSum())
			.withCommonHouseholdCost(view.commonHouseholdCost())
			.withFamilyCost(view.familyCost())
			.withBalance(view.balance())
			.withTotalSum(view.totalSum())
			.withIsFinal(view.isFinal())
			.withPersons(ofNullable(view.persons()).orElseGet(List::of).stream().map(LifecareHistoryMapper::toCalculationPerson).toList())
			.withIncomes(ofNullable(view.incomes()).orElseGet(List::of).stream().map(LifecareHistoryMapper::toCalculationIncome).toList())
			.withExpenses(ofNullable(view.expenses()).orElseGet(List::of).stream().map(LifecareHistoryMapper::toCalculationExpense).toList())
			.withSpecialExpenses(ofNullable(view.specialExpenses()).orElseGet(List::of).stream().map(LifecareHistoryMapper::toCalculationExpense).toList());
	}

	public static LifecareDecision toDecision(final DecisionView view) {
		return LifecareDecision.create()
			.withId(view.id())
			.withDate(view.date())
			.withType(view.type())
			.withFromDate(view.fromDate())
			.withToDate(view.toDate())
			.withReason(view.reason())
			.withDecisionMaker(view.decisionMaker())
			.withOrganization(view.organization())
			.withAmount(view.amount())
			.withCoApplicant(view.coApplicant())
			.withReasonCoApplicant(view.reasonCoApplicant())
			.withPersons(ofNullable(view.persons()).orElseGet(List::of).stream().map(LifecareHistoryMapper::toDecisionPerson).toList());
	}

	public static LifecareDocument toDocument(final DocumentView view) {
		return LifecareDocument.create()
			.withId(view.id())
			.withTitle(view.title())
			.withDate(view.date())
			.withDocumentType(view.documentType())
			.withOwnerId(view.ownerId())
			.withOwnerType(view.ownerType());
	}

	private static LifecareCalculationIncome toCalculationIncome(final CalculationIncomeView view) {
		return LifecareCalculationIncome.create()
			.withType(view.type())
			.withAmountApplicant(view.amountApplicant())
			.withApplicantSearchDate(view.applicantSearchDate())
			.withAmountCoApplicant(view.amountCoApplicant())
			.withCoApplicantSearchDate(view.coApplicantSearchDate());
	}

	private static LifecareCalculationExpense toCalculationExpense(final CalculationExpenseView view) {
		return LifecareCalculationExpense.create()
			.withType(view.type())
			.withAppliedAmount(view.appliedAmount())
			.withApprovedAmount(view.approvedAmount());
	}

	private static LifecareCalculationPerson toCalculationPerson(final CalculationPersonView view) {
		return LifecareCalculationPerson.create()
			.withPersonId(view.personId())
			.withName(view.name())
			.withAmount(view.amount())
			.withDeviationFromDate(view.deviationFromDate())
			.withDeviationToDate(view.deviationToDate());
	}

	private static LifecareDecisionPerson toDecisionPerson(final DecisionPersonView view) {
		return LifecareDecisionPerson.create()
			.withPersonId(view.personId())
			.withName(view.name())
			.withCoApplicant(view.coApplicant());
	}
}
