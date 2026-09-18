package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodRuleFeederTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private PeriodRulesService periodRulesServiceMock;

	@InjectMocks
	private PeriodRuleFeeder feeder;

	// ----------------------------------------------------------------------------------------------------------------
	// Selection
	// ----------------------------------------------------------------------------------------------------------------

	@Test
	void anIncomeThatIsNeitherRuleIsNotJudged() {
		final var housingAllowance = control(income("Bostadsbidrag", "Bostadsbidrag", "Bostadsbidrag",
			LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), BigDecimal.valueOf(12)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of(housingAllowance))).isEmpty();
		verifyNoInteractions(periodRulesServiceMock);
	}

	@Test
	void aSplitPaymentWithoutSubBenefitOrAmountTypeMatchesNeitherRule() {
		// FK splitting a payment across several utbetalningsdetalj rows leaves both null in the extractor, so the
		// payment cannot be routed to a rule. Documented gap — it must not be guessed into one.
		final var split = control(income("Dagersättning", null, null,
			LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), BigDecimal.valueOf(22)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of(split))).isEmpty();
		verifyNoInteractions(periodRulesServiceMock);
	}

	@Test
	void selectionIsCaseAndWhitespaceInsensitive() {
		when(periodRulesServiceMock.dayCheck(any(), anyBoolean(), any(), any()))
			.thenReturn(new PeriodRulesService.PeriodVerdict(true, "Gick ej att läsa ut"));
		final var income = control(income("  DAGERSÄTTNING ", "Arbetsmarknadspolitiskt Pgm", "AKTIVITETSSTÖD",
			LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31), BigDecimal.valueOf(22)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of(income))).hasSize(1);
	}

	// ----------------------------------------------------------------------------------------------------------------
	// Decision_dagersattningDagkontroll
	// ----------------------------------------------------------------------------------------------------------------

	@Test
	void aPeriodSpanningTwoMonthsIsReportedAsUnreadable() {
		when(periodRulesServiceMock.dayCheck(any(), anyBoolean(), any(), any()))
			.thenReturn(new PeriodRulesService.PeriodVerdict(true, "Gick ej att läsa ut antal dagar"));
		final var income = control(dayBenefit(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31), BigDecimal.valueOf(40)));

		final var warnings = feeder.periodWarnings(MUNICIPALITY_ID, List.of(income));

		verify(periodRulesServiceMock).dayCheck(eq(MUNICIPALITY_ID), eq(false), eq(BigDecimal.valueOf(40)), eq(null));
		assertThat(warnings).singleElement().satisfies(warning -> {
			assertThat(warning.type()).isEqualTo(WarningService.TYPE_SSBTEK_DAY_CHECK);
			assertThat(warning.sourceKey()).isEqualTo("DAGERSATTNING:2026-04-01");
			assertThat(warning.message()).isEqualTo("Gick ej att läsa ut antal dagar");
		});
	}

	@Test
	void aMissingPeriodIsAlsoUnreadable() {
		when(periodRulesServiceMock.dayCheck(any(), anyBoolean(), any(), any()))
			.thenReturn(new PeriodRulesService.PeriodVerdict(true, "Gick ej att läsa ut antal dagar"));
		final var income = control(dayBenefit(null, null, BigDecimal.valueOf(22)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of(income))).hasSize(1);
		verify(periodRulesServiceMock).dayCheck(eq(MUNICIPALITY_ID), eq(false), any(), eq(null));
	}

	@Test
	void aReadableMonthWithNoDayCountIsReportedAsMissing() {
		when(periodRulesServiceMock.dayCheck(any(), anyBoolean(), any(), any()))
			.thenReturn(new PeriodRulesService.PeriodVerdict(true, "Antal uttagna dagar saknas i SSBTEK-svaret"));
		final var income = control(dayBenefit(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), null));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of(income))).hasSize(1);
		verify(periodRulesServiceMock).dayCheck(eq(MUNICIPALITY_ID), eq(true), eq(null), eq(null));
	}

	@Test
	void aReadableMonthWithADayCountIsLeftUncheckedUntilTheCalendarExists() {
		// The remaining branch compares against icke-röda dagar. There is no holiday calendar, and passing null would
		// make FEEL's "uttagnaDagar = null" false — a "dagarna stämmer inte" warning on every correct payment.
		final var income = control(dayBenefit(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), BigDecimal.valueOf(22)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of(income))).isEmpty();
		verify(periodRulesServiceMock, never()).dayCheck(any(), anyBoolean(), any(), any());
	}

	// ----------------------------------------------------------------------------------------------------------------
	// Decision_foraldrapenningKontroll
	// ----------------------------------------------------------------------------------------------------------------

	@Test
	void parentalBenefitPassesTheDayCountThePeriodLengthAndTheGap() {
		when(periodRulesServiceMock.parentalBenefitCheck(any(), anyBoolean(), any(), any(), anyBoolean(), any()))
			.thenReturn(new PeriodRulesService.PeriodVerdict(true, "Glapp mellan föregående föräldrapenningperiod och denna"));
		final var previous = comparison(parentalBenefit(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 25), BigDecimal.valueOf(25)));
		final var current = control(parentalBenefit(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), BigDecimal.valueOf(30)));

		final var warnings = feeder.periodWarnings(MUNICIPALITY_ID, List.of(previous, current));

		final var gap = ArgumentCaptor.forClass(Integer.class);
		final var daysInPeriod = ArgumentCaptor.forClass(Integer.class);
		verify(periodRulesServiceMock).parentalBenefitCheck(eq(MUNICIPALITY_ID), eq(true), eq(BigDecimal.valueOf(30)),
			daysInPeriod.capture(), eq(true), gap.capture());
		assertThat(daysInPeriod.getValue()).isEqualTo(30); // 1 April - 30 April, both ends counted
		assertThat(gap.getValue()).isEqualTo(6); // 26-31 March uncovered
		assertThat(warnings).singleElement().satisfies(warning -> {
			assertThat(warning.type()).isEqualTo(WarningService.TYPE_PARENTAL_BENEFIT_PERIOD_CHECK);
			assertThat(warning.sourceKey()).isEqualTo("FORALDRAPENNING:2026-04-01");
		});
	}

	@Test
	void consecutivePeriodsAreNoGap() {
		when(periodRulesServiceMock.parentalBenefitCheck(any(), anyBoolean(), any(), any(), anyBoolean(), any()))
			.thenReturn(PeriodRulesService.PeriodVerdict.none());
		final var previous = comparison(parentalBenefit(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), BigDecimal.valueOf(31)));
		final var current = control(parentalBenefit(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), BigDecimal.valueOf(30)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of(previous, current))).isEmpty();
		verify(periodRulesServiceMock).parentalBenefitCheck(any(), anyBoolean(), any(), any(), eq(true), eq(0));
	}

	@Test
	void overlappingPeriodsGiveANegativeGap() {
		when(periodRulesServiceMock.parentalBenefitCheck(any(), anyBoolean(), any(), any(), anyBoolean(), any()))
			.thenReturn(PeriodRulesService.PeriodVerdict.none());
		final var previous = comparison(parentalBenefit(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 2), BigDecimal.valueOf(33)));
		final var current = control(parentalBenefit(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), BigDecimal.valueOf(30)));

		feeder.periodWarnings(MUNICIPALITY_ID, List.of(previous, current));

		verify(periodRulesServiceMock).parentalBenefitCheck(any(), anyBoolean(), any(), any(), eq(true), eq(-2));
	}

	@Test
	void noParentalBenefitLastMonthMeansNoPreviousPeriodAndNoGap() {
		when(periodRulesServiceMock.parentalBenefitCheck(any(), anyBoolean(), any(), any(), anyBoolean(), any()))
			.thenReturn(new PeriodRulesService.PeriodVerdict(true, "Går ej att kontrollera glapp"));
		final var current = control(parentalBenefit(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), BigDecimal.valueOf(30)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of(current))).hasSize(1);
		verify(periodRulesServiceMock).parentalBenefitCheck(any(), anyBoolean(), any(), any(), eq(false), eq(null));
	}

	@Test
	void theLatestOfSeveralPreviousPeriodsIsTheOneMeasuredFrom() {
		when(periodRulesServiceMock.parentalBenefitCheck(any(), anyBoolean(), any(), any(), anyBoolean(), any()))
			.thenReturn(PeriodRulesService.PeriodVerdict.none());
		final var early = comparison(parentalBenefit(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10), BigDecimal.valueOf(10)));
		final var late = comparison(parentalBenefit(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 31), BigDecimal.valueOf(17)));
		final var current = control(parentalBenefit(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), BigDecimal.valueOf(30)));

		feeder.periodWarnings(MUNICIPALITY_ID, List.of(early, late, current));

		verify(periodRulesServiceMock).parentalBenefitCheck(any(), anyBoolean(), any(), any(), eq(true), eq(0));
	}

	@Test
	void anUnreadableParentalPeriodIsReportedAsSuchRatherThanAsADayMismatch() {
		when(periodRulesServiceMock.parentalBenefitCheck(any(), anyBoolean(), any(), any(), anyBoolean(), any()))
			.thenReturn(new PeriodRulesService.PeriodVerdict(true, "Gick ej att läsa ut perioden för föräldrapenning"));
		final var current = control(parentalBenefit(null, null, BigDecimal.valueOf(30)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of(current))).hasSize(1);
		verify(periodRulesServiceMock).parentalBenefitCheck(eq(MUNICIPALITY_ID), eq(false), any(), eq(null), anyBoolean(), any());
	}

	@Test
	void aVerdictWithoutAWarningRaisesNothing() {
		when(periodRulesServiceMock.parentalBenefitCheck(any(), anyBoolean(), any(), any(), anyBoolean(), any()))
			.thenReturn(PeriodRulesService.PeriodVerdict.none());
		final var current = control(parentalBenefit(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), BigDecimal.valueOf(30)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of(current))).isEmpty();
	}

	@Test
	void noIncomesAtAllIsHandled() {
		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, null)).isEmpty();
		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, List.of())).isEmpty();
		verifyNoInteractions(periodRulesServiceMock);
	}

	// ----------------------------------------------------------------------------------------------------------------

	private static SsbtekIncome dayBenefit(final LocalDate from, final LocalDate to, final BigDecimal days) {
		return income("Dagersättning", "Arbetsmarknadspolitiskt program", "Aktivitetsstöd", from, to, days);
	}

	private static SsbtekIncome parentalBenefit(final LocalDate from, final LocalDate to, final BigDecimal days) {
		return income("Dagersättning", "Föräldrapenning", "Föräldrapenning", from, to, days);
	}

	private static SsbtekIncome income(final String benefit, final String subBenefit, final String amountType,
		final LocalDate from, final LocalDate to, final BigDecimal days) {

		return new SsbtekIncome(benefit, subBenefit, amountType, BigDecimal.valueOf(5000), from, from, to, days, ApplicantRole.APPLICANT);
	}

	private static ClassifiedIncome control(final SsbtekIncome income) {
		return new ClassifiedIncome(income, "TA_MED", "Dagersättning", false, null, false);
	}

	private static ClassifiedIncome comparison(final SsbtekIncome income) {
		return new ClassifiedIncome(income, "TA_MED", "Dagersättning", false, null, true);
	}
}
