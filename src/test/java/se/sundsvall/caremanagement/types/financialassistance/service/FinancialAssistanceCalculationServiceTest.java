package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import se.sundsvall.caremanagement.lifecare.service.model.Completeness;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousFamily;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.DayCheckBasis;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
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
import static org.mockito.Mockito.when;
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
	private MissingIncomeFeeder missingIncomeFeederMock;

	@Mock
	private LateTransferFeeder lateTransferFeederMock;

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
		verify(warningServiceMock, never()).reconcileCalculationWarnings(any(), any(), any(), any(), any(), any());
		verify(warningServiceMock, never()).reconcileRuleWarnings(any(), any(), any(), any(), any());
		verifyNoInteractions(draftServiceMock, calculationFeederMock, decisionServiceMock);
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
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, "199001011234", month, "[]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("AWAITING_DECISION"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, "199001011234", month))
			.thenReturn(new PreviousHousehold(Set.of("199001011234"), true, 1, null, null, "Specnorm 2025"));
		when(lifecareCaseServiceMock.previousFamily(MUNICIPALITY_ID, "199001011234", month)).thenReturn(family);
		when(calculationFeederMock.personRows(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), eq(errand), any(), eq(family))).thenReturn(personRows);
		when(calculationFeederMock.familyWarnings(errand, family)).thenReturn(List.of(familyWarning));
		when(calculationFeederMock.commonHouseholdCostWarnings(family, personRows)).thenReturn(List.of());
		// the previous norm is asked for first, by name without the year; the month no longer offers it
		when(calculationServiceMock.selectNormId(eq(MUNICIPALITY_ID), eq("199001011234"), eq(month), eq(List.of("Specnorm")), any()))
			.thenReturn(new CalculationService.NormChoice(1, false));

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]"));

		verify(draftServiceMock).refresh(eq(ERRAND_ID), eq("2026-06"), eq(1), eq(List.of("NATIONAL_NORM")), eq(personRows), any(), any());
		final ArgumentCaptor<List<WarningService.WarningInput>> warnings = ArgumentCaptor.captor();
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID), any(), any(), any(), any(), warnings.capture());
		assertThat(warnings.getValue()).contains(familyWarning)
			.anySatisfy(warning -> {
				assertThat(warning.type()).isEqualTo(WarningService.TYPE_PREVIOUS_NORM_NOT_AVAILABLE);
				assertThat(warning.message()).isEqualTo(
					"Normen i föregående normberäkning (Specnorm 2025) finns inte för ansökningsmånaden – normen är vald efter ansökan, kontrollera den");
			});
	}

	/** A first application: no previous normberäkning, so the norm comes from the application. */
	private void noPreviousCalculation(final YearMonth month) {
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, "199001011234", month)).thenReturn(PreviousHousehold.empty());
		when(lifecareCaseServiceMock.previousFamily(MUNICIPALITY_ID, "199001011234", month)).thenReturn(PreviousFamily.empty());
		when(calculationServiceMock.selectNormId(eq(MUNICIPALITY_ID), eq("199001011234"), eq(month), eq(List.of()), any())).thenReturn(new CalculationService.NormChoice(7, false));
	}

	@Test
	void prepareRecordsReviewRequiredRecommendationAndKompletteringWhenIncomplete() {
		final var month = YearMonth.of(2026, JUNE);
		noPreviousCalculation(month);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM"))));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, "199001011234", month, "[json]")).thenReturn(new Completeness(false, List.of("Dagersättning")));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("UNDER_REVIEW"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[json]")
			.withUnhandledIncomes(List.of("Bostadstillägg (NOT_ON_WHITELIST)")).withChangeWarnings(List.of("Bostadsbidrag: -23%"));

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getCalculationId()).isNull();
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
			.contains("Saknas fortfarande i SSBTEK: Dagersättning");

		final var patchCaptor = ArgumentCaptor.forClass(PatchErrand.class);
		verify(errandServiceMock).updateErrand(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		assertThat(patchCaptor.getValue().getStatus()).isEqualTo("SUPPLEMENT_REQUESTED");
		verify(warningServiceMock).reconcileCalculationWarnings(eq(ERRAND_ID),
			eq(List.of("Bostadstillägg (NOT_ON_WHITELIST)")), eq(List.of("Bostadsbidrag: -23%")), eq(List.of("Dagersättning")), any(), any());
		// No lifecareCalculationId yet: the draft is refreshed, so the full calculation reconcile runs.
		verify(draftServiceMock).refresh(eq(ERRAND_ID), eq("2026-06"), eq(7), eq(List.of("NATIONAL_NORM")), any(), any(), any());
		verify(warningServiceMock, never()).reconcileRuleWarnings(any(), any(), any(), any(), any());
	}

	@Test
	void prepareWithACalculationSavedInLifecareKeepsTheDraftAndItsWarnings() {
		// Draken has saved the normberäkning in Lifecare and set its id on the errand: that calculation is the truth, so
		// the draft is not refreshed and its warnings are not reconciled. The SSBTEK work carries on as before.
		final var month = YearMonth.of(2026, JUNE);
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM")).withLifecareCalculationId(4242);
		final var questionWarning = new WarningService.WarningInput(WarningService.TYPE_PENDING_BENEFIT, "pending-benefit", "text");
		final var previous = new PreviousHousehold(Set.of("199001011234"), true, 1, null, null, "Riksnorm 2025");
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, "199001011234", month)).thenReturn(previous);
		when(applicationRuleFeederMock.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, errand)).thenReturn(List.of(questionWarning));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, "199001011234", month, "[json]")).thenReturn(new Completeness(false, List.of("Dagersättning")));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("UNDER_REVIEW"));

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[json]")
			.withUnhandledIncomes(List.of("Bostadstillägg (NOT_ON_WHITELIST)")).withChangeWarnings(List.of("Bostadsbidrag: -23%"));

		final var response = service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		// The draft is left alone: no refresh, no feed, no family copy, no late transfer, no duplicate read.
		verifyNoInteractions(draftServiceMock, lateTransferFeederMock);
		verify(calculationFeederMock, never()).incomeRows(any(), any());
		verify(calculationFeederMock, never()).expenseFeed(any(), any(), any(), any(), any());
		verify(calculationFeederMock, never()).personRows(any(), any(), any(), any(), any(), any());
		verify(calculationServiceMock, never()).incomeLines(any(), any(), any(), any());
		verify(calculationServiceMock, never()).selectNormId(any(), any(), any(), any(), any());
		verify(lifecareCaseServiceMock, never()).previousFamily(any(), any(), any());
		// The housing-cost change is frozen with the draft: it concerns the calculation's boendekostnad.
		verify(calculationFeederMock, never()).housingDeltaWarnings(any(), any(), any());

		// Only the SSBTEK income warnings and the draft-independent rule warnings are reconciled.
		verify(warningServiceMock, never()).reconcileCalculationWarnings(any(), any(), any(), any(), any(), any());
		final ArgumentCaptor<List<WarningService.WarningInput>> rules = ArgumentCaptor.captor();
		verify(warningServiceMock).reconcileRuleWarnings(eq(ERRAND_ID), eq(List.of("Bostadstillägg (NOT_ON_WHITELIST)")), eq(List.of("Bostadsbidrag: -23%")),
			eq(List.of("Dagersättning")), rules.capture());
		assertThat(rules.getValue()).containsExactly(questionWarning);
		verify(applicationRuleFeederMock).previousCalculationWarnings(MUNICIPALITY_ID, errand, previous);
		verify(periodRuleFeederMock).periodWarnings(eq(MUNICIPALITY_ID), eq(YearMonth.of(2026, 5)), any(), any());
		verify(missingIncomeFeederMock).missingIncomeWarnings(any());

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
		assertThat(errand.getLastDailyRunAt()).isCloseTo(OffsetDateTime.now(), within(10, SECONDS));
		verify(repositoryMock).save(errand);
	}

	@Test
	void prepareRecordsOkRecommendationAndVantarWhenComplete() {
		final var month = YearMonth.of(2026, JUNE);
		noPreviousCalculation(month);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, "199001011234", month, "[]")).thenReturn(new Completeness(true, List.of()));
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
	}

	@Test
	void prepareDoesNotDuplicateRecommendationOrRewriteUnchangedStatus() {
		final var month = YearMonth.of(2026, JUNE);
		noPreviousCalculation(month);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(calculationServiceMock.completeness(MUNICIPALITY_ID, "199001011234", month, "[]")).thenReturn(new Completeness(true, List.of()));
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
