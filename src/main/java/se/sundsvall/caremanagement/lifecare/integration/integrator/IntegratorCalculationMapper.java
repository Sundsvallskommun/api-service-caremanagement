package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.CommonCalculationExpenseDTO;
import generated.se.sundsvall.lifecarefamilycare.CommonCalculationIncomeDTO;
import generated.se.sundsvall.lifecarefamilycare.CommonCalculationSpecialExpenseDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationPersonDTO;
import generated.se.sundsvall.lifecareintegrator.Calculation;
import generated.se.sundsvall.lifecareintegrator.CalculationExpense;
import generated.se.sundsvall.lifecareintegrator.CalculationIncome;
import generated.se.sundsvall.lifecareintegrator.CalculationPerson;
import generated.se.sundsvall.lifecareintegrator.PagedCalculationResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * Translates the integrator's calculation models back into the FamilyCare DTOs the {@code lifecare} services consume.
 *
 * <p>
 * The translation is deliberately shallow: the two models carry the same fields under the same names, because the
 * integrator's are a projection of FamilyCare's. What differs is types — amounts are {@link BigDecimal} rather than
 * {@code Double}, dates are {@link LocalDate} rather than the strings FamilyCare renders — and one name, where
 * FamilyCare's {@code final} became {@code finalCalculation} to avoid the Java keyword.
 *
 * <p>
 * The one field that does not survive is the person's identity. FamilyCare puts a personal identity number on each
 * calculation person; the integrator replaces it with the party id it resolves to, and never emits the number. Callers
 * that need to tell household members apart should use {@code partyId} — the personal identity number simply is not
 * available on this route, by design.
 */
final class IntegratorCalculationMapper {

	private IntegratorCalculationMapper() {}

	static ApiPaginationCompositePersonBasedCalculationDTO toFamilyCare(final PagedCalculationResponse response) {
		final var composite = new ApiPaginationCompositePersonBasedCalculationDTO();
		composite.setResult(ofNullable(response)
			.map(PagedCalculationResponse::getCalculations)
			.orElseGet(List::of).stream()
			.map(IntegratorCalculationMapper::toCalculation)
			.toList());

		ofNullable(response).map(PagedCalculationResponse::getMeta).ifPresent(meta -> {
			composite.setPageNumber(meta.getPage());
			composite.setPageSize(meta.getLimit());
			composite.setTotalNumberOfPages(meta.getTotalPages());
			composite.setTotalNumberOfRecords(toInteger(meta.getTotalRecords()));
		});

		return composite;
	}

	private static PersonBasedCalculationDTO toCalculation(final Calculation calculation) {
		return new PersonBasedCalculationDTO()
			.id(calculation.getId())
			.norm(calculation.getNorm())
			.fromDate(toText(calculation.getFromDate()))
			.toDate(toText(calculation.getToDate()))
			.incomeSum(toDouble(calculation.getIncomeSum()))
			.expenseSum(toDouble(calculation.getExpenseSum()))
			.specialExpenseSum(toDouble(calculation.getSpecialExpenseSum()))
			.normSum(toDouble(calculation.getNormSum()))
			.commonHouseholdCost(toDouble(calculation.getCommonHouseholdCost()))
			.familyCost(toDouble(calculation.getFamilyCost()))
			.balance(toDouble(calculation.getBalance()))
			.totalSum(toDouble(calculation.getTotalSum()))
			.investigationId(calculation.getInvestigationId())
			.serviceId(calculation.getServiceId())
			._final(calculation.getFinalCalculation())
			.connectedApplication(calculation.getConnectedApplication())
			.calculationPersonDTOs(mapEach(calculation.getPersons(), IntegratorCalculationMapper::toPerson))
			.calculationIncomesDTOs(mapEach(calculation.getIncomes(), IntegratorCalculationMapper::toIncome))
			.calculationExpensesDTOs(mapEach(calculation.getExpenses(), IntegratorCalculationMapper::toExpense))
			.calculationSpecialExpensesDTOs(mapEach(calculation.getSpecialExpenses(), IntegratorCalculationMapper::toSpecialExpense));
	}

	/**
	 * The party id goes into {@code personId}. It is not a personal identity number and must not be treated as one, but
	 * it is what identifies the person on this route, and putting it in the field the services already read keeps the
	 * household roster working rather than silently emptying it.
	 */
	private static PersonBasedCalculationPersonDTO toPerson(final CalculationPerson person) {
		return new PersonBasedCalculationPersonDTO()
			.personId(person.getPartyId())
			.name(person.getName())
			.amount(toDouble(person.getAmount()))
			.deviationFromDate(toText(person.getDeviationFromDate()))
			.deviationToDate(toText(person.getDeviationToDate()));
	}

	private static CommonCalculationIncomeDTO toIncome(final CalculationIncome income) {
		return new CommonCalculationIncomeDTO()
			.type(income.getType())
			.amountApplicant(toDouble(income.getAmountApplicant()))
			.applicantSearchDate(toText(income.getApplicantSearchDate()))
			.amountCoApplicant(toDouble(income.getAmountCoApplicant()))
			.coApplicantSearchDate(toText(income.getCoApplicantSearchDate()));
	}

	private static CommonCalculationExpenseDTO toExpense(final CalculationExpense expense) {
		return new CommonCalculationExpenseDTO()
			.type(expense.getType())
			.appliedAmount(toDouble(expense.getAppliedAmount()))
			.approvedAmount(toDouble(expense.getApprovedAmount()));
	}

	private static CommonCalculationSpecialExpenseDTO toSpecialExpense(final CalculationExpense expense) {
		return new CommonCalculationSpecialExpenseDTO()
			.type(expense.getType())
			.appliedAmount(toDouble(expense.getAppliedAmount()))
			.approvedAmount(toDouble(expense.getApprovedAmount()));
	}

	private static <S, T> List<T> mapEach(final List<S> source, final java.util.function.Function<S, T> mapper) {
		return ofNullable(source).orElseGet(List::of).stream().map(mapper).toList();
	}

	private static Double toDouble(final BigDecimal value) {
		return ofNullable(value).map(BigDecimal::doubleValue).orElse(null);
	}

	private static String toText(final LocalDate date) {
		return ofNullable(date).map(LocalDate::toString).orElse(null);
	}

	private static Integer toInteger(final Long value) {
		return ofNullable(value).map(Long::intValue).orElse(null);
	}
}
