package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Cost;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Income;
import se.sundsvall.caremanagement.types.financialassistance.api.model.TypeOption;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialAssistanceTypesTest {

	@Test
	void cataloguesHaveExpectedSizes() {
		assertThat(FinancialAssistanceTypes.INCOME_TYPES).hasSize(33);
		assertThat(FinancialAssistanceTypes.COST_TYPES).hasSize(7);
		assertThat(FinancialAssistanceTypes.LIVING_COST_TYPES).hasSize(8);
	}

	@Test
	void everyOptionHasCodeAndDisplayName() {
		final var all = List.of(FinancialAssistanceTypes.INCOME_TYPES, FinancialAssistanceTypes.COST_TYPES, FinancialAssistanceTypes.LIVING_COST_TYPES);
		all.forEach(catalogue -> assertThat(catalogue).allSatisfy(option -> {
			assertThat(option.getCode()).isNotBlank();
			assertThat(option.getDisplayName()).isNotBlank();
		}));
	}

	@Test
	void codesAreUniqueAcrossAllCatalogues() {
		final var codes = allCodes();
		assertThat(codes).doesNotHaveDuplicates();
	}

	@Test
	void metadataAssemblesTheThreeCatalogues() {
		final var metadata = FinancialAssistanceTypes.metadata();

		assertThat(metadata.getIncomeTypes()).isEqualTo(FinancialAssistanceTypes.INCOME_TYPES);
		assertThat(metadata.getCostTypes()).isEqualTo(FinancialAssistanceTypes.COST_TYPES);
		assertThat(metadata.getLivingCostTypes()).isEqualTo(FinancialAssistanceTypes.LIVING_COST_TYPES);
	}

	@Test
	void citizenReportableIncomeTypesAreTheNonSsbtekSet() {
		final var reportable = FinancialAssistanceTypes.INCOME_TYPES.stream()
			.filter(TypeOption::isCitizenReportable)
			.map(TypeOption::getCode)
			.toList();

		// the Mina-sidor set: incomes that do NOT arrive via SSBTEK (FK / Pension / CSN / A-kassa / SKV)
		assertThat(reportable).containsExactlyInAnyOrder(
			"FINANCIAL_AID_OTHER_MUNICIPALITY", "RENT_SHARE_FROM_CHILD", "SALARY_AFTER_TAX",
			"SWISH_DEPOSITS_TRANSFERS", "OCCUPATIONAL_PENSION_INSURANCE", "CHILD_SUPPORT", "OTHER_INCOME");
	}

	@Test
	void allCostTypesAreCitizenReportable() {
		assertThat(FinancialAssistanceTypes.COST_TYPES).allMatch(TypeOption::isCitizenReportable);
		assertThat(FinancialAssistanceTypes.LIVING_COST_TYPES).allMatch(TypeOption::isCitizenReportable);
	}

	@Test
	void incomeCatalogueMatchesIncomeTypeValidation() throws NoSuchFieldException {
		final var allowed = Income.class.getDeclaredField("incomeType").getAnnotation(OneOf.class).value();

		assertThat(codesOf(FinancialAssistanceTypes.INCOME_TYPES)).containsExactlyInAnyOrder(allowed);
	}

	@Test
	void costCataloguesMatchCostTypeValidation() throws NoSuchFieldException {
		final var allowed = Cost.class.getDeclaredField("costType").getAnnotation(OneOf.class).value();
		final var costAndLivingCodes = java.util.stream.Stream.concat(
			codesOf(FinancialAssistanceTypes.COST_TYPES).stream(),
			codesOf(FinancialAssistanceTypes.LIVING_COST_TYPES).stream()).toList();

		assertThat(costAndLivingCodes).containsExactlyInAnyOrder(allowed);
	}

	private static List<String> allCodes() {
		return java.util.stream.Stream.of(FinancialAssistanceTypes.INCOME_TYPES, FinancialAssistanceTypes.COST_TYPES, FinancialAssistanceTypes.LIVING_COST_TYPES)
			.flatMap(List::stream)
			.map(TypeOption::getCode)
			.toList();
	}

	private static List<String> codesOf(final List<TypeOption> catalogue) {
		return catalogue.stream().map(TypeOption::getCode).toList();
	}
}
