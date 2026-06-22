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
		assertThat(FinancialAssistanceTypes.INCOME_TYPES).hasSize(7);
		assertThat(FinancialAssistanceTypes.COST_TYPES).hasSize(11);
	}

	@Test
	void everyOptionHasCodeExternalNameAndIsCitizenReportable() {
		final var all = java.util.stream.Stream.concat(FinancialAssistanceTypes.INCOME_TYPES.stream(), FinancialAssistanceTypes.COST_TYPES.stream()).toList();
		assertThat(all).allSatisfy(option -> {
			assertThat(option.getCode()).isNotBlank();
			assertThat(option.getExternalDisplayName()).isNotBlank();
			assertThat(option.isCitizenReportable()).isTrue();
		});
	}

	@Test
	void incomeTypesHaveNoGroup() {
		assertThat(FinancialAssistanceTypes.INCOME_TYPES).allSatisfy(option -> assertThat(option.getGroup()).isNull());
	}

	@Test
	void costTypesAreGroupedIntoTheMinaSidorSections() {
		assertThat(FinancialAssistanceTypes.COST_TYPES).allSatisfy(option -> assertThat(option.getGroup()).isNotBlank());
		assertThat(FinancialAssistanceTypes.COST_TYPES).extracting(TypeOption::getGroup).containsOnly(
			"Boende", "Arbete och studier", "Hälsa", "Övrigt");
	}

	@Test
	void metadataAssemblesTheCatalogues() {
		final var metadata = FinancialAssistanceTypes.metadata();

		assertThat(metadata.getIncomeTypes()).isEqualTo(FinancialAssistanceTypes.INCOME_TYPES);
		assertThat(metadata.getCostTypes()).isEqualTo(FinancialAssistanceTypes.COST_TYPES);
	}

	@Test
	void incomeCatalogueCodesMatchIncomeTypeValidation() throws NoSuchFieldException {
		final var allowed = Income.class.getDeclaredField("incomeType").getAnnotation(OneOf.class).value();

		assertThat(codesOf(FinancialAssistanceTypes.INCOME_TYPES)).containsExactlyInAnyOrder(allowed);
	}

	@Test
	void costCatalogueCodesMatchCostTypeValidation() throws NoSuchFieldException {
		final var allowed = Cost.class.getDeclaredField("costType").getAnnotation(OneOf.class).value();

		assertThat(codesOf(FinancialAssistanceTypes.COST_TYPES)).containsExactlyInAnyOrder(allowed);
	}

	private static List<String> codesOf(final List<TypeOption> catalogue) {
		return catalogue.stream().map(TypeOption::getCode).toList();
	}
}
