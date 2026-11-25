package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Cost;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Income;
import se.sundsvall.caremanagement.types.financialassistance.api.model.TypeOption;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class FinancialAssistanceTypesTest {

	@Test
	void cataloguesHaveExpectedSizes() {
		// 7 citizen + 26 handläggare-only income; 10 citizen + 5 handläggare-only cost
		// (INTERNET left the cost catalogue 2026-09-21 — the regelverk prices no internet cost)
		assertThat(FinancialAssistanceTypes.INCOME_TYPES).hasSize(33);
		assertThat(FinancialAssistanceTypes.COST_TYPES).hasSize(15);
	}

	@Test
	void everyOptionHasACodeAndAtLeastOneDisplayName() {
		allOptions().forEach(option -> {
			assertThat(option.getCode()).isNotBlank();
			assertThat(option.getExternalDisplayName() != null || option.getInternalDisplayName() != null).isTrue();
		});
	}

	@Test
	void codesAreUniqueWithinEachCatalogue() {
		assertThat(codesOf(FinancialAssistanceTypes.INCOME_TYPES)).doesNotHaveDuplicates();
		assertThat(codesOf(FinancialAssistanceTypes.COST_TYPES)).doesNotHaveDuplicates();
	}

	@Test
	void citizenReportableTypesHaveAnExternalName() {
		allOptions().stream().filter(TypeOption::isCitizenReportable).forEach(option -> assertThat(option.getExternalDisplayName()).isNotBlank());
	}

	@Test
	void handlaggareOnlyTypesHaveInternalNameNoExternalNoGroup() {
		final var handlaggareOnly = allOptions().stream().filter(option -> !option.isCitizenReportable()).toList();
		assertThat(handlaggareOnly).isNotEmpty()
			.allSatisfy(option -> {
				assertThat(option.getInternalDisplayName()).isNotBlank();
				assertThat(option.getExternalDisplayName()).isNull();
				assertThat(option.getGroup()).isNull();
			});
	}

	@Test
	void incomeTypesHaveNoGroup() {
		assertThat(FinancialAssistanceTypes.INCOME_TYPES).allSatisfy(option -> assertThat(option.getGroup()).isNull());
	}

	@Test
	void citizenReportableCostTypesAreGroupedIntoTheMinaSidorSections() {
		final var citizenCosts = FinancialAssistanceTypes.COST_TYPES.stream().filter(TypeOption::isCitizenReportable).toList();
		assertThat(citizenCosts).allSatisfy(option -> assertThat(option.getGroup()).isNotBlank());
		assertThat(citizenCosts).extracting(TypeOption::getGroup).containsOnly("HOUSING", "WORK_AND_STUDIES", "HEALTH", "OTHER");
	}

	@Test
	void metadataAssemblesTheCatalogues() {
		final var metadata = FinancialAssistanceTypes.metadata();

		assertThat(metadata.getIncomeTypes()).isEqualTo(FinancialAssistanceTypes.INCOME_TYPES);
		assertThat(metadata.getCostTypes()).isEqualTo(FinancialAssistanceTypes.COST_TYPES);
		assertThat(metadata.getMoneyTypes()).isEqualTo(FinancialAssistanceTypes.MONEY_TYPES);
		assertThat(metadata.getPaymentMethods()).isEqualTo(FinancialAssistanceTypes.PAYMENT_METHODS);
	}

	@Test
	void moneyTypesAreStillAPlaceholderUntilLifecareCatalogueIsKnown() {
		// Payment.moneyType stays an unconstrained string: verksamhetens answer of 2026-09-21 gave betalsätt but not
		// pengatyp, so this catalogue still starts empty rather than guessing at codes.
		assertThat(FinancialAssistanceTypes.MONEY_TYPES).isEmpty();
	}

	@Test
	void paymentMethodsCarryTheValueSetVerksamhetenSupplied() {
		// The live Lifecare dropdown as verksamheten screenshotted it 2026-09-21, in their order. Still a list and not
		// an enum, so a correction is an edit here rather than an API version.
		assertThat(FinancialAssistanceTypes.PAYMENT_METHODS)
			.extracting(TypeOption::getCode, TypeOption::getInternalDisplayName)
			.containsExactly(
				tuple("BANKGIRO_VIA_PLUSGIRO", "Bankgiro via Plusgiro"),
				tuple("BANKKONTO_VIA_PLUSGIRO", "Bankkonto via Plusgiro"),
				tuple("MEMORIAL", "Memorial"),
				tuple("PERSONKONTO", "Personkonto"),
				tuple("PLUSGIRO", "Plusgiro"));
	}

	@Test
	void citizenReportableIncomeCodesMatchIncomeTypeValidation() throws NoSuchFieldException {
		final var allowed = Income.class.getDeclaredField("incomeType").getAnnotation(OneOf.class).value();

		assertThat(citizenCodesOf(FinancialAssistanceTypes.INCOME_TYPES)).containsExactlyInAnyOrder(allowed);
	}

	@Test
	void citizenReportableCostCodesMatchCostTypeValidation() throws NoSuchFieldException {
		final var allowed = Cost.class.getDeclaredField("costType").getAnnotation(OneOf.class).value();

		assertThat(citizenCodesOf(FinancialAssistanceTypes.COST_TYPES)).containsExactlyInAnyOrder(allowed);
	}

	private static List<TypeOption> allOptions() {
		return Stream.concat(FinancialAssistanceTypes.INCOME_TYPES.stream(), FinancialAssistanceTypes.COST_TYPES.stream()).toList();
	}

	private static List<String> codesOf(final List<TypeOption> catalogue) {
		return catalogue.stream().map(TypeOption::getCode).toList();
	}

	private static List<String> citizenCodesOf(final List<TypeOption> catalogue) {
		return catalogue.stream().filter(TypeOption::isCitizenReportable).map(TypeOption::getCode).toList();
	}
}
