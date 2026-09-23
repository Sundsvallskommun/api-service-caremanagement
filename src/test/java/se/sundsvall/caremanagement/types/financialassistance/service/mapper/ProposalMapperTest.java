package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;

import static org.assertj.core.api.Assertions.assertThat;

class ProposalMapperTest {

	private static NormExpenseRow expense(final String costType, final String applied, final String effective) {
		return NormExpenseRow.create().withCostType(costType).withAppliedAmount(new BigDecimal(applied)).withEffectiveAmount(new BigDecimal(effective));
	}

	@Test
	void estimatedAmountIsNormPlusExpensesMinusIncomes() {
		final var draft = CalculationDraft.create().withIncomeSum(new BigDecimal("3000")).withExpenseSum(new BigDecimal("800")).withSpecialExpenseSum(new BigDecimal("250"));

		assertThat(ProposalMapper.estimatedAmount(draft, new BigDecimal("6200"))).isEqualByComparingTo("4250");
		assertThat(ProposalMapper.estimatedAmount(CalculationDraft.create(), new BigDecimal("6200"))).isEqualByComparingTo("6200"); // missing sums count as zero
	}

	@Test
	void outcomeRule() {
		final var rejected = List.of(expense("RENT", "9000", "8000"));

		assertThat(ProposalMapper.outcome(BigDecimal.ZERO, List.of())).isEqualTo("AVSLAG");
		assertThat(ProposalMapper.outcome(new BigDecimal("-1"), rejected)).isEqualTo("AVSLAG"); // amount wins over expenses
		assertThat(ProposalMapper.outcome(BigDecimal.ONE, List.of())).isEqualTo("BIFALL");
		assertThat(ProposalMapper.outcome(BigDecimal.ONE, rejected)).isEqualTo("DELAVSLAG");
	}

	@ParameterizedTest
	@CsvSource({
		"BIFALL, true, Bifall månad med barn",
		"BIFALL, false, Bifall månad utan barn",
		"DELAVSLAG, true, Bifall månad med barn",
		"AVSLAG, true,",
		", false,"
	})
	void phraseTextOnlyOnApprovedOutcomes(final String outcome, final boolean children, final String expected) {
		assertThat(ProposalMapper.phraseText(outcome, children)).isEqualTo(expected);
	}

	@Test
	void partiallyRejectedExpensesSkipsDeletedFullyApprovedAndUnapplied() {
		final var draft = CalculationDraft.create()
			.withExpenses(List.of(
				expense("RENT", "9000", "8000"),
				expense("INTERNET", "300", "300"),
				expense("ELECTRICITY", "500", "100").withDeleted(true),
				NormExpenseRow.create().withCostType("MEDICINE").withEffectiveAmount(BigDecimal.TEN)))
			.withSpecialExpenses(List.of(
				NormExpenseRow.create().withCostType("DENTAL_CARE").withAppliedAmount(new BigDecimal("2000")))); // no effective → 0 approved

		assertThat(ProposalMapper.partiallyRejectedExpenses(draft)).extracting(NormExpenseRow::getCostType).containsExactly("RENT", "DENTAL_CARE");
		assertThat(ProposalMapper.partiallyRejectedExpenses(CalculationDraft.create())).isEmpty();
	}

	@Test
	void childrenInCalculationNeedsAnIncludedLiveChildRow() {
		assertThat(ProposalMapper.childrenInCalculation(CalculationDraft.create().withPersons(List.of(
			NormPersonRow.create().withRole("APPLICANT").withIncluded(true))))).isFalse();
		assertThat(ProposalMapper.childrenInCalculation(CalculationDraft.create().withPersons(List.of(
			NormPersonRow.create().withRole("CHILD").withIncluded(false))))).isFalse();
		assertThat(ProposalMapper.childrenInCalculation(CalculationDraft.create().withPersons(List.of(
			NormPersonRow.create().withRole("CHILD").withIncluded(true).withDeleted(true))))).isFalse();
		assertThat(ProposalMapper.childrenInCalculation(CalculationDraft.create().withPersons(List.of(
			NormPersonRow.create().withRole("VISITATION_CHILD").withIncluded(true))))).isTrue();
		assertThat(ProposalMapper.childrenInCalculation(CalculationDraft.create())).isFalse();
	}

	@Test
	void expenseLabelAndSourceKey() {
		assertThat(ProposalMapper.expenseLabel(NormExpenseRow.create().withCostType("RENT"))).isEqualTo("Boendekostnad");
		assertThat(ProposalMapper.expenseLabel(NormExpenseRow.create().withCostType("OTHER").withOtherSubType("Busskort"))).isEqualTo("Övriga utgifter (Busskort)");
		assertThat(ProposalMapper.expenseLabel(NormExpenseRow.create().withCostType("MEDICINE").withSpecification("Apoteket"))).isEqualTo("Medicin (Apoteket)");
		assertThat(ProposalMapper.expenseLabel(NormExpenseRow.create().withCostType("UNKNOWN_CODE"))).isEqualTo("UNKNOWN_CODE");
		assertThat(ProposalMapper.expenseLabel(NormExpenseRow.create())).isEqualTo("Utgift");

		assertThat(ProposalMapper.expenseSourceKey(NormExpenseRow.create().withCostType("RENT"))).isEqualTo("RENT");
		assertThat(ProposalMapper.expenseSourceKey(NormExpenseRow.create().withCostType("OTHER").withOtherSubType("Busskort"))).isEqualTo("OTHER:Busskort");
		assertThat(ProposalMapper.expenseSourceKey(NormExpenseRow.create())).isEmpty();
	}

	@Test
	void plainStripsTrailingZeros() {
		assertThat(ProposalMapper.plain(new BigDecimal("8500.00"))).isEqualTo("8500");
		assertThat(ProposalMapper.plain(new BigDecimal("1250.50"))).isEqualTo("1250.5");
		assertThat(ProposalMapper.plain(null)).isEqualTo("0");
	}

	@Test
	void isAdvanceOnBenefitMatchesTypeOnlyCaseInsensitively() {
		assertThat(ProposalMapper.isAdvanceOnBenefit(decision("EK Förskott på förmån 12 Kap 1 § och 33 kap 2 § SoL, bifall", "Arbetslös, ingen ersättning/stöd"))).isTrue();
		assertThat(ProposalMapper.isAdvanceOnBenefit(decision("EK FÖRSKOTT på förmån, avslag", null))).isTrue();
		assertThat(ProposalMapper.isAdvanceOnBenefit(decision("Bifall", "Förskott på förmån"))).isFalse();
		assertThat(ProposalMapper.isAdvanceOnBenefit(decision("Bifall", "Försörjningsstöd"))).isFalse();
		assertThat(ProposalMapper.isAdvanceOnBenefit(decision(null, null))).isFalse();
	}

	private static DecisionView decision(final String type, final String reason) {
		return new DecisionView(1, "2026-04-28", type, "2026-05-01", "2026-05-31", reason, "Anna", "IFO", 2, new BigDecimal("8500"), "Astrid Testsson", "Hemarbetande", List.of());
	}

	@Test
	void toPreviousDecision() {
		final var result = ProposalMapper.toPreviousDecision(decision("Bifall", "Försörjningsstöd"));

		assertThat(result.getType()).isEqualTo("Bifall");
		assertThat(result.getReason()).isEqualTo("Försörjningsstöd");
		assertThat(result.getCoApplicant()).isEqualTo("Astrid Testsson");
		assertThat(result.getCoApplicantReason()).isEqualTo("Hemarbetande");
		assertThat(result.getPeriodFrom()).isEqualTo("2026-05-01");
		assertThat(result.getPeriodTo()).isEqualTo("2026-05-31");
		assertThat(result.getAmount()).isEqualByComparingTo("8500");
		assertThat(result.getDate()).isEqualTo("2026-04-28");
		assertThat(result).hasNoNullFieldsOrProperties();
	}
}
