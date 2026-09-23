package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.DayCheckBasis;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EconomicDecisionPeriod;
import se.sundsvall.caremanagement.types.financialassistance.service.PeriodRulesService.DayCheck;
import se.sundsvall.caremanagement.types.financialassistance.service.PeriodRulesService.PeriodVerdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodRuleFeederTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final YearMonth CONTROL_MONTH = YearMonth.of(2026, 9);
	private static final String W1 = "Det finns ekonomiskt beslut hos Arbetsförmedlingen och 450 dagar är inte förbrukade men det saknas utbetalning i SSBTEK denna månad";
	private static final String W2 = "Det finns ekonomiskt beslut hos Arbetsförmedlingen och 450 dagar är inte förbrukade men antal utbetalda dagar stämmer inte överens med icke-röda dagar föregående månad";

	/** AF decision covering the control month, FK days left: the gate is open. */
	private static final DayCheckBasis OPEN_GATE = DayCheckBasis.create()
		.withEconomicDecisionPeriods(List.of(new EconomicDecisionPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31))))
		.withConsumedDays(212)
		.withAllDaysConsumed(false);

	@Mock
	private PeriodRulesService periodRulesServiceMock;

	@InjectMocks
	private PeriodRuleFeeder feeder;

	// ----------------------------------------------------------------------------------------------------------------
	// No payment in the control month -> the "saknas utbetalning" question
	// ----------------------------------------------------------------------------------------------------------------

	@Test
	void noPaymentAsksTheTableAboutTheAbsenceAndRaisesWarningOne() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(new PeriodVerdict(true, W1));

		final var warnings = feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(), OPEN_GATE);

		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(true, false, false, null, null, null, null));
		assertThat(warnings).singleElement().satisfies(warning -> {
			assertThat(warning.type()).isEqualTo(WarningService.TYPE_SSBTEK_DAY_CHECK);
			assertThat(warning.sourceKey()).isEqualTo("DAGERSATTNING:SAKNAS:2026-09");
			assertThat(warning.message()).isEqualTo(W1);
		});
	}

	@Test
	void anotherIncomeIsNotAPayment() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(PeriodVerdict.none());
		final var housingAllowance = control(income("Bostadsbidrag", "Bostadsbidrag", "Bostadsbidrag",
			LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), BigDecimal.valueOf(12)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(housingAllowance), OPEN_GATE)).isEmpty();
		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(true, false, false, null, null, null, null));
	}

	@Test
	void aComparisonPeriodPaymentIsNotAControlMonthPayment() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(PeriodVerdict.none());
		final var lastMonths = comparison(dayBenefit(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), BigDecimal.valueOf(23)));

		feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(lastMonths), OPEN_GATE);

		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(true, false, false, null, null, null, null));
	}

	@Test
	void foraldrapenningIsNeverJudgedAsAPayment() {
		// Verksamheten 2026-09-21: föräldrapenning is off the rålista and its day/gap checks are retired. It is neither
		// checked nor mistaken for the aktivitetsstöd the table asks about.
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(PeriodVerdict.none());
		final var parental = control(income("Dagersättning", "Föräldrapenning", "Föräldrapenning",
			LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), BigDecimal.valueOf(3)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(parental), OPEN_GATE)).isEmpty();
		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(true, false, false, null, null, null, null));
	}

	@Test
	void aSplitDayAllowanceSuppressesTheMissingPaymentQuestion() {
		// FK splitting a payment across several utbetalningsdetalj rows leaves sub-benefit and amount type null, so it
		// cannot be routed. It may well be the aktivitetsstöd - claiming it is missing would be a fabricated finding.
		final var split = control(income("Dagersättning", null, null,
			LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), BigDecimal.valueOf(21)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(split), OPEN_GATE)).isEmpty();
		verifyNoInteractions(periodRulesServiceMock);
	}

	@Test
	void noIncomesAndNoBasisStillAsksTheTableWhichStopsOnTheUnreadGate() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(PeriodVerdict.none());

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, null, null)).isEmpty();
		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(null, null, false, null, null, null, null));
	}

	// ----------------------------------------------------------------------------------------------------------------
	// A payment -> the day count against the non-red days of the month it covers
	// ----------------------------------------------------------------------------------------------------------------

	@Test
	void aWholeMonthPaymentIsComparedWithTheNonRedDaysOfTheMonthItCovers() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(new PeriodVerdict(true, W2));
		// Paid in September (the control month), covering August: August 2026 has 21 non-red days and no eve.
		final var payment = control(dayBenefit(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), BigDecimal.valueOf(21.5)));

		final var warnings = feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(payment), OPEN_GATE);

		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(true, false, true, true, BigDecimal.valueOf(21.5), 21, 21));
		assertThat(warnings).singleElement().satisfies(warning -> {
			assertThat(warning.type()).isEqualTo(WarningService.TYPE_SSBTEK_DAY_CHECK);
			assertThat(warning.sourceKey()).isEqualTo("DAGERSATTNING:2026-08-01");
			assertThat(warning.message()).isEqualTo(W2);
		});
	}

	@Test
	void aMonthWithEvesSendsBothReadings() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(PeriodVerdict.none());
		final var payment = control(dayBenefit(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), BigDecimal.valueOf(23)));

		feeder.periodWarnings(MUNICIPALITY_ID, YearMonth.of(2027, 1), List.of(payment),
			DayCheckBasis.create().withEconomicDecisionPeriods(List.of(period(LocalDate.of(2026, 8, 1), null))).withAllDaysConsumed(false));

		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(true, false, true, true, BigDecimal.valueOf(23), 22, 20));
	}

	@Test
	void aPeriodSpanningTwoMonthsIsUnreadable() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(new PeriodVerdict(true, "Gick ej att läsa ut antal dagar"));
		final var payment = control(dayBenefit(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), BigDecimal.valueOf(40)));

		final var warnings = feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(payment), OPEN_GATE);

		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(true, false, true, false, BigDecimal.valueOf(40), null, null));
		assertThat(warnings).singleElement().satisfies(warning -> assertThat(warning.sourceKey()).isEqualTo("DAGERSATTNING:2026-07-01"));
	}

	@Test
	void aMissingPeriodIsUnreadableAndKeyedOnThePaymentDate() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(new PeriodVerdict(true, "Gick ej att läsa ut antal dagar"));
		final var payment = control(new SsbtekIncome("Dagersättning", "Arbetsmarknadspolitiskt program", "Aktivitetsstöd",
			BigDecimal.valueOf(5000), LocalDate.of(2026, 9, 25), null, null, BigDecimal.valueOf(22), ApplicantRole.APPLICANT));

		final var warnings = feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(payment), OPEN_GATE);

		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(true, false, true, false, BigDecimal.valueOf(22), null, null));
		assertThat(warnings).singleElement().satisfies(warning -> assertThat(warning.sourceKey()).isEqualTo("DAGERSATTNING:2026-09-25"));
	}

	@Test
	void aPaymentWithoutPeriodOrDateGetsAFallbackKey() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(new PeriodVerdict(true, "Gick ej att läsa ut antal dagar"));
		final var payment = control(new SsbtekIncome("Dagersättning", "Dagersättning", "Etableringsersättning",
			BigDecimal.valueOf(5000), null, null, null, BigDecimal.valueOf(22), ApplicantRole.APPLICANT));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(payment), OPEN_GATE))
			.singleElement().satisfies(warning -> assertThat(warning.sourceKey()).isEqualTo("DAGERSATTNING:okand-period"));
	}

	@Test
	void everyPaymentIsJudgedOnItsOwn() {
		when(periodRulesServiceMock.dayCheck(any(), any()))
			.thenReturn(new PeriodVerdict(true, W2))
			.thenReturn(PeriodVerdict.none());
		final var first = control(dayBenefit(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), BigDecimal.valueOf(20)));
		final var second = control(income("Dagersättning", "Dagersättning", "Utvecklingsersättning",
			LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), BigDecimal.valueOf(24)));

		final var warnings = feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(first, second), OPEN_GATE);

		verify(periodRulesServiceMock, times(2)).dayCheck(eq(MUNICIPALITY_ID), any());
		assertThat(warnings).singleElement().satisfies(warning -> assertThat(warning.sourceKey()).isEqualTo("DAGERSATTNING:2026-08-01"));
	}

	@Test
	void selectionIsCaseAndWhitespaceInsensitive() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(new PeriodVerdict(true, W2));
		final var payment = control(income("  DAGERSÄTTNING ", "Arbetsmarknadspolitiskt Pgm", "AKTIVITETSSTÖD",
			LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), BigDecimal.valueOf(22)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(payment), OPEN_GATE)).hasSize(1);
		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(true, false, true, true, BigDecimal.valueOf(22), 21, 21));
	}

	@Test
	void aVerdictWithoutAWarningRaisesNothing() {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(PeriodVerdict.none());
		final var payment = control(dayBenefit(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), BigDecimal.valueOf(21)));

		assertThat(feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(payment), OPEN_GATE)).isEmpty();
	}

	// ----------------------------------------------------------------------------------------------------------------
	// The gate, as resolved from the basis
	// ----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name = "{0}")
	@MethodSource("gates")
	void theGateIsResolvedFromTheBasis(final String name, final DayCheckBasis basis, final Boolean economicDecision, final Boolean allDaysConsumed) {
		when(periodRulesServiceMock.dayCheck(any(), any())).thenReturn(PeriodVerdict.none());

		feeder.periodWarnings(MUNICIPALITY_ID, CONTROL_MONTH, List.of(), basis);

		verify(periodRulesServiceMock).dayCheck(MUNICIPALITY_ID, new DayCheck(economicDecision, allDaysConsumed, false, null, null, null, null));
		verifyNoMoreInteractions(periodRulesServiceMock);
	}

	private static Stream<Arguments> gates() {
		final var withNullEntry = new ArrayList<EconomicDecisionPeriod>();
		withNullEntry.add(null);
		withNullEntry.add(period(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 20)));

		return Stream.of(
			Arguments.of("no basis at all: both halves unread", null, null, null),
			Arguments.of("empty basis: both halves unread", DayCheckBasis.create(), null, null),
			Arguments.of("AF answered with no decision", DayCheckBasis.create().withEconomicDecisionPeriods(List.of()), false, null),
			Arguments.of("AF decision ended before the control month", afOnly(period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 31))), false, null),
			Arguments.of("AF decision starts after the control month", afOnly(period(LocalDate.of(2026, 10, 1), null)), false, null),
			Arguments.of("AF decision open-ended from before", afOnly(period(LocalDate.of(2026, 1, 1), null)), true, null),
			Arguments.of("AF decision without a start date", afOnly(period(null, LocalDate.of(2026, 9, 1))), true, null),
			Arguments.of("AF decision inside the month, null entries ignored", DayCheckBasis.create().withEconomicDecisionPeriods(withNullEntry), true, null),
			Arguments.of("FK flag: all consumed", DayCheckBasis.create().withAllDaysConsumed(true), null, true),
			Arguments.of("FK flag: days left", DayCheckBasis.create().withAllDaysConsumed(false).withConsumedDays(100), null, false),
			Arguments.of("FK count only: 450 reached", DayCheckBasis.create().withConsumedDays(450), null, true),
			Arguments.of("FK count only: days left", DayCheckBasis.create().withConsumedDays(449), null, false),
			Arguments.of("FK count reaches 450 despite the flag", DayCheckBasis.create().withAllDaysConsumed(false).withConsumedDays(450), null, true),
			Arguments.of("both answered, gate open", OPEN_GATE, true, false));
	}

	// ----------------------------------------------------------------------------------------------------------------

	private static DayCheckBasis afOnly(final EconomicDecisionPeriod period) {
		return DayCheckBasis.create().withEconomicDecisionPeriods(List.of(period));
	}

	private static EconomicDecisionPeriod period(final LocalDate from, final LocalDate to) {
		return new EconomicDecisionPeriod(from, to);
	}

	private static SsbtekIncome dayBenefit(final LocalDate from, final LocalDate to, final BigDecimal days) {
		return income("Dagersättning", "Arbetsmarknadspolitiskt program", "Aktivitetsstöd", from, to, days);
	}

	private static SsbtekIncome income(final String benefit, final String subBenefit, final String amountType,
		final LocalDate from, final LocalDate to, final BigDecimal days) {

		return new SsbtekIncome(benefit, subBenefit, amountType, BigDecimal.valueOf(5000), LocalDate.of(2026, 9, 25), from, to, days, ApplicantRole.APPLICANT);
	}

	private static ClassifiedIncome control(final SsbtekIncome income) {
		return new ClassifiedIncome(income, "TA_MED", "Dagersättning", false, null, false);
	}

	private static ClassifiedIncome comparison(final SsbtekIncome income) {
		return new ClassifiedIncome(income, "TA_MED", "Dagersättning", false, null, true);
	}
}
