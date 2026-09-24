package se.sundsvall.caremanagement.lifecare.service.mapper;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceTypes;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseTypeMapperTest {

	/**
	 * The two directions are written out by hand, so this locks them together: every forward entry must have a reverse
	 * entry keyed by the normalized name, and neither map may carry an entry the other lacks. Adding a cost type to one
	 * and forgetting the other fails here rather than silently dropping an amount when a previous calculation is read
	 * back.
	 */
	@Test
	void theCostTypeAndNameMapsAreExactInverses() {
		final var nameByCostType = ExpenseTypeMapper.familyCareNameByCostType();
		final var costTypeByName = ExpenseTypeMapper.costTypeByFamilyCareName();

		assertThat(costTypeByName).hasSameSizeAs(nameByCostType);
		assertThat(nameByCostType).allSatisfy((costType, name) -> assertThat(costTypeByName)
			.containsEntry(MapperUtil.normalize(name), costType));
	}

	@Test
	void namesAgreeWithTheFinancialAssistanceCostCatalogue() {
		// The Lifecare dropdown labels live in FinancialAssistanceTypes.COST_TYPES; this mapper cannot read them
		// (types.financialassistance already depends on lifecare, so that would be a module cycle), so the two are
		// held together here instead. They had drifted -- this mapper said "Rent"/"El"/"Bredband"/"A-kassa"/"Resor"/
		// "Ovrigt", none of which FamilyCare offers, and every one of those costs was dropped at commit.
		final var expected = FinancialAssistanceTypes.COST_TYPES.stream()
			.filter(option -> option.getInternalDisplayName() != null)
			.collect(java.util.stream.Collectors.toMap(option -> option.getCode(), option -> option.getInternalDisplayName()));

		assertThat(ExpenseTypeMapper.familyCareNameByCostType()).containsExactlyInAnyOrderEntriesOf(expected);
	}

	@Test
	void resolvesAFamilyCareNameToItsCostTypeIgnoringCaseAndSpace() {
		assertThat(ExpenseTypeMapper.costTypeForFamilyCareName("Boendekostnad")).contains("RENT");
		assertThat(ExpenseTypeMapper.costTypeForFamilyCareName("  el 1  ")).contains("ELECTRICITY");
	}

	@Test
	void anUnknownOrMissingFamilyCareNameResolvesToNothing() {
		assertThat(ExpenseTypeMapper.costTypeForFamilyCareName("Okänd kostnad")).isEmpty();
		assertThat(ExpenseTypeMapper.costTypeForFamilyCareName(null)).isEmpty();
	}
}
