package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.CO_APPLICANT;

class ClassifiedIncomeToFamilyCareMapperTest {

	private static PersonBasedCalculationProposalDTO proposal() {
		return new PersonBasedCalculationProposalDTO()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(20).name("Bostadsbidrag"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(30).name("Dagersättning"));
	}

	private static ClassifiedIncome classified(final String benefit, final String calculation, final String atgard, final String amount, final ApplicantRole role) {
		return new ClassifiedIncome(new SsbtekIncome(benefit, null, "Månad", new BigDecimal(amount), LocalDate.of(2026, MAY, 15), role), atgard, calculation, false, "note");
	}

	/** A classified income picked up from the comparison period rather than the control period. */
	private static ClassifiedIncome fromComparisonPeriod(final String benefit, final String calculation, final String amount) {
		return new ClassifiedIncome(new SsbtekIncome(benefit, null, "Månad", new BigDecimal(amount), LocalDate.of(2026, MAY, 15), APPLICANT),
			"TA_MED", calculation, false, "note", true);
	}

	@Test
	void dropsAComparisonPeriodIncomeThePreviousMonthAlreadyTransferred() {
		final var kept = ClassifiedIncomeToFamilyCareMapper.withoutAlreadyTransferred(
			List.of(fromComparisonPeriod("Underhållsstöd", "Underhållsstöd", "1673")),
			List.of("Underhållsstöd"));

		// the comparison period was last month's control period - taking it again counts the same money twice
		assertThat(kept).isEmpty();
	}

	@Test
	void keepsAComparisonPeriodIncomeThePreviousMonthMissed() {
		final var kept = ClassifiedIncomeToFamilyCareMapper.withoutAlreadyTransferred(
			List.of(fromComparisonPeriod("Underhållsstöd", "Underhållsstöd", "1673")),
			List.of("Bostadsbidrag"));

		// the regelverk's "nödlösning": an income that never made it across last month still has to come along
		assertThat(kept).hasSize(1);
	}

	@Test
	void neverDropsAControlPeriodIncomeEvenWhenThePreviousMonthHadTheSameType() {
		final var kept = ClassifiedIncomeToFamilyCareMapper.withoutAlreadyTransferred(
			List.of(classified("Bostadsbidrag", "Bostadsbidrag", "TA_MED_KVITTNING", "1850", APPLICANT)),
			List.of("Bostadsbidrag"));

		// the control period transfers in full every month; last month's calculation says nothing about it
		assertThat(kept).hasSize(1);
	}

	@Test
	void dropsNothingWhenThePreviousMonthIsUnknown() {
		final var incomes = List.of(fromComparisonPeriod("Underhållsstöd", "Underhållsstöd", "1673"));

		assertThat(ClassifiedIncomeToFamilyCareMapper.withoutAlreadyTransferred(incomes, null)).hasSize(1);
		assertThat(ClassifiedIncomeToFamilyCareMapper.withoutAlreadyTransferred(incomes, List.of())).hasSize(1);
	}

	@Test
	void resolvesCategoryToFamilyCareTypeIdAndSumsPerTypeAndRecipient() {
		final var lines = ClassifiedIncomeToFamilyCareMapper.toIncomeLines(List.of(
			classified("Bostadsbidrag", "Bostadsbidrag", "TA_MED_KVITTNING", "1850", APPLICANT),
			classified("Bostadsbidrag", "Bostadsbidrag", "TA_MED_KVITTNING", "150", APPLICANT),
			classified("Bostadsbidrag", "Bostadsbidrag", "TA_MED_KVITTNING", "200", CO_APPLICANT),
			classified("Dagersättning", "Dagersättning", "TA_MED", "5000", APPLICANT)),
			proposal());

		assertThat(lines)
			.extracting(line -> line.typeId(), line -> line.recipient(), line -> line.amount().intValue())
			.containsExactlyInAnyOrder(
				tuple(20, "APPLICANT", 2000),
				tuple(20, "CO_APPLICANT", 200),
				tuple(30, "APPLICANT", 5000));
	}

	@Test
	void skipsNonTransferableAndUnknownCategory() {
		final var lines = ClassifiedIncomeToFamilyCareMapper.toIncomeLines(List.of(
			classified("Handikappersättning", "-", "EJ_TA_MED", "100", APPLICANT),
			classified("Underhållsstöd", "-", "EJ_PA_LISTAN", "100", APPLICANT),
			classified("Okänd", "Okänd kategori", "TA_MED", "100", APPLICANT),
			classified("Tom", "-", "TA_MED", "100", APPLICANT)),
			proposal());

		assertThat(lines).isEmpty();
	}

	/** The proposal as Lifecare names its types — the dropdown names, not the regelverk's categories. */
	private static PersonBasedCalculationProposalDTO lifecareNamedProposal() {
		return new PersonBasedCalculationProposalDTO()
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(40).name("Barnbidrag/Flerbarnstillägg"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(41).name("Dagersättning från FK"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(42).name("Pension/SA/Livränta/Omvårdnadsbidrag"))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(43).name("A-kassa/Alfaersättning"));
	}

	@ParameterizedTest
	@MethodSource("regelverkCategoryArguments")
	void resolvesARegelverkCategoryToTheLifecareTypeItMeans(final String category, final int expectedTypeId) {
		final var lines = ClassifiedIncomeToFamilyCareMapper.toIncomeLines(
			List.of(classified("Förmån", category, "TA_MED", "1250", APPLICANT)), lifecareNamedProposal());

		assertThat(lines).extracting(FamilyCareIncomeLine::typeId).containsExactly(expectedTypeId);
	}

	private static Stream<Arguments> regelverkCategoryArguments() {
		return Stream.of(
			Arguments.of("Barnbidrag", 40),
			Arguments.of("Dagersättning", 41),
			Arguments.of("PLV", 42),
			Arguments.of(" a-kassa/ALFA ", 43));
	}

	@Test
	void prefersTheCategorysOwnNameWhenLifecareOffersIt() {
		// proposal() has a type called exactly "Dagersättning": the translation is a fallback, never an override.
		final var lines = ClassifiedIncomeToFamilyCareMapper.toIncomeLines(
			List.of(classified("Dagersättning", "Dagersättning", "TA_MED", "5000", APPLICANT)), proposal());

		assertThat(lines).extracting(FamilyCareIncomeLine::typeId).containsExactly(30);
	}

	@Test
	void dropsAComparisonPeriodIncomeTheLifecareNamedPreviousMonthAlreadyTransferred() {
		// The previous calculation is read back from Lifecare, so it carries Lifecare's name for the type.
		final var kept = ClassifiedIncomeToFamilyCareMapper.withoutAlreadyTransferred(
			List.of(fromComparisonPeriod("Allmänt barnbidrag", "Barnbidrag", "1250")),
			List.of("Barnbidrag/Flerbarnstillägg"));

		assertThat(kept).isEmpty();
	}

	@Test
	void untransferableNamesTheTransferableIncomesNoTypeMatches() {
		final var untransferable = ClassifiedIncomeToFamilyCareMapper.untransferable(List.of(
			classified("Allmänt barnbidrag", "Barnbidrag", "TA_MED", "1250", APPLICANT),
			classified("Studiemedel", "Studiemedel", "TA_MED", "3000", CO_APPLICANT),
			classified("Handikappersättning", "-", "EJ_TA_MED", "1450", CO_APPLICANT),
			classified("Okänd", "-", "EJ_PA_LISTAN", "100", APPLICANT)),
			lifecareNamedProposal());

		// Only the income the rules say to transfer and nothing can take — the others are not the draft's to hold.
		assertThat(untransferable).extracting(income -> income.income().benefit()).containsExactly("Studiemedel");
	}

	@Test
	void untransferableIsEmptyForNoIncomesAndTreatsAMissingProposalAsNoTypes() {
		assertThat(ClassifiedIncomeToFamilyCareMapper.untransferable(null, lifecareNamedProposal())).isEmpty();
		assertThat(ClassifiedIncomeToFamilyCareMapper.untransferable(
			List.of(classified("Bostadsbidrag", "Bostadsbidrag", "TA_MED_KVITTNING", "1850", APPLICANT)), null))
			.hasSize(1);
	}

	@Test
	void missingPreviousIncomeTypesCountsATranslatedCategoryAsCovered() {
		final var missing = ClassifiedIncomeToFamilyCareMapper.missingPreviousIncomeTypes(
			List.of("Barnbidrag/Flerbarnstillägg", "A-kassa/Alfaersättning"),
			List.of(classified("Allmänt barnbidrag", "Barnbidrag", "TA_MED", "1250", APPLICANT)),
			lifecareNamedProposal());

		assertThat(missing).containsExactly("A-kassa/Alfaersättning");
	}

	@Test
	void nullClassifiedYieldsEmpty() {
		assertThat(ClassifiedIncomeToFamilyCareMapper.toIncomeLines(null, proposal())).isEmpty();
	}

	@Test
	void toIncomeLinesDropsNullRoleInsteadOfFailing() {
		// A classified income with no role must be skipped (it can't be folded per-recipient) rather than NPE in the grouping
		// key.
		final var lines = ClassifiedIncomeToFamilyCareMapper.toIncomeLines(List.of(
			classified("Bostadsbidrag", "Bostadsbidrag", "TA_MED_KVITTNING", "1850", APPLICANT),
			classified("Dagersättning", "Dagersättning", "TA_MED", "5000", null)),
			proposal());

		assertThat(lines).extracting(FamilyCareIncomeLine::typeId).containsExactly(20);
	}
}
