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

class MissingIncomeFeederTest {

	private final MissingIncomeFeeder feeder = new MissingIncomeFeeder();

	private static ClassifiedIncome classified(final String benefit, final boolean fromComparisonPeriod) {
		final var income = new SsbtekIncome(benefit, null, null, new BigDecimal("1673"), LocalDate.parse("2026-04-20"), ApplicantRole.APPLICANT);
		return new ClassifiedIncome(income, "TA_MED", benefit, false, "Ta med", fromComparisonPeriod);
	}

	@Test
	void warnsForEveryBenefitOnlyInTheComparisonPeriod() {
		final var warnings = feeder.missingIncomeWarnings(List.of(
			classified("Underhållsstöd", true),
			classified("Bostadsbidrag", false)));

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(tuple("INCOME_MISSING_PREVIOUS_PERIOD", "underhållsstöd",
				"Underhållsstöd fanns föregående månad i SSBTEK men saknas nu"));
	}

	@Test
	void collapsesSeveralPaymentsOfTheSameBenefitIntoOneWarning() {
		// Two payments of the same benefit are one fact for the handläggare, and a stable source key is what lets the
		// next run close exactly this warning.
		final var warnings = feeder.missingIncomeWarnings(List.of(
			classified("Underhållsstöd", true),
			classified("underhållsstöd ", true)));

		assertThat(warnings).singleElement().satisfies(warning -> {
			assertThat(warning.sourceKey()).isEqualTo("underhållsstöd");
			assertThat(warning.message()).isEqualTo("Underhållsstöd fanns föregående månad i SSBTEK men saknas nu");
		});
	}

	@Test
	void isQuietWhenNothingCameFromTheComparisonPeriod() {
		assertThat(feeder.missingIncomeWarnings(List.of(classified("Bostadsbidrag", false)))).isEmpty();
	}

	@Test
	void toleratesAnAbsentOrIncompletePayload() {
		// A payload from before the flag existed, a null list, and a row without a benefit must all pass silently
		// rather than raise a warning naming nothing.
		assertThat(feeder.missingIncomeWarnings(null)).isEmpty();
		assertThat(feeder.missingIncomeWarnings(List.of())).isEmpty();
		assertThat(feeder.missingIncomeWarnings(Arrays.asList(classified(null, true), classified("  ", true), null))).isEmpty();
	}
}
