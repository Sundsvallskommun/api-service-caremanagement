package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.CO_APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.UnhandledReason.FC_TYPE_NOT_IN_PROPOSAL;
import static se.sundsvall.caremanagement.lifecare.service.model.UnhandledReason.NOT_ON_WHITELIST;

class SsbtekToFcIncomeMapperTest {

	private static PersonBasedCalculationProposalDTO proposal() {
		return new PersonBasedCalculationProposalDTO()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(10).name("Bostadsbidrag"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(20).name("PLV"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(30).name("A-kassa/Alfa"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(40).name("Barnbidrag"));
	}

	private static SsbtekIncome income(final String forman, final String delforman, final String beloppstyp, final double amount, final String date, final se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole role) {
		return new SsbtekIncome(forman, delforman, beloppstyp, BigDecimal.valueOf(amount), LocalDate.parse(date), role);
	}

	private static OffsetDateTime atUtc(final String date) {
		return LocalDate.parse(date).atStartOfDay().atOffset(ZoneOffset.UTC);
	}

	@Test
	void mapsSingleApplicantIncome() {
		final var incomes = List.of(income("Bostadsbidrag", "Bostadsbidrag", "Månad", 1000, "2026-04-15", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, proposal());

		assertThat(result.unhandledIncomes()).isEmpty();
		assertThat(result.calculationIncomes()).singleElement().satisfies(row -> {
			assertThat(row.getId()).isEqualTo(10);
			assertThat(row.getApplicantAmount()).isEqualTo(1000.0);
			assertThat(row.getApplicantAmountDate()).isEqualTo(atUtc("2026-04-15"));
			assertThat(row.getCoApplicantAmount()).isNull();
			assertThat(row.getCoApplicantAmountDate()).isNull();
			assertThat(row.getNote()).isEqualTo("SSBTEK: Bostadsbidrag / Bostadsbidrag / Månad");
		});
	}

	@Test
	void mergesApplicantAndCoApplicantIntoOneRow() {
		final var incomes = List.of(
			income("Bostadsbidrag", null, null, 1000, "2026-04-15", APPLICANT),
			income("Bostadsbidrag", null, null, 500, "2026-04-20", CO_APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, proposal());

		assertThat(result.calculationIncomes()).singleElement().satisfies(row -> {
			assertThat(row.getId()).isEqualTo(10);
			assertThat(row.getApplicantAmount()).isEqualTo(1000.0);
			assertThat(row.getApplicantAmountDate()).isEqualTo(atUtc("2026-04-15"));
			assertThat(row.getCoApplicantAmount()).isEqualTo(500.0);
			assertThat(row.getCoApplicantAmountDate()).isEqualTo(atUtc("2026-04-20"));
		});
	}

	@Test
	void aggregatesAmountsOfSameTypeAndRoleAndKeepsLatestDate() {
		final var incomes = List.of(
			income("Bostadsbidrag", null, null, 1000, "2026-04-10", APPLICANT),
			income("Bostadsbidrag", null, null, 250, "2026-04-20", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, proposal());

		assertThat(result.calculationIncomes()).singleElement().satisfies(row -> {
			assertThat(row.getApplicantAmount()).isEqualTo(1250.0);
			assertThat(row.getApplicantAmountDate()).isEqualTo(atUtc("2026-04-20"));
		});
	}

	@Test
	void mapsDistinctTypesToDistinctRows() {
		final var incomes = List.of(
			income("Bostadsbidrag", null, null, 1000, "2026-04-15", APPLICANT),
			income("Allmänt barnbidrag", null, null, 1250, "2026-04-15", APPLICANT),
			income("Arbetslöshetsersättning", null, null, 8000, "2026-04-15", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, proposal());

		assertThat(result.calculationIncomes())
			.extracting(row -> row.getId())
			.containsExactlyInAnyOrder(10, 40, 30);
	}

	@Test
	void excludedFormanIsSkippedSilently() {
		final var incomes = List.of(income("Handikappersättning", null, null, 800, "2026-04-15", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, proposal());

		assertThat(result.calculationIncomes()).isEmpty();
		assertThat(result.unhandledIncomes()).isEmpty();
	}

	@Test
	void unknownFormanIsUnhandled() {
		final var incomes = List.of(income("Försörjningsstöd", "Del", "Belopp", 5000, "2026-04-15", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, proposal());

		assertThat(result.calculationIncomes()).isEmpty();
		assertThat(result.unhandledIncomes()).singleElement().satisfies(unhandled -> {
			assertThat(unhandled.forman()).isEqualTo("Försörjningsstöd");
			assertThat(unhandled.delforman()).isEqualTo("Del");
			assertThat(unhandled.beloppstyp()).isEqualTo("Belopp");
			assertThat(unhandled.reason()).isEqualTo(NOT_ON_WHITELIST);
		});
	}

	@Test
	void whitelistedButFcTypeMissingFromProposalIsUnhandled() {
		// Underhållsstöd is whitelisted (-> "Underhållsstöd") but the proposal does not offer that type.
		final var incomes = List.of(income("Underhållsstöd", null, null, 1500, "2026-04-15", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, proposal());

		assertThat(result.calculationIncomes()).isEmpty();
		assertThat(result.unhandledIncomes()).singleElement().satisfies(unhandled -> {
			assertThat(unhandled.forman()).isEqualTo("Underhållsstöd");
			assertThat(unhandled.reason()).isEqualTo(FC_TYPE_NOT_IN_PROPOSAL);
		});
	}

	@Test
	void nullProposalMakesAllMappableIncomesUnhandled() {
		final var incomes = List.of(income("Bostadsbidrag", null, null, 1000, "2026-04-15", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, null);

		assertThat(result.calculationIncomes()).isEmpty();
		assertThat(result.unhandledIncomes()).singleElement()
			.satisfies(unhandled -> assertThat(unhandled.reason()).isEqualTo(FC_TYPE_NOT_IN_PROPOSAL));
	}

	@Test
	void resolutionIsCaseInsensitiveAgainstProposalNames() {
		final var upperCaseProposal = new PersonBasedCalculationProposalDTO()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(99).name("BOSTADSBIDRAG"));
		final var incomes = List.of(income("bostadsbidrag", null, null, 700, "2026-04-15", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, upperCaseProposal);

		assertThat(result.unhandledIncomes()).isEmpty();
		assertThat(result.calculationIncomes()).singleElement()
			.satisfies(row -> assertThat(row.getId()).isEqualTo(99));
	}

	@Test
	void nullIncomeEntriesAreIgnored() {
		final var incomes = Arrays.asList(null, income("Bostadsbidrag", null, null, 1000, "2026-04-15", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, proposal());

		assertThat(result.calculationIncomes()).hasSize(1);
		assertThat(result.unhandledIncomes()).isEmpty();
	}

	@Test
	void blankAndNullSubFieldsAreOmittedFromNote() {
		final var incomes = List.of(income("Bostadsbidrag", "", null, 1000, "2026-04-15", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, proposal());

		assertThat(result.calculationIncomes()).singleElement()
			.satisfies(row -> assertThat(row.getNote()).isEqualTo("SSBTEK: Bostadsbidrag"));
	}

	@Test
	void proposalTypesWithNullIdOrNameAreIgnored() {
		final var proposalWithGaps = new PersonBasedCalculationProposalDTO()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(null).name("Bostadsbidrag"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(7).name(null))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(10).name("Bostadsbidrag"));
		final var incomes = List.of(income("Bostadsbidrag", null, null, 1000, "2026-04-15", APPLICANT));

		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(incomes, proposalWithGaps);

		assertThat(result.unhandledIncomes()).isEmpty();
		assertThat(result.calculationIncomes()).singleElement()
			.satisfies(row -> assertThat(row.getId()).isEqualTo(10));
	}

	@Test
	void nullIncomesReturnsEmptyResult() {
		final var result = SsbtekToFcIncomeMapper.toCalculationIncomes(null, proposal());

		assertThat(result.calculationIncomes()).isEmpty();
		assertThat(result.unhandledIncomes()).isEmpty();
	}
}
