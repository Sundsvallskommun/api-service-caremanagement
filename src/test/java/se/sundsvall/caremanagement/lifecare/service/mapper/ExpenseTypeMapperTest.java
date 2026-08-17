package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationExpenseTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationSpecialExpenseTypeDTO;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.lifecare.service.mapper.ExpenseTypeMapper.BUCKET_SPECIAL_EXPENSE;

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

	private static PersonBasedCalculationProposalDTO proposalWith(final PersonBasedCalculationExpenseTypeDTO... types) {
		return new PersonBasedCalculationProposalDTO().calculationExpenseTypes(List.of(types));
	}

	@Test
	void resolvesKnownCostTypeToMatchingExpenseTypeId() {
		final var proposal = proposalWith(
			new PersonBasedCalculationExpenseTypeDTO().id(42).name("Rent"),
			new PersonBasedCalculationExpenseTypeDTO().id(7).name("El"));

		final var result = ExpenseTypeMapper.resolveExpenseTypeId("RENT", proposal, null);

		assertThat(result).contains(42);
	}

	@Test
	void resolvesCaseInsensitively() {
		final var proposal = proposalWith(new PersonBasedCalculationExpenseTypeDTO().id(7).name("  el  "));

		final var result = ExpenseTypeMapper.resolveExpenseTypeId("ELECTRICITY", proposal, null);

		assertThat(result).contains(7);
	}

	@Test
	void returnsEmptyForUnmappedCostType() {
		final var proposal = proposalWith(new PersonBasedCalculationExpenseTypeDTO().id(42).name("Rent"));

		final var result = ExpenseTypeMapper.resolveExpenseTypeId("NONSENSE", proposal, null);

		assertThat(result).isEmpty();
	}

	@Test
	void returnsEmptyWhenMappedNameAbsentFromCatalogue() {
		final var proposal = proposalWith(new PersonBasedCalculationExpenseTypeDTO().id(7).name("El"));

		final var result = ExpenseTypeMapper.resolveExpenseTypeId("RENT", proposal, null);

		assertThat(result).isEmpty();
	}

	@Test
	void returnsEmptyWhenProposalExpenseTypesEmpty() {
		final var proposal = new PersonBasedCalculationProposalDTO().calculationExpenseTypes(List.of());

		final var result = ExpenseTypeMapper.resolveExpenseTypeId("RENT", proposal, null);

		assertThat(result).isEmpty();
	}

	@Test
	void returnsEmptyWhenProposalExpenseTypesNull() {
		final var result = ExpenseTypeMapper.resolveExpenseTypeId("RENT", new PersonBasedCalculationProposalDTO(), null);

		assertThat(result).isEmpty();
	}

	@Test
	void specialExpenseBucketResolvesAgainstSpecialCatalogue() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.calculationExpenseTypes(List.of(new PersonBasedCalculationExpenseTypeDTO().id(7).name("El"))) // wrong catalogue
			.calculationSpecialExpenseTypes(List.of(new PersonBasedCalculationSpecialExpenseTypeDTO().id(88).name("Läkarvård")));

		assertThat(ExpenseTypeMapper.resolveExpenseTypeId("MEDICAL_CARE", proposal, BUCKET_SPECIAL_EXPENSE)).contains(88);
		// regular bucket would miss it (not in the regular catalogue)
		assertThat(ExpenseTypeMapper.resolveExpenseTypeId("MEDICAL_CARE", proposal, null)).isEmpty();
	}

	@Test
	void specialExpenseBucketEmptyWhenSpecialCatalogueMissingTheName() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.calculationSpecialExpenseTypes(List.of(new PersonBasedCalculationSpecialExpenseTypeDTO().id(88).name("Glasögon")));

		assertThat(ExpenseTypeMapper.resolveExpenseTypeId("MEDICAL_CARE", proposal, BUCKET_SPECIAL_EXPENSE)).isEmpty();
	}
}
