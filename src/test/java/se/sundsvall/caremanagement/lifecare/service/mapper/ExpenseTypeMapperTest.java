package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationExpenseTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationSpecialExpenseTypeDTO;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.lifecare.service.mapper.ExpenseTypeMapper.BUCKET_SPECIAL_EXPENSE;

class ExpenseTypeMapperTest {

	private static PersonBasedCalculationProposalDTO proposalWith(final PersonBasedCalculationExpenseTypeDTO... types) {
		return new PersonBasedCalculationProposalDTO().calculationExpenseTypes(List.of(types));
	}

	@Test
	void resolvesKnownCostTypeToMatchingExpenseTypeId() {
		final var proposal = proposalWith(
			new PersonBasedCalculationExpenseTypeDTO().id(42).name("Rent"),
			new PersonBasedCalculationExpenseTypeDTO().id(7).name("El"));

		final var result = ExpenseTypeMapper.resolveExpenseTypeId("RENT", proposal);

		assertThat(result).contains(42);
	}

	@Test
	void resolvesCaseInsensitively() {
		final var proposal = proposalWith(new PersonBasedCalculationExpenseTypeDTO().id(7).name("  el  "));

		final var result = ExpenseTypeMapper.resolveExpenseTypeId("ELECTRICITY", proposal);

		assertThat(result).contains(7);
	}

	@Test
	void returnsEmptyForUnmappedCostType() {
		final var proposal = proposalWith(new PersonBasedCalculationExpenseTypeDTO().id(42).name("Rent"));

		final var result = ExpenseTypeMapper.resolveExpenseTypeId("NONSENSE", proposal);

		assertThat(result).isEmpty();
	}

	@Test
	void returnsEmptyWhenMappedNameAbsentFromCatalogue() {
		final var proposal = proposalWith(new PersonBasedCalculationExpenseTypeDTO().id(7).name("El"));

		final var result = ExpenseTypeMapper.resolveExpenseTypeId("RENT", proposal);

		assertThat(result).isEmpty();
	}

	@Test
	void returnsEmptyWhenProposalExpenseTypesEmpty() {
		final var proposal = new PersonBasedCalculationProposalDTO().calculationExpenseTypes(List.of());

		final var result = ExpenseTypeMapper.resolveExpenseTypeId("RENT", proposal);

		assertThat(result).isEmpty();
	}

	@Test
	void returnsEmptyWhenProposalExpenseTypesNull() {
		final var result = ExpenseTypeMapper.resolveExpenseTypeId("RENT", new PersonBasedCalculationProposalDTO());

		assertThat(result).isEmpty();
	}

	@Test
	void specialExpenseBucketResolvesAgainstSpecialCatalogue() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.calculationExpenseTypes(List.of(new PersonBasedCalculationExpenseTypeDTO().id(7).name("El"))) // wrong catalogue
			.calculationSpecialExpenseTypes(List.of(new PersonBasedCalculationSpecialExpenseTypeDTO().id(88).name("Läkarvård")));

		assertThat(ExpenseTypeMapper.resolveExpenseTypeId("MEDICAL_CARE", proposal, BUCKET_SPECIAL_EXPENSE)).contains(88);
		// regular bucket would miss it (not in the regular catalogue)
		assertThat(ExpenseTypeMapper.resolveExpenseTypeId("MEDICAL_CARE", proposal)).isEmpty();
	}

	@Test
	void specialExpenseBucketEmptyWhenSpecialCatalogueMissingTheName() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.calculationSpecialExpenseTypes(List.of(new PersonBasedCalculationSpecialExpenseTypeDTO().id(88).name("Glasögon")));

		assertThat(ExpenseTypeMapper.resolveExpenseTypeId("MEDICAL_CARE", proposal, BUCKET_SPECIAL_EXPENSE)).isEmpty();
	}
}
