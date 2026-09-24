package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
			classified("Elstöd", ApplicantRole.CO_APPLICANT)));

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(
				tuple("INCOME_NOT_TRANSFERABLE", "studiemedel|APPLICANT", APPLICANT_MESSAGE),
				tuple("INCOME_NOT_TRANSFERABLE", "elstöd|CO_APPLICANT", CO_APPLICANT_MESSAGE));
	}

	@Test
	void collapsesSeveralPaymentsOfTheSameBenefitToTheSamePerson() {
		final var warnings = feeder.untransferableIncomeWarnings(List.of(
			classified("Studiemedel", ApplicantRole.APPLICANT),
			classified(" studiemedel", ApplicantRole.APPLICANT)));

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
			classified("Studiemedel", ApplicantRole.CO_APPLICANT)));

		assertThat(warnings).extracting(WarningService.WarningInput::sourceKey)
			.containsExactly("studiemedel|APPLICANT", "studiemedel|CO_APPLICANT");
	}

	@Test
	void keysAnIncomeWithoutRoleOnTheBenefitAlone() {
		final var warnings = feeder.untransferableIncomeWarnings(List.of(classified("Studiemedel", null)));

		assertThat(warnings).extracting(WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(tuple("studiemedel", APPLICANT_MESSAGE));
	}

	@Test
	void ignoresNullsAndBenefitlessIncomes() {
		final var withoutBenefit = new SsbtekIncome("  ", null, null, new BigDecimal("10"), LocalDate.parse("2026-09-20"), ApplicantRole.APPLICANT);

		final var warnings = feeder.untransferableIncomeWarnings(Arrays.asList(
			null,
			new ClassifiedIncome(null, "TA_MED", "Studiemedel", false, "Ta med"),
			new ClassifiedIncome(withoutBenefit, "TA_MED", "Studiemedel", false, "Ta med")));

		assertThat(warnings).isEmpty();
	}

	@Test
	void noIncomesYieldNoWarnings() {
		assertThat(feeder.untransferableIncomeWarnings(null)).isEmpty();
		assertThat(feeder.untransferableIncomeWarnings(List.of())).isEmpty();
	}
}
