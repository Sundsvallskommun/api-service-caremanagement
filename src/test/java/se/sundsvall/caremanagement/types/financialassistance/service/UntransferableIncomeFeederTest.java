package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class UntransferableIncomeFeederTest {

	private static final String APPLICANT_MESSAGE = "Studiemedel i SSBTEK ska tas med i normberäkningen men saknar inkomsttyp i Lifecare (Studiemedel) och har inte förts över – för in den för hand";
	private static final String CO_APPLICANT_MESSAGE = "Elstöd (medsökande) i SSBTEK ska tas med i normberäkningen men saknar inkomsttyp i Lifecare (Elstöd) och har inte förts över – för in den för hand";

	private final UntransferableIncomeFeeder feeder = new UntransferableIncomeFeeder();

	private static ClassifiedIncome classified(final String benefit, final ApplicantRole role) {
		final var income = new SsbtekIncome(benefit, null, null, new BigDecimal("2500"), LocalDate.parse("2026-09-20"), role);
		return new ClassifiedIncome(income, "TA_MED", benefit, false, "Ta med");
	}

	@Test
	void warnsForEachIncomeWithTheCategoryItCouldNotPlace() {
		final var warnings = feeder.untransferableIncomeWarnings(List.of(
			classified("Studiemedel", ApplicantRole.APPLICANT),
			classified("Elstöd", ApplicantRole.CO_APPLICANT)), Map.of());

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(
				tuple("INCOME_NOT_TRANSFERABLE", "studiemedel|APPLICANT", APPLICANT_MESSAGE),
				tuple("INCOME_NOT_TRANSFERABLE", "elstöd|CO_APPLICANT", CO_APPLICANT_MESSAGE));
	}

	@Test
	void collapsesSeveralPaymentsOfTheSameBenefitToTheSamePerson() {
		final var warnings = feeder.untransferableIncomeWarnings(List.of(
			classified("Studiemedel", ApplicantRole.APPLICANT),
			classified(" studiemedel", ApplicantRole.APPLICANT)), Map.of());

		assertThat(warnings).singleElement().satisfies(warning -> {
			assertThat(warning.sourceKey()).isEqualTo("studiemedel|APPLICANT");
			assertThat(warning.message()).isEqualTo(APPLICANT_MESSAGE);
		});
	}

	@Test
	void keepsTheSameBenefitApartPerPerson() {
		// The draft has a column per person, so the applicant's and the co-applicant's income are two things to add by hand.
		final var warnings = feeder.untransferableIncomeWarnings(List.of(
			classified("Studiemedel", ApplicantRole.APPLICANT),
			classified("Studiemedel", ApplicantRole.CO_APPLICANT)), Map.of());

		assertThat(warnings).extracting(WarningService.WarningInput::sourceKey)
			.containsExactly("studiemedel|APPLICANT", "studiemedel|CO_APPLICANT");
	}

	@Test
	void keysAnIncomeWithoutRoleOnTheBenefitAlone() {
		final var warnings = feeder.untransferableIncomeWarnings(List.of(classified("Studiemedel", null)), Map.of());

		assertThat(warnings).extracting(WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(tuple("studiemedel", APPLICANT_MESSAGE));
	}

	@Test
	void ignoresNullsAndBenefitlessIncomes() {
		final var withoutBenefit = new SsbtekIncome("  ", null, null, new BigDecimal("10"), LocalDate.parse("2026-09-20"), ApplicantRole.APPLICANT);

		final var warnings = feeder.untransferableIncomeWarnings(Arrays.asList(
			null,
			new ClassifiedIncome(null, "TA_MED", "Studiemedel", false, "Ta med"),
			new ClassifiedIncome(withoutBenefit, "TA_MED", "Studiemedel", false, "Ta med")), Map.of());

		assertThat(warnings).isEmpty();
	}

	@Test
	void noIncomesYieldNoWarnings() {
		assertThat(feeder.untransferableIncomeWarnings(null, Map.of())).isEmpty();
		assertThat(feeder.untransferableIncomeWarnings(List.of(), Map.of())).isEmpty();
	}

	@Test
	void namesAHouseholdChildAndKeepsTwoChildrenApart() {
		final var warnings = feeder.untransferableIncomeWarnings(List.of(
			childIncome("Studiehjälp", "child-1"),
			childIncome("Studiehjälp", "child-2"),
			childIncome("Studiehjälp", "child-3")), Map.of("child-1", "Kalle", "child-2", ""));

		assertThat(warnings).extracting(WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(
				tuple("studiehjälp|CHILD|child-1",
					"Studiehjälp (barn: Kalle) i SSBTEK ska tas med i normberäkningen men saknar inkomsttyp i Lifecare (Studiehjälp) och har inte förts över – för in den för hand"),
				tuple("studiehjälp|CHILD|child-2",
					"Studiehjälp (barn) i SSBTEK ska tas med i normberäkningen men saknar inkomsttyp i Lifecare (Studiehjälp) och har inte förts över – för in den för hand"),
				tuple("studiehjälp|CHILD|child-3",
					"Studiehjälp (barn) i SSBTEK ska tas med i normberäkningen men saknar inkomsttyp i Lifecare (Studiehjälp) och har inte förts över – för in den för hand"));
	}

	private static ClassifiedIncome childIncome(final String benefit, final String partyId) {
		final var income = new SsbtekIncome(benefit, null, null, new BigDecimal("900"), LocalDate.parse("2026-09-20"), null, null, null, ApplicantRole.CHILD, partyId);
		return new ClassifiedIncome(income, "TA_MED", benefit, false, "Ta med");
	}
}
