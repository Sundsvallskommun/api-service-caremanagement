package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationExpensePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationInvestigationDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationNormDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationPersonPostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationServiceDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationSpecialExpensePostDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationSections;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;

class CalculationAssemblerTest {

	private static final String PERSON_ID = "198001012389";
	private static final YearMonth MONTH = YearMonth.of(2026, JUNE);

	@Test
	void assemblesPersonDatesAndIncomes() {
		final var income = new PersonBasedCalculationIncomePostDTO().id(10).applicantAmount(2400.0);

		final var body = CalculationAssembler.assemble(PERSON_ID, null, List.of(income), MONTH);

		assertThat(body.getPersonId()).isEqualTo(PERSON_ID);
		assertThat(body.getCalculationDate()).isEqualTo("2026-06-01");
		assertThat(body.getCalculationFromDate()).isEqualTo("2026-06-01");
		assertThat(body.getCalculationToDate()).isEqualTo("2026-06-30");
		assertThat(body.getCalculationIncomes()).containsExactly(income);
		// No proposal → no links resolved.
		assertThat(body.getServiceId()).isNull();
		assertThat(body.getInvestigationId()).isNull();
		assertThat(body.getNormId()).isNull();
		assertThat(body.getAktualiseringId()).isNull();
	}

	@Test
	void nullIncomesBecomeEmptyList() {
		final var body = CalculationAssembler.assemble(PERSON_ID, null, (List<PersonBasedCalculationIncomePostDTO>) null, MONTH);

		assertThat(body.getCalculationIncomes()).isEmpty();
	}

	@Test
	void picksFirstServiceInvestigationAndCoveringNorm() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.addServicesItem(new PersonBasedCalculationServiceDTO().id(5))
			.addServicesItem(new PersonBasedCalculationServiceDTO().id(6))
			.addInvestigationsItem(new PersonBasedCalculationInvestigationDTO().id(7))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(100).fromDate("2020-01-01").toDate("2025-12-31"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(200).fromDate("2026-01-01").toDate("2026-12-31"));

		final var body = CalculationAssembler.assemble(PERSON_ID, proposal, List.of(), MONTH);

		assertThat(body.getServiceId()).isEqualTo(5);
		assertThat(body.getInvestigationId()).isEqualTo(7);
		assertThat(body.getNormId()).isEqualTo(200); // the norm whose window covers 2026-06
	}

	@Test
	void fallsBackToFirstNormWhenNoneCoversTheMonth() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(100).fromDate("2020-01-01").toDate("2020-12-31"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(200).fromDate("2021-01-01").toDate("2021-12-31"));

		final var body = CalculationAssembler.assemble(PERSON_ID, proposal, List.of(), MONTH);

		assertThat(body.getNormId()).isEqualTo(100);
	}

	@Test
	void linksAktualiseringOnlyWhenMandatory() {
		final var mandatory = new PersonBasedCalculationProposalDTO()
			.aktualiseringMandatory(true)
			.addAktualiseringsItem(new PersonBasedCalculationAktualiseringDTO().id(42));
		assertThat(CalculationAssembler.assemble(PERSON_ID, mandatory, List.of(), MONTH).getAktualiseringId()).isEqualTo(42);

		final var notMandatory = new PersonBasedCalculationProposalDTO()
			.aktualiseringMandatory(false)
			.addAktualiseringsItem(new PersonBasedCalculationAktualiseringDTO().id(42));
		assertThat(CalculationAssembler.assemble(PERSON_ID, notMandatory, List.of(), MONTH).getAktualiseringId()).isNull();
	}

	// ---- Full sections overload --------------------------------------------------------------------------------------

	@Test
	void assemblesAllSectionsAndAppliesHeaderOverrides() {
		final var income = new PersonBasedCalculationIncomePostDTO().id(10).applicantAmount(2400.0);
		final var expense = new PersonBasedCalculationExpensePostDTO().id(20);
		final var specialExpense = new PersonBasedCalculationSpecialExpensePostDTO().id(30);
		final var person = new PersonBasedCalculationPersonPostDTO().personId(PERSON_ID);
		final var header = new CalculationHeader(999, LocalDate.of(2026, JUNE, 5), LocalDate.of(2026, JUNE, 25),
			LocalDate.of(2026, JUNE, 5), true, 3);

		final var proposal = new PersonBasedCalculationProposalDTO()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(200).fromDate("2026-01-01").toDate("2026-12-31"));

		final var sections = new CalculationSections(List.of(income), List.of(expense), List.of(specialExpense), List.of(person), header);
		final var body = CalculationAssembler.assemble(PERSON_ID, proposal, sections, MONTH);

		assertThat(body.getPersonId()).isEqualTo(PERSON_ID);
		assertThat(body.getCalculationIncomes()).containsExactly(income);
		assertThat(body.getCalculationExpenses()).containsExactly(expense);
		assertThat(body.getCalculationSpecialExpenses()).containsExactly(specialExpense);
		assertThat(body.getCalculationPersons()).containsExactly(person);
		// Header overrides the proposal norm and the date window + household.
		assertThat(body.getNormId()).isEqualTo(999);
		assertThat(body.getCalculationFromDate()).isEqualTo("2026-06-05");
		assertThat(body.getCalculationToDate()).isEqualTo("2026-06-25");
		assertThat(body.getCalculationDate()).isEqualTo("2026-06-05");
		assertThat(body.getHasCustomHouseholdSize()).isTrue();
		assertThat(body.getHouseholdSize()).isEqualTo(3);
	}

	@Test
	void nullSectionFieldsLeaveBodyDefaults() {
		final var proposal = new PersonBasedCalculationProposalDTO()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(200).fromDate("2026-01-01").toDate("2026-12-31"));

		final var sections = new CalculationSections(null, null, null, null, null);
		final var body = CalculationAssembler.assemble(PERSON_ID, proposal, sections, MONTH);

		assertThat(body.getCalculationIncomes()).isEmpty();
		assertThat(body.getCalculationExpenses()).isEmpty();
		assertThat(body.getCalculationSpecialExpenses()).isEmpty();
		assertThat(body.getCalculationPersons()).isEmpty();
		// No header → proposal norm + application-month window stand.
		assertThat(body.getNormId()).isEqualTo(200);
		assertThat(body.getCalculationFromDate()).isEqualTo("2026-06-01");
		assertThat(body.getCalculationToDate()).isEqualTo("2026-06-30");
	}
}
