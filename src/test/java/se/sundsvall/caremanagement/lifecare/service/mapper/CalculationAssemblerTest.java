package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationNormDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;

class CalculationAssemblerTest {

	private static final YearMonth MONTH = YearMonth.of(2026, JUNE);

	@Test
	void picksTheNormCoveringTheMonth() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(100).fromDate("2020-01-01").toDate("2025-12-31"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(200).fromDate("2026-01-01").toDate("2026-12-31"));

		assertThat(CalculationAssembler.selectNormId(proposal, MONTH, List.of())).contains(200); // the norm whose window covers 2026-06
	}

	/**
	 * The regression behind <em>Saknar norm för angiven hushållsstorlek</em>: FamilyCare offered four norms for
	 * September 2026 and all four covered the month, so "first that covers" picked Matnorm — a reduced food norm with
	 * no single-person row — while the application had asked for Riksnorm all along.
	 */
	@Test
	void picksTheNormTheApplicationAskedForWhenSeveralCoverTheMonth() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(4).name("Matnorm 2026").fromDate("2026-08-01").toDate("2027-02-28"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(3).name("Nettonorm 2026").fromDate("2026-08-01").toDate("2027-02-28"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(1).name("Riksnorm 2026").fromDate("2026-06-01").toDate("2027-02-28"));

		assertThat(CalculationAssembler.selectNormId(proposal, MONTH, List.of("NATIONAL_NORM"))).contains(1);
	}

	/** "Annan norm" names no FamilyCare norm, so it selects nothing and the covering-window default stands. */
	@Test
	void fallsBackToTheCoveringNormWhenTheNormTypeNamesNoCatalogueEntry() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(4).name("Matnorm 2026").fromDate("2026-06-01").toDate("2027-02-28"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(1).name("Riksnorm 2026").fromDate("2026-06-01").toDate("2027-02-28"));

		assertThat(CalculationAssembler.selectNormId(proposal, MONTH, List.of("OTHER_NORM"))).contains(4);
	}

	@Test
	void matchingNormIdIsStrict() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(4).name("Matnorm 2026").fromDate("2026-01-01").toDate("2026-12-31"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(1).name("Riksnorm 2026").fromDate("2026-01-01").toDate("2026-12-31"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(9).name("Specnorm 2025").fromDate("2025-01-01").toDate("2025-12-31"));
		final var september = YearMonth.of(2026, 9);

		assertThat(CalculationAssembler.matchingNormId(proposal, september, List.of("riksnorm"))).contains(1);
		// a norm that does not cover the month does not match, and nothing falls back to the first covering one
		assertThat(CalculationAssembler.matchingNormId(proposal, september, List.of("Specnorm"))).isEmpty();
		assertThat(CalculationAssembler.matchingNormId(proposal, september, List.of())).isEmpty();
		assertThat(CalculationAssembler.matchingNormId(proposal, september, null)).isEmpty();
		assertThat(CalculationAssembler.matchingNormId(null, september, List.of("Riksnorm"))).isEmpty();
	}

	@Test
	void fallsBackToFirstNormWhenNoneCoversTheMonth() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(100).fromDate("2020-01-01").toDate("2020-12-31"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(200).fromDate("2021-01-01").toDate("2021-12-31"));

		assertThat(CalculationAssembler.selectNormId(proposal, MONTH, List.of())).contains(100);
	}

	@Test
	void selectsNothingWithoutAProposalOrNorms() {
		assertThat(CalculationAssembler.selectNormId(null, MONTH, List.of("Riksnorm"))).isEmpty();
		assertThat(CalculationAssembler.selectNormId(new PersonBasedCalculationProposalDTO(), MONTH, null)).isEmpty();
	}
}
