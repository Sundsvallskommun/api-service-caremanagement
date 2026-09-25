package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.CalculationService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncomeLines;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.Completeness;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveExpense;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.EffectivePerson;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.IncomeTypeTotal;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousFamily;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.DayCheckBasis;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.JUNE;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceCalculationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private CalculationService calculationServiceMock;

	@Mock
	private LifecareCaseService lifecareCaseServiceMock;

	@Mock
	private CitizenService citizenServiceMock;

	@Mock
	private DecisionService decisionServiceMock;

	@Mock
	private WarningService warningServiceMock;

	@Mock
	private DraftService draftServiceMock;

	@Mock
	private CalculationFeeder calculationFeederMock;

	@Mock
	private ApplicationRuleFeeder applicationRuleFeederMock;

	@Mock
	private PeriodRuleFeeder periodRuleFeederMock;

	@Mock
	private IncomeChangeFeeder incomeChangeFeederMock;

	@Mock
	private LateTransferFeeder lateTransferFeederMock;

	@Mock
	private UntransferableIncomeFeeder untransferableIncomeFeederMock;

	@Mock
	private PaymentWarningService paymentWarningServiceMock;

	@Mock
	private LifecareServiceIdService lifecareServiceIdServiceMock;

	@Mock
	private CalculationSyncService calculationSyncServiceMock;

	@InjectMocks
	private FinancialAssistanceCalculationService service;

	@Test
	void prepareScopeChecksTheErrandBeforeDoingAnything() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "No errand"));
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoInteractions(citizenServiceMock, repositoryMock, calculationServiceMock, draftServiceMock);
	}

	@Test
	void prepareUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No citizen found for partyId f47ac10b-58cc-4372-a567-0e02b2c3d479");
	}

	@Test
	void prepareRequiresClassifiedIncomes() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: classifiedIncomes is required — the SSBTEK rules is evaluated in the process, not caremanagement");
	}

	@Test
	void prepareOnAnSsbtekReadFailureOnlyWarnsAndLeavesTheCalculationAlone() {
		// Verksamhetens regelverk: do not run the rules on data we could not read. Refreshing the draft with no incomes
		// would clear rows a previous run transferred, and the once-only recommendation would freeze "no warnings" onto
		// an errand nobody managed to check.
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID);
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withSsbtekError(true);

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(warningServiceMock).reconcileSsbtekReadFailure(ERRAND_ID, true);
		verify(warningServiceMock, never()).reconcileCalculationWarnings(any(), any(), any(), any(), any(), any(), any());
		verify(warningServiceMock, never()).reconcileRuleWarnings(any(), any(), any(), any(), any(), any());
		verifyNoInteractions(draftServiceMock, calculationFeederMock, decisionServiceMock, paymentWarningServiceMock, incomeChangeFeederMock);
		assertThat(response.isInformationComplete()).isFalse();
		assertThat(errand.getLastDailyRunAt()).isNotNull();
	}

	@Test
	void prepareMissingErrandYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No financial-assistance errand for id errand-1");
	}

	@ParameterizedTest
	@CsvSource(value = {
		"Riksnorm 2025, Riksnorm",
		"'  Matnorm 2026 ', Matnorm",
		"Riksnorm, Riksnorm",
		"'  ', NULL",
		"NULL, NULL"
	}, nullValues = "NULL")
	void previousNormNamesDropTheYear(final String previousNorm, final String expected) {
		final var names = FinancialAssistanceCalculationService.previousNormNames(previousNorm);

		if (expected == null) {
			assertThat(names).isEmpty();
		} else {
			assertThat(names).containsExactly(expected);
		}
	}

	@Test
	void prepareTakesTheNormFromThePreviousCalculationAndFlagsWhatCannotBeCopied() {
		final var month = YearMonth.of(2026, JUNE);
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM"));
		final var family = new PreviousFamily(List.of(new PreviousFamily.Member("p1", "NILSSON KARIN", null, null)), true, BigDecimal.valueOf(800));
		final var personRows = List.of(FaNormPersonEntity.create().withPartyId("p1"));
		final var familyWarning = new WarningService.WarningInput(WarningService.TYPE_FAMILY_DIFFERS_FROM_APPLICATION, "not-in-previous:p2", "text");
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("AWAITING_DECISION"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month))
			.thenReturn(new PreviousHousehold(Set.of(APPLICANT_PARTY_ID), true, 1, null, null, "Specnorm 2025"));
		when(lifecareCaseServiceMock.previousFamily(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenReturn(family);
		when(calculationFeederMock.personRows(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(errand), any(), eq(family))).thenReturn(personRows);
		when(calculationFeederMock.familyWarnings(errand, family)).thenReturn(List.of(familyWarning));
		when(calculationFeederMock.commonHouseholdCostWarnings(family, errand)).thenReturn(List.of());
		// the previous norm is asked for first, by name without the year; the month no longer offers it
		when(calculationServiceMock.applicationIncomeLines(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any())).thenReturn(new ApplicationIncomeLines(List.of(), List.of()));
		when(calculationServiceMock.selectNormId(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), eq(month), eq(List.of("Specnorm")), any()))
			.thenReturn(new CalculationService.NormChoice(1, false));

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]"));

		verify(draftServiceMock).refresh(eq(ERRAND_ID), eq("2026-06"), eq(1), eq(List.of("NATIONAL_NORM")), eq(personRows), any(), any());
		final ArgumentCaptor<List<WarningService.WarningInput>> warnings = ArgumentCaptor.captor();
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID), any(), any(), any(), any(), warnings.capture(), eq(Set.of()));
		// The family was read, so any earlier read-failure warning closes itself.
		verify(warningServiceMock).reconcileLifecareReadFailure(ERRAND_ID, "lifecare-read:previous-family", false);
		assertThat(warnings.getValue()).contains(familyWarning)
			.anySatisfy(warning -> {
				assertThat(warning.type()).isEqualTo(WarningService.TYPE_PREVIOUS_NORM_NOT_AVAILABLE);
				assertThat(warning.message()).isEqualTo(
					"Normen i föregående normberäkning (Specnorm 2025) finns inte för ansökningsmånaden – normen är vald efter ansökan, kontrollera den");
			});
	}

	@Test
	void prepareWithAFailedPreviousFamilyReadRaisesTheReadFailureAndLeavesTheFamilyWarningsAlone() {
		// A failed read is not "no previous calculation": the NORM-04 warnings from the last successful read must not be
		// auto-closed on its strength, and the handläggare is told the family could not be checked.
		final var month = YearMonth.of(2026, JUNE);
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM"));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("AWAITING_DECISION"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenReturn(PreviousHousehold.empty());
		when(lifecareCaseServiceMock.previousFamily(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare down"));
		when(calculationServiceMock.applicationIncomeLines(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any())).thenReturn(new ApplicationIncomeLines(List.of(), List.of()));
		when(calculationServiceMock.selectNormId(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), eq(month), eq(List.of()), any())).thenReturn(new CalculationService.NormChoice(7, false));

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]"));

		// The household falls back to the application, as before ...
		verify(calculationFeederMock).personRows(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(errand), any(), eq(PreviousFamily.empty()));
		// ... but no family warning is computed from the empty family, the family types are left as they were ...
		verify(calculationFeederMock, never()).familyWarnings(any(), any());
		verify(calculationFeederMock, never()).commonHouseholdCostWarnings(any(), any());
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID), any(), any(), any(), any(), any(), eq(WarningService.PREVIOUS_FAMILY_TYPES));
		// ... and the failure is raised.
		verify(warningServiceMock).reconcileLifecareReadFailure(ERRAND_ID, "lifecare-read:previous-family", true);
	}

	@Test
	void prepareWarnsForTheIncomesTheDraftCouldNotTake() {
		// An income the rules transfer but no Lifecare type takes is absent from the draft; the warning is what tells the
		// handläggare, and it is built from the transfer's own filter rather than a second reading of the rules.
		final var month = YearMonth.of(2026, JUNE);
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM"));
		final var untransferable = List.of(new ClassifiedIncome(
			new SsbtekIncome("Studiemedel", null, null, BigDecimal.valueOf(2500), LocalDate.of(2026, 5, 25), ApplicantRole.APPLICANT),
			"TA_MED", "Studiemedel", false, "Ta med"));
		final var warning = new WarningService.WarningInput(WarningService.TYPE_INCOME_NOT_TRANSFERABLE, "studiemedel|APPLICANT", "text");
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("AWAITING_DECISION"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));
		noPreviousCalculation(month);
		when(calculationServiceMock.untransferableIncomes(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[]")).thenReturn(untransferable);
		when(untransferableIncomeFeederMock.untransferableIncomeWarnings(untransferable, Map.of())).thenReturn(List.of(warning));

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]"));

		final ArgumentCaptor<List<WarningService.WarningInput>> warnings = ArgumentCaptor.captor();
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID), any(), any(), any(), any(), warnings.capture(), eq(Set.of()));
		assertThat(warnings.getValue()).contains(warning);
	}

	@Test
	void prepareFeedsTheApplicationsDeclaredIncomesIntoTheDraftAsRowsOfTheirOwn() {
		// Swish, lön and the rest SSBTEK never reports come from the application: they go into the draft next to the
		// SSBTEK rows, and one no Lifecare type takes is warned about rather than left out without a trace.
		final var month = YearMonth.of(2026, JUNE);
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM"));
		final var declared = List.of(new ApplicationIncome("SWISH_DEPOSITS", null, BigDecimal.valueOf(599), LocalDate.of(2026, 5, 24), "Swish/kontoinsättningar"));
		final var ssbtekLines = List.of(new FamilyCareIncomeLine(2, "Bostadsbidrag", "APPLICANT", BigDecimal.valueOf(4500), null, "SSBTEK: Bostadsbidrag"));
		final var applicationLines = List.of(new FamilyCareIncomeLine(30, "Swish/Insättningar/Överföringar", "APPLICANT", BigDecimal.valueOf(599), null, "Ansökan: Swish/kontoinsättningar"));
		final var notTaken = new ApplicationIncome("RENT_SHARE_FROM_CHILD", null, BigDecimal.TEN, null, "Hyresdel från barn");
		final var ssbtekRow = FaNormIncomeEntity.create().withTypeId(2).withOrigin("SYSTEM");
		final var applicationRow = FaNormIncomeEntity.create().withTypeId(30).withOrigin("APPLICATION");
		final var warning = new WarningService.WarningInput(WarningService.TYPE_INCOME_NOT_TRANSFERABLE, "application|rent_share_from_child|applicant", "text");
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("AWAITING_DECISION"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenReturn(PreviousHousehold.empty());
		when(lifecareCaseServiceMock.previousFamily(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenReturn(PreviousFamily.empty());
		when(calculationServiceMock.selectNormId(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), eq(month), eq(List.of()), any())).thenReturn(new CalculationService.NormChoice(7, false));
		when(calculationServiceMock.incomeLines(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[]", Map.of())).thenReturn(ssbtekLines);
		when(calculationFeederMock.incomeRows(ERRAND_ID, ssbtekLines)).thenReturn(List.of(ssbtekRow));
		when(calculationFeederMock.applicationIncomes(errand)).thenReturn(declared);
		when(calculationServiceMock.applicationIncomeLines(MUNICIPALITY_ID, APPLICANT_PARTY_ID, declared)).thenReturn(new ApplicationIncomeLines(applicationLines, List.of(notTaken)));
		when(calculationFeederMock.applicationIncomeRows(ERRAND_ID, applicationLines)).thenReturn(List.of(applicationRow));
		when(untransferableIncomeFeederMock.untransferableApplicationIncomeWarnings(List.of(notTaken))).thenReturn(List.of(warning));

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]"));

		verify(draftServiceMock).refresh(eq(ERRAND_ID), eq("2026-06"), eq(7), any(), any(), eq(List.of(ssbtekRow, applicationRow)), any());
		final ArgumentCaptor<List<WarningService.WarningInput>> warnings = ArgumentCaptor.captor();
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID), any(), any(), any(), any(), warnings.capture(), eq(Set.of()));
		assertThat(warnings.getValue()).contains(warning);
	}

	/** A first application: no previous normberäkning, so the norm comes from the application. */
	private void noPreviousCalculation(final YearMonth month) {
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenReturn(PreviousHousehold.empty());
		when(lifecareCaseServiceMock.previousFamily(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenReturn(PreviousFamily.empty());
		when(calculationServiceMock.selectNormId(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), eq(month), eq(List.of()), any())).thenReturn(new CalculationService.NormChoice(7, false));
		when(calculationServiceMock.applicationIncomeLines(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any())).thenReturn(new ApplicationIncomeLines(List.of(), List.of()));
	}

	@Test
	void prepareRecordsReviewRequiredRecommendationAndKompletteringWhenIncomplete() {
		final var month = YearMonth.of(2026, JUNE);
		noPreviousCalculation(month);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM"))));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[json]")).thenReturn(new Completeness(false, List.of("Dagersättning")));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("UNDER_REVIEW"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));
		incomeChange(month, "[json]");

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[json]")
			.withUnhandledIncomes(List.of("Bostadstillägg (NOT_ON_WHITELIST)")).withChangeWarnings(List.of("Bostadsbidrag: -23%"));

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getCalculationId()).isNull();
		// careM's own comparison against the previous normberäkning; the engine's period-over-period list is ignored.
		assertThat(response.getChangeWarnings()).containsExactly(INCOME_CHANGE_TEXT);
		assertThat(response.isInformationComplete()).isFalse();
		assertThat(response.getMissingIncomeTypes()).containsExactly("Dagersättning");

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		final var decision = decisionCaptor.getValue();
		assertThat(decision.getDecisionType()).isEqualTo("RECOMMENDATION");
		assertThat(decision.getValue()).isEqualTo("REVIEW_REQUIRED");
		assertThat(decision.getCreatedBy()).isEqualTo("drakel");
		assertThat(decision.getDescription())
			.contains("Ej överförd inkomst: Bostadstillägg (NOT_ON_WHITELIST)")
			.contains(INCOME_CHANGE_TEXT)
			.contains("Saknas fortfarande i SSBTEK: Dagersättning")
			.doesNotContain("-23%");

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo("SUPPLEMENT_REQUESTED");
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID),
			eq(List.of("Bostadstillägg (NOT_ON_WHITELIST)")), eq(List.of(INCOME_CHANGE_TEXT)), eq(List.of("Dagersättning")), any(), any(), eq(Set.of()));
		// No lifecareCalculationId yet: the draft is refreshed, so the full calculation reconcile runs.
		verify(draftServiceMock).refresh(eq(ERRAND_ID), eq("2026-06"), eq(7), eq(List.of("NATIONAL_NORM")), any(), any(), any());
		verify(warningServiceMock, never()).reconcileRuleWarnings(any(), any(), any(), any(), any(), any());
		// No draft header in this run, so there is nothing to propose; the incomplete-basis proposal has its own test.
		verify(calculationServiceMock, never()).commitEffective(any(), any(), any(), any(), any(), any(), any());
		verify(repositoryMock, never()).linkLifecareCalculationIfAbsent(any(), any());
	}

	@Test
	void prepareCreatesTheProposalEvenWhenTheBasisIsIncomplete() {
		// Swish is never reported by SSBTEK, so waiting for a complete basis meant the proposal was never created. What
		// SSBTEK reports later reaches the saved calculation through the sync; what is missing stays a warning.
		final var month = YearMonth.of(2026, JUNE);
		final var errand = completeRunWithDraft(month);
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[]")).thenReturn(new Completeness(false, List.of("Swish")));
		when(calculationServiceMock.commitEffective(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), eq(month), any(), any(), any(), any())).thenReturn(779);
		when(repositoryMock.linkLifecareCalculationIfAbsent(ERRAND_ID, 779)).thenReturn(1);

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, completeRequest());

		assertThat(response.isInformationComplete()).isFalse();
		verify(repositoryMock).linkLifecareCalculationIfAbsent(ERRAND_ID, 779);
		assertThat(errand.getLifecareCalculationId()).isEqualTo(779);
	}

	@Test
	void prepareWithACalculationSavedInLifecareKeepsTheDraftAndItsWarnings() {
		// Draken has saved the normberäkning in Lifecare and set its id on the errand: that calculation is the truth, so
		// the draft is not refreshed and its warnings are not reconciled. The SSBTEK work carries on as before.
		final var month = YearMonth.of(2026, JUNE);
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM")).withLifecareCalculationId(4242);
		final var questionWarning = new WarningService.WarningInput(WarningService.TYPE_PENDING_BENEFIT, "pending-benefit", "text");
		final var previous = new PreviousHousehold(Set.of(APPLICANT_PARTY_ID), true, 1, null, null, "Riksnorm 2025");
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenReturn(previous);
		when(applicationRuleFeederMock.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, errand)).thenReturn(List.of(questionWarning));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[json]")).thenReturn(new Completeness(false, List.of("Dagersättning")));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("UNDER_REVIEW"));
		incomeChange(month, "[json]");

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[json]")
			.withUnhandledIncomes(List.of("Bostadstillägg (NOT_ON_WHITELIST)")).withChangeWarnings(List.of("Bostadsbidrag: -23%"));

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		// The draft is left alone: no refresh, no feed, no family copy, no late transfer, no duplicate read. Only its header
		// is read, for the SSBTEK sync — and without one (as here) there is nothing to sync against.
		verify(draftServiceMock).header(ERRAND_ID);
		verifyNoMoreInteractions(draftServiceMock);
		verifyNoInteractions(lateTransferFeederMock, untransferableIncomeFeederMock, lifecareServiceIdServiceMock, calculationSyncServiceMock);
		// A linked calculation is never proposed again.
		verify(calculationServiceMock, never()).commitEffective(any(), any(), any(), any(), any(), any(), any());
		verify(repositoryMock, never()).linkLifecareCalculationIfAbsent(any(), any());
		verify(calculationFeederMock, never()).incomeRows(any(), any());
		verify(calculationFeederMock, never()).expenseFeed(any(), any(), any(), any(), any());
		verify(calculationFeederMock, never()).personRows(any(), any(), any(), any(), any(), any());
		verify(calculationServiceMock, never()).incomeLines(any(), any(), any(), any(), any());
		verify(calculationServiceMock, never()).selectNormId(any(), any(), any(), any(), any());
		verify(lifecareCaseServiceMock, never()).previousFamily(any(), any(), any());
		// The housing-cost change is frozen with the draft: it concerns the calculation's boendekostnad.
		verify(calculationFeederMock, never()).housingDeltaWarnings(any(), any(), any());

		// Only the SSBTEK income warnings and the draft-independent rule warnings are reconciled.
		verify(warningServiceMock, never()).reconcileCalculationWarnings(any(), any(), any(), any(), any(), any(), any());
		final ArgumentCaptor<List<WarningService.WarningInput>> rules = ArgumentCaptor.captor();
		verify(warningServiceMock).reconcileRuleWarnings(eq(ERRAND_ID), eq(List.of("Bostadstillägg (NOT_ON_WHITELIST)")), eq(List.of(INCOME_CHANGE_TEXT)),
			eq(List.of("Dagersättning")), rules.capture(), eq(Set.of()));
		assertThat(rules.getValue()).containsExactly(questionWarning);
		verify(applicationRuleFeederMock).previousCalculationWarnings(MUNICIPALITY_ID, errand, previous);
		verify(periodRuleFeederMock).periodWarnings(eq(MUNICIPALITY_ID), eq(YearMonth.of(2026, 5)), any(), any());

		// Completeness, the one-time recommendation, the status, the read-failure close and the run stamp are unchanged.
		assertThat(response.isInformationComplete()).isFalse();
		assertThat(response.getMissingIncomeTypes()).containsExactly("Dagersättning");
		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		assertThat(decisionCaptor.getValue().getValue()).isEqualTo("REVIEW_REQUIRED");
		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo("SUPPLEMENT_REQUESTED");
		verify(warningServiceMock).reconcileSsbtekReadFailure(ERRAND_ID, false);
		// A frozen draft does not freeze the medsökande payment warning: it concerns the household, not the draft.
		verify(paymentWarningServiceMock).reconcile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		assertThat(errand.getLastDailyRunAt()).isCloseTo(OffsetDateTime.now(), within(10, SECONDS));
		verify(repositoryMock).save(errand);
	}

	private static final String INCOME_CHANGE_TEXT = "Bostadsbidrag: 1000 kr i föregående normberäkning → 1250 kr nu";

	/** This month's transfer against the previous normberäkning yields one INCOME_CHANGE text. */
	private void incomeChange(final YearMonth month, final String classifiedIncomes) {
		final Optional<Map<String, BigDecimal>> previousAmounts = Optional.of(Map.of("bostadsbidrag", new BigDecimal("1000")));
		final var totals = List.of(new IncomeTypeTotal("Bostadsbidrag", new BigDecimal("1250"), List.of("Bostadsbidrag")));
		when(lifecareCaseServiceMock.previousCalculationIncomeTypeAmounts(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenReturn(previousAmounts);
		when(calculationServiceMock.incomeTypeTotals(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, classifiedIncomes)).thenReturn(totals);
		when(incomeChangeFeederMock.incomeChangeWarnings(MUNICIPALITY_ID, totals, previousAmounts)).thenReturn(List.of(INCOME_CHANGE_TEXT));
	}

	@Test
	void prepareWithAFailedPreviousCalculationReadLeavesTheIncomeChangesUnverified() {
		// A failed read is not "no previous normberäkning": the INCOME_CHANGE warnings from the last successful run must
		// not be auto-closed, and the once-only recommendation must not be written as if nothing had changed.
		final var month = YearMonth.of(2026, JUNE);
		completeRun(month);
		when(lifecareCaseServiceMock.previousCalculationIncomeTypeAmounts(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare down"));

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, completeRequest()
			.withChangeWarnings(List.of("Bostadsbidrag: -23%")));

		assertThat(response.getChangeWarnings()).isEmpty();
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID), any(), eq(List.of()), any(), any(), any(), eq(Set.of(WarningService.TYPE_INCOME_CHANGE)));
		verifyNoInteractions(incomeChangeFeederMock);
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		// The rest of the run carries on.
		verify(paymentWarningServiceMock).reconcile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void prepareWithAFailedPreviousCalculationReadAndAFailedFamilyReadLeavesBothUnverified() {
		final var month = YearMonth.of(2026, JUNE);
		completeRun(month);
		when(lifecareCaseServiceMock.previousFamily(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare down"));
		when(lifecareCaseServiceMock.previousCalculationIncomeTypeAmounts(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare down"));

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, completeRequest());

		final ArgumentCaptor<Set<String>> unverified = ArgumentCaptor.captor();
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID), any(), any(), any(), any(), any(), unverified.capture());
		assertThat(unverified.getValue()).containsExactlyInAnyOrderElementsOf(
			Stream.concat(WarningService.PREVIOUS_FAMILY_TYPES.stream(), Stream.of(WarningService.TYPE_INCOME_CHANGE)).toList());
	}

	@Test
	void prepareWithACalculationSavedInLifecareAndAFailedPreviousCalculationReadLeavesTheIncomeChangesUnverified() {
		final var month = YearMonth.of(2026, JUNE);
		linkedRun(month);
		when(calculationServiceMock.incomeTypeTotals(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[json]")).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare down"));

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, linkedRequest());

		assertThat(response.getChangeWarnings()).isEmpty();
		verify(warningServiceMock).reconcileRuleWarnings(eq(ERRAND_ID), any(), eq(List.of()), any(), any(), eq(Set.of(WarningService.TYPE_INCOME_CHANGE)));
		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
	}

	@Test
	void prepareWithoutAPreviousCalculationComparesNothing() {
		// A nyansökan: the feeder is handed the empty previous side and has nothing to compare with.
		final var month = YearMonth.of(2026, JUNE);
		completeRun(month);
		when(lifecareCaseServiceMock.previousCalculationIncomeTypeAmounts(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenReturn(Optional.empty());

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, completeRequest()
			.withChangeWarnings(List.of("Bostadsbidrag: -23%")));

		verify(incomeChangeFeederMock).incomeChangeWarnings(MUNICIPALITY_ID, List.of(), Optional.empty());
		assertThat(response.getChangeWarnings()).isEmpty();
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID), any(), eq(List.of()), any(), any(), any(), eq(Set.of()));
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
	}

	/** An errand whose normberäkning is saved in Lifecare (4242), with a draft header covering June 2026. */
	private FinancialAssistanceEntity linkedRun(final YearMonth month) {
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM")).withLifecareCalculationId(4242);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month)).thenReturn(PreviousHousehold.empty());
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[json]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("AWAITING_DECISION"));
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.of(FaCalculationDraftEntity.create().withErrandId(ERRAND_ID)));
		return errand;
	}

	private static CalculationRequest linkedRequest() {
		return CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[json]");
	}

	@Test
	void prepareWithACalculationSavedInLifecareComparesSsbtekWithIt() {
		final var month = YearMonth.of(2026, JUNE);
		linkedRun(month);
		final var lines = List.of(new FamilyCareIncomeLine(20, "Lön", "APPLICANT", new BigDecimal("12400"), null, null));
		final var rows = List.of(FaNormIncomeEntity.create().withTypeId(20).withTypeName("Lön").withApplicantProcessAmount(new BigDecimal("12400")));
		when(calculationServiceMock.incomeLines(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[json]", Map.of())).thenReturn(lines);
		when(calculationFeederMock.incomeRows(ERRAND_ID, lines)).thenReturn(rows);

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, linkedRequest());

		// This run's SSBTEK amounts are recorded — not merged into the frozen draft — and compared with the saved
		// calculation over the draft's period, which falls back to the application month when the header has none.
		verify(calculationSyncServiceMock).recordSsbtek(ERRAND_ID, rows);
		verify(calculationSyncServiceMock).reconcileWarnings(MUNICIPALITY_ID, ERRAND_ID, APPLICANT_PARTY_ID, 4242, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
		verify(draftServiceMock, never()).refresh(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void prepareWithACalculationSavedInLifecareCompletesWhenTheSsbtekSyncFails() {
		final var month = YearMonth.of(2026, JUNE);
		final var errand = linkedRun(month);
		when(calculationServiceMock.incomeLines(any(), any(), any(), any(), any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare said no"));

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, linkedRequest());

		// Best-effort: the comparison is skipped and the rest of the run completes as usual.
		verifyNoInteractions(calculationSyncServiceMock);
		assertThat(response.isInformationComplete()).isTrue();
		verify(paymentWarningServiceMock).reconcile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(repositoryMock).save(errand);
	}

	@Test
	void prepareRecordsOkRecommendationAndVantarWhenComplete() {
		final var month = YearMonth.of(2026, JUNE);
		noPreviousCalculation(month);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("SUPPLEMENT_REQUESTED"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));

		final var dayCheckBasis = DayCheckBasis.create().withAllDaysConsumed(false);
		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]")
			.withDayCheckBasis(dayCheckBasis);

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		// The day check gets the control month (the month before the application month) and the gate facts as sent.
		verify(periodRuleFeederMock).periodWarnings(eq(MUNICIPALITY_ID), eq(YearMonth.of(2026, 5)), any(), eq(dayCheckBasis));

		final var decisionCaptor = ArgumentCaptor.forClass(Decision.class);
		verify(decisionServiceMock).create(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), decisionCaptor.capture());
		assertThat(decisionCaptor.getValue().getValue()).isEqualTo("OK");
		assertThat(decisionCaptor.getValue().getDescription()).contains("Inga varningar");

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo("AWAITING_DECISION");

		// The daily loop stamps the errand with its run time.
		final var entityCaptor = ArgumentCaptor.forClass(FinancialAssistanceEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getLastDailyRunAt()).isCloseTo(OffsetDateTime.now(), within(10, SECONDS));

		// A run that read SSBTEK closes any read-failure warning an earlier run left behind.
		verify(warningServiceMock).reconcileSsbtekReadFailure(ERRAND_ID, false);
		// The medsökande payment warning follows the household on every run.
		verify(paymentWarningServiceMock).reconcile(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	/** A complete run on an errand with no linked calculation. */
	private FinancialAssistanceEntity completeRun(final YearMonth month) {
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID);
		noPreviousCalculation(month);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("SUPPLEMENT_REQUESTED"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));
		return errand;
	}

	/** {@link #completeRun}, with a refreshed draft that has one row per section. */
	private FinancialAssistanceEntity completeRunWithDraft(final YearMonth month) {
		final var errand = completeRun(month);
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.of(FaCalculationDraftEntity.create().withErrandId(ERRAND_ID).withNormId(7)
			.withCalculationFromDate(LocalDate.of(2026, 6, 1)).withCalculationToDate(LocalDate.of(2026, 6, 30)).withCalculationDate(LocalDate.of(2026, 6, 1))));
		when(draftServiceMock.liveIncomes(ERRAND_ID)).thenReturn(List.of(FaNormIncomeEntity.create().withTypeId(20).withApplicantProcessAmount(new BigDecimal("5000"))));
		when(draftServiceMock.liveExpenses(ERRAND_ID)).thenReturn(List.of(FaNormExpenseEntity.create().withCostType("HOUSING_COST").withProcessAmount(new BigDecimal("6500"))));
		when(draftServiceMock.livePersons(ERRAND_ID)).thenReturn(List.of(FaNormPersonEntity.create().withPartyId(APPLICANT_PARTY_ID).withProcessDays(30)));
		when(lifecareServiceIdServiceMock.currentOrResolve(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(55);
		return errand;
	}

	private static CalculationRequest completeRequest() {
		return CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");
	}

	@Test
	void prepareCreatesTheProposalInLifecareOnTheFirstRun() {
		final var month = YearMonth.of(2026, JUNE);
		final var errand = completeRunWithDraft(month);
		final var allIncomes = List.of(FaNormIncomeEntity.create().withTypeId(20), FaNormIncomeEntity.create().withTypeId(21).withDeleted(true));
		when(draftServiceMock.allIncomes(ERRAND_ID)).thenReturn(allIncomes);
		when(calculationServiceMock.commitEffective(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), eq(month), any(), any(), any(), any())).thenReturn(777);
		when(repositoryMock.linkLifecareCalculationIfAbsent(ERRAND_ID, 777)).thenReturn(1);

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, completeRequest());

		// The draft's effective rows go to Lifecare with the header's norm and period and the errand's EB insats.
		final ArgumentCaptor<CalculationHeader> header = ArgumentCaptor.captor();
		final ArgumentCaptor<List<EffectiveIncome>> incomes = ArgumentCaptor.captor();
		final ArgumentCaptor<List<EffectiveExpense>> expenses = ArgumentCaptor.captor();
		final ArgumentCaptor<List<EffectivePerson>> persons = ArgumentCaptor.captor();
		verify(calculationServiceMock).commitEffective(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), eq(month), header.capture(), incomes.capture(), expenses.capture(), persons.capture());
		assertThat(header.getValue().normId()).isEqualTo(7);
		assertThat(header.getValue().calculationFromDate()).isEqualTo(LocalDate.of(2026, 6, 1));
		assertThat(header.getValue().calculationToDate()).isEqualTo(LocalDate.of(2026, 6, 30));
		assertThat(header.getValue().serviceId()).isEqualTo(55);
		assertThat(incomes.getValue()).singleElement().satisfies(income -> assertThat(income.applicantAmount()).isEqualByComparingTo("5000"));
		assertThat(expenses.getValue()).singleElement().satisfies(expense -> assertThat(expense.approvedAmount()).isEqualByComparingTo("6500"));
		assertThat(persons.getValue()).singleElement().satisfies(person -> assertThat(person.partyId()).isEqualTo(APPLICANT_PARTY_ID));

		// Linked on the errand, and the loaded entity carries the id so the run stamp cannot write the old value back.
		verify(repositoryMock).linkLifecareCalculationIfAbsent(ERRAND_ID, 777);
		// What was posted becomes the baseline later SSBTEK changes are measured against — deleted rows included.
		verify(calculationSyncServiceMock).seedFromProposal(ERRAND_ID, allIncomes);
		assertThat(errand.getLifecareCalculationId()).isEqualTo(777);
		verify(repositoryMock).save(errand);
	}

	@Test
	void prepareLeavesTheProposalToTheNextRunWhenLifecareRefusesIt() {
		final var month = YearMonth.of(2026, JUNE);
		final var errand = completeRunWithDraft(month);
		when(calculationServiceMock.commitEffective(any(), any(), any(), any(), any(), any(), any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "Lifecare said no"));

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, completeRequest());

		// Best-effort: nothing is linked, and the rest of the run completes as usual.
		verify(repositoryMock, never()).linkLifecareCalculationIfAbsent(any(), any());
		assertThat(errand.getLifecareCalculationId()).isNull();
		assertThat(response.isInformationComplete()).isTrue();
		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo("AWAITING_DECISION");
		verify(repositoryMock).save(errand);
	}

	@Test
	void prepareLeavesItsProposalUnlinkedWhenDrakenLinkedOneFirst() {
		final var month = YearMonth.of(2026, JUNE);
		final var errand = completeRunWithDraft(month);
		when(calculationServiceMock.commitEffective(any(), any(), any(), any(), any(), any(), any())).thenReturn(778);
		when(repositoryMock.linkLifecareCalculationIfAbsent(ERRAND_ID, 778)).thenReturn(0);

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, completeRequest());

		// The BFF's link stands: the loaded entity is not given this run's id, so the stamp never overwrites it.
		assertThat(errand.getLifecareCalculationId()).isNull();
		// Nor is a calculation this run does not own taken as the baseline.
		verifyNoInteractions(calculationSyncServiceMock);
		verify(repositoryMock).save(errand);
	}

	@Test
	void prepareDoesNotProposeWithoutADraftHeader() {
		final var month = YearMonth.of(2026, JUNE);
		completeRun(month);
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.empty());

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, completeRequest());

		verify(calculationServiceMock, never()).commitEffective(any(), any(), any(), any(), any(), any(), any());
		verify(repositoryMock, never()).linkLifecareCalculationIfAbsent(any(), any());
	}

	@Test
	void prepareDoesNotDuplicateRecommendationOrRewriteUnchangedStatus() {
		final var month = YearMonth.of(2026, JUNE);
		noPreviousCalculation(month);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, APPLICANT_PARTY_ID, month, "[]")).thenReturn(new Completeness(true, List.of()));
		// a recommendation already exists, and the errand is already in the target status
		when(decisionServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(Decision.create().withDecisionType("RECOMMENDATION")));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("AWAITING_DECISION"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		verify(decisionServiceMock, never()).create(any(), any(), any(), any());
		verify(errandServiceMock, never()).updateErrand(any(), any(), any(), any());
	}

	@Test
	void getDraftReturnsDraftAfterScopeCheck() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var draft = CalculationDraft.create().withApplicationMonth("2026-06");
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(draft);

		assertThat(service.getDraft(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isSameAs(draft);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void patchDraftHeaderScopeChecksThenDelegates() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		final var draft = CalculationDraft.create().withNormId(5);
		when(draftServiceMock.patchHeader(eq(ERRAND_ID), any(NormHeaderInput.class))).thenReturn(draft);

		assertThat(service.patchDraftHeader(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new NormHeaderInput().withHouseholdSize(1))).isSameAs(draft);
		verify(draftServiceMock).patchHeader(eq(ERRAND_ID), any(NormHeaderInput.class));
	}
}
