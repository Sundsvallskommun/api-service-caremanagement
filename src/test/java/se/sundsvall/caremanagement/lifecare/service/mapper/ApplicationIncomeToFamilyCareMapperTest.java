package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ApplicationIncomeToFamilyCareMapperTest {

	private static PersonBasedCalculationProposalDTO proposal() {
		return new PersonBasedCalculationProposalDTO()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(30).name("Swish/Insättningar/Överföringar"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(10).name("Lön efter skatt"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(40).name("Övriga inkomster"));
	}

	@Test
	void declaredIncomesResolveToTheProposalsTypesAndSumPerTypeAndRecipient() {
		final var incomes = List.of(
			new ApplicationIncome("SWISH_DEPOSITS", null, BigDecimal.valueOf(599), LocalDate.of(2026, 9, 24), "Swish/kontoinsättningar"),
			new ApplicationIncome("OTHER_INCOME", "APPLICANT", BigDecimal.valueOf(100), LocalDate.of(2026, 9, 1), "Annan inkomst"),
			new ApplicationIncome("RENT_SHARE_FROM_CHILD", "APPLICANT", BigDecimal.valueOf(200), LocalDate.of(2026, 9, 10), "Hyresdel från barn"),
			new ApplicationIncome("SALARY", "CO_APPLICANT", BigDecimal.valueOf(6788), null, "Lön"));

		final var result = ApplicationIncomeToFamilyCareMapper.toIncomeLines(incomes, proposal());

		assertThat(result.lines())
			.extracting(FamilyCareIncomeLine::typeId, FamilyCareIncomeLine::typeName, FamilyCareIncomeLine::recipient, FamilyCareIncomeLine::amount, FamilyCareIncomeLine::date,
				FamilyCareIncomeLine::note)
			.containsExactly(
				tuple(30, "Swish/Insättningar/Överföringar", "APPLICANT", BigDecimal.valueOf(599), OffsetDateTime.of(2026, 9, 24, 0, 0, 0, 0, ZoneOffset.UTC),
					"Ansökan: Swish/kontoinsättningar"),
				tuple(40, "Övriga inkomster", "APPLICANT", BigDecimal.valueOf(300), OffsetDateTime.of(2026, 9, 10, 0, 0, 0, 0, ZoneOffset.UTC),
					"Ansökan: Annan inkomst, Hyresdel från barn"),
				tuple(10, "Lön efter skatt", "CO_APPLICANT", BigDecimal.valueOf(6788), null, "Ansökan: Lön"));
		assertThat(result.untransferable()).isEmpty();
	}

	@Test
	void anIncomeNoProposalTypeTakesIsReportedNotDropped() {
		final var pension = new ApplicationIncome("OCCUPATIONAL_PENSION_INSURANCE", null, BigDecimal.valueOf(533), null, "Tjänstepension/försäkringar");
		final var unknown = new ApplicationIncome("LOTTERY", null, BigDecimal.ONE, null, null);

		final var result = ApplicationIncomeToFamilyCareMapper.toIncomeLines(List.of(pension, unknown), proposal());

		assertThat(result.lines()).isEmpty();
		assertThat(result.untransferable()).containsExactly(pension, unknown);
	}

	@Test
	void zeroAndMissingAmountsAndNullsAreSkipped() {
		final var incomes = new java.util.ArrayList<ApplicationIncome>();
		incomes.add(null);
		incomes.add(new ApplicationIncome("SWISH_DEPOSITS", null, BigDecimal.ZERO, null, null));
		incomes.add(new ApplicationIncome("SWISH_DEPOSITS", null, null, null, null));

		final var result = ApplicationIncomeToFamilyCareMapper.toIncomeLines(incomes, proposal());

		assertThat(result.lines()).isEmpty();
		assertThat(result.untransferable()).isEmpty();
	}

	@Test
	void theCodeStandsInForAMissingLabelAndNoInputGivesNothing() {
		final var result = ApplicationIncomeToFamilyCareMapper.toIncomeLines(List.of(new ApplicationIncome("SWISH_DEPOSITS", null, BigDecimal.TEN, null, null)), proposal());

		assertThat(result.lines()).extracting(FamilyCareIncomeLine::note).containsExactly("Ansökan: SWISH_DEPOSITS");
		assertThat(ApplicationIncomeToFamilyCareMapper.toIncomeLines(null, null).lines()).isEmpty();
	}
}
