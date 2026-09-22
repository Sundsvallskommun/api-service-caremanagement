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

class LateTransferFeederTest {

	private static final String EXPECTED_MESSAGE = "Underhållsstöd i SSBTEK från föregående månad var inte överförd till normberäkningen men har nu förts över – kontrollera om den ska vara med i normberäkningen";

	private final LateTransferFeeder feeder = new LateTransferFeeder();

	private static ClassifiedIncome classified(final String benefit) {
		final var income = new SsbtekIncome(benefit, null, null, new BigDecimal("1673"), LocalDate.parse("2026-04-20"), ApplicantRole.APPLICANT);
		return new ClassifiedIncome(income, "TA_MED", benefit, false, "Ta med", true);
	}

	@Test
	void warnsForEachLateTransferredBenefit() {
		final var warnings = feeder.lateTransferWarnings(List.of(classified("Underhållsstöd")));

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(tuple("INCOME_TRANSFERRED_LATE", "underhållsstöd", EXPECTED_MESSAGE));
	}

	@Test
	void collapsesSeveralPaymentsOfTheSameBenefitIntoOneWarning() {
		// Two payments of the same benefit are one fact for the handläggare, and a stable source key is what lets the
		// next run close exactly this warning.
		final var warnings = feeder.lateTransferWarnings(List.of(classified("Underhållsstöd"), classified("underhållsstöd ")));

		assertThat(warnings).singleElement().satisfies(warning -> {
			assertThat(warning.sourceKey()).isEqualTo("underhållsstöd");
			assertThat(warning.message()).isEqualTo(EXPECTED_MESSAGE);
		});
	}

	@Test
	void takesTheCallersSetAsGivenWithoutRejudgingThePeriod() {
		// The caller hands in the incomes the transfer filter kept; re-testing isFromComparisonPeriod here would be a
		// second reading of the same rule, and the two could then disagree about what was transferred.
		final var income = new SsbtekIncome("Bostadsbidrag", null, null, new BigDecimal("900"), LocalDate.parse("2026-04-20"), ApplicantRole.APPLICANT);
		final var notFlagged = new ClassifiedIncome(income, "TA_MED", "Bostadsbidrag", false, "Ta med", false);

		assertThat(feeder.lateTransferWarnings(List.of(notFlagged))).hasSize(1);
	}

	@Test
	void ignoresNullsAndBenefitlessIncomes() {
		final var withoutBenefit = new SsbtekIncome("  ", null, null, new BigDecimal("10"), LocalDate.parse("2026-04-20"), ApplicantRole.APPLICANT);

		final var warnings = feeder.lateTransferWarnings(Arrays.asList(
			null,
			new ClassifiedIncome(null, "TA_MED", null, false, "Ta med", true),
			new ClassifiedIncome(withoutBenefit, "TA_MED", null, false, "Ta med", true)));

		assertThat(warnings).isEmpty();
	}

	@Test
	void noIncomesYieldNoWarnings() {
		assertThat(feeder.lateTransferWarnings(null)).isEmpty();
		assertThat(feeder.lateTransferWarnings(List.of())).isEmpty();
	}
}
