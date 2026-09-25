package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationExpenseView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
		final var rejected = List.of(new ProposalMapper.PartialRejection("RENT", "Boendekostnad", new BigDecimal("9000"), new BigDecimal("1000")));

		assertThat(ProposalMapper.outcome(BigDecimal.ZERO, List.of())).isEqualTo("AVSLAG");
		assertThat(ProposalMapper.outcome(new BigDecimal("-1"), rejected)).isEqualTo("AVSLAG"); // amount wins over expenses
		assertThat(ProposalMapper.outcome(BigDecimal.ONE, List.of())).isEqualTo("BIFALL");
		assertThat(ProposalMapper.outcome(BigDecimal.ONE, rejected)).isEqualTo("DELAVSLAG");
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

		assertThat(ProposalMapper.partiallyRejectedExpenses(draft))
			.extracting(ProposalMapper.PartialRejection::sourceKey, rejection -> rejection.rejectedAmount().toPlainString())
			.containsExactly(tuple("RENT", "1000"), tuple("DENTAL_CARE", "2000"));
		assertThat(ProposalMapper.partiallyRejectedExpenses(CalculationDraft.create())).isEmpty();
	}

	@Test
	void partiallyRejectedExpensesOfTheSavedCalculation() {
		final var calculation = new CalculationView(31, null, null, null, null, null, null, null, null, null, null, null, false, List.of(), List.of(),
			List.of(
				// Approved in full by the caseworker in Lifecare, whatever the draft said.
				new CalculationExpenseView("Boendekostnad", new BigDecimal("9000"), new BigDecimal("9000")),
				// Two rows of one type are summed; the sign FamilyCare gives the amounts does not matter.
				new CalculationExpenseView("Hemförsäkring", new BigDecimal("-200"), new BigDecimal("-200")),
				new CalculationExpenseView("hemförsäkring ", new BigDecimal("150"), null),
				new CalculationExpenseView("Resor", null, BigDecimal.TEN)),
			List.of(new CalculationExpenseView("Tandvård", new BigDecimal("2000"), new BigDecimal("1500"))));

		assertThat(ProposalMapper.partiallyRejectedExpenses(calculation))
			.extracting(ProposalMapper.PartialRejection::label, rejection -> rejection.appliedAmount().toPlainString(), rejection -> rejection.rejectedAmount().toPlainString())
			.containsExactly(tuple("Hemförsäkring", "350", "150"), tuple("Tandvård", "2000", "500"));
		assertThat(ProposalMapper.partiallyRejectedExpenses(new CalculationView(31, null, null, null, null, null, null, null, null, null, null, null, false,
			List.of(), List.of(), null, null))).isEmpty();
	}

	@Test
	void lifecareExpenseSourceKeyReusesTheCostTypeCode() {
		assertThat(ProposalMapper.lifecareExpenseSourceKey("Boendekostnad")).isEqualTo("RENT");
		assertThat(ProposalMapper.lifecareExpenseSourceKey("boendekostnad")).isEqualTo("RENT");
		assertThat(ProposalMapper.lifecareExpenseSourceKey("Något Lifecare-eget")).isEqualTo("lifecare:något lifecare-eget");
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
	void isRecoveryClaimMatchesADecisionMotAterbetalning() {
		assertThat(ProposalMapper.isRecoveryClaim(decision("EK Bistånd mot återbetalning 9 kap 1 § SoL", null))).isTrue();
		assertThat(ProposalMapper.isRecoveryClaim(decision("EK ÅTERKRAV 9 kap 2 § SoL", null))).isTrue();
		// an eftergift forgives the claim, it is not one
		assertThat(ProposalMapper.isRecoveryClaim(decision("EK Bistånd som eftergift av återbetalning", null))).isFalse();
		assertThat(ProposalMapper.isRecoveryClaim(decision("Ek Ekonomiskt bistånd 12 kap 1, 7 §§ SoL, bifall", "Återbetalning av skuld"))).isFalse();
		assertThat(ProposalMapper.isRecoveryClaim(decision(null, null))).isFalse();
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
