package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseService;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.Completeness;
import se.sundsvall.caremanagement.lifecare.service.model.EffectiveIncome;
import se.sundsvall.caremanagement.lifecare.service.model.FcIncomeLine;
import se.sundsvall.caremanagement.rpa.service.RpaService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaIncome;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.JUNE;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
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
	private LifecareEbCaseService lifecareEbCaseServiceMock;

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
	private RpaService rpaServiceMock;

	@InjectMocks
	private FinancialAssistanceCalculationService service;

	@Test
	void commitPostsEffectiveRowsAndReturnsId() {
		final var month = YearMonth.of(2026, JUNE);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.of(FaCalculationDraftEntity.create().withErrandId(ERRAND_ID).withNormId(7)));
		when(draftServiceMock.liveIncomes(ERRAND_ID)).thenReturn(List.of(
			FaNormIncomeEntity.create().withTypeId(20).withApplicantProcessAmount(new BigDecimal("1000")).withApplicantCaseworkerAmount(new BigDecimal("1100"))));
		when(draftServiceMock.liveExpenses(ERRAND_ID)).thenReturn(List.of(FaNormExpenseEntity.create().withCostType("RENT").withAppliedAmount(new BigDecimal("9000")).withProcessAmount(new BigDecimal("8000"))));
		when(draftServiceMock.livePersons(ERRAND_ID)).thenReturn(List.of(FaNormPersonEntity.create().withPartyId("p1").withProcessDays(30)));
		when(calculationServiceMock.commitEffective(eq("199001011234"), eq(month), any(CalculationHeader.class), any(), any(), any())).thenReturn(4712);

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID)
			.withUnhandledIncomes(List.of("Något (EJ_PA_LISTAN)")).withChangeWarnings(List.of("Bostadsbidrag: -23%"));

		final var response = service.commitCalculation(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getCalculationId()).isEqualTo(4712);
		assertThat(response.getUnhandledIncomes()).containsExactly("Något (EJ_PA_LISTAN)");
		assertThat(response.getChangeWarnings()).containsExactly("Bostadsbidrag: -23%");

		final ArgumentCaptor<List<EffectiveIncome>> incomeCaptor = ArgumentCaptor.captor();
		verify(calculationServiceMock).commitEffective(eq("199001011234"), eq(month), any(CalculationHeader.class), incomeCaptor.capture(), any(), any());
		assertThat(incomeCaptor.getValue()).singleElement().satisfies(income -> {
			assertThat(income.typeId()).isEqualTo(20);
			assertThat(income.applicantAmount()).isEqualTo(1100.0); // caseworker value wins over the process value
		});
		// commit does not touch the errand status/recommendation — that is prepare's job
		verifyNoInteractions(decisionServiceMock);
	}

	@Test
	void commitYields404WhenNoDraftHeader() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.empty());
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);

		assertThatThrownBy(() -> service.commitCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void prepareUnresolvedPartyIdYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void prepareRequiresClassifiedIncomes() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}

	@Test
	void prepareMissingErrandYields404() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		assertThatThrownBy(() -> service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void prepareRecordsReviewRequiredRecommendationAndKompletteringWhenIncomplete() {
		final var month = YearMonth.of(2026, JUNE);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withNormType(List.of("NATIONAL_NORM"))));
		when(calculationServiceMock.completeness("199001011234", month, "[json]")).thenReturn(new Completeness(false, List.of("Dagersättning")));
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
	}

	@Test
	void prepareRecordsOkRecommendationAndVantarWhenComplete() {
		final var month = YearMonth.of(2026, JUNE);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(calculationServiceMock.completeness("199001011234", month, "[]")).thenReturn(new Completeness(true, List.of()));
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withStatus("SUPPLEMENT_REQUESTED"));
		when(calculationFeederMock.expenseFeed(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any(), any(), any())).thenReturn(new CalculationFeeder.ExpenseFeed(List.of(), List.of()));

		final var request = CalculationRequest.create()
			.withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID).withClassifiedIncomes("[]");

		service.prepareCalculation(MUNICIPALITY_ID, NAMESPACE, request);

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
	}

	@Test
	void prepareDoesNotDuplicateRecommendationOrRewriteUnchangedStatus() {
		final var month = YearMonth.of(2026, JUNE);
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));
		when(calculationServiceMock.completeness("199001011234", month, "[]")).thenReturn(new Completeness(true, List.of()));
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
	void commitFromApplicationFeedsApplicationDataThroughTheSamePipeline() {
		final var month = YearMonth.of(2026, JUNE);
		final var errand = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withIncomes(List.of(
			FaIncome.create().withIncomeType("SALARY").withAmount(new BigDecimal("18500")).withRecipient("APPLICANT"),
			FaIncome.create().withIncomeType("SWISH_DEPOSITS").withAmount(new BigDecimal("300")).withRecipient("CO_APPLICANT")));

		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(errand));
		when(calculationServiceMock.applicationIncomeLines(eq("199001011234"), any()))
			.thenReturn(List.of(new FcIncomeLine(11, "Lön efter skatt", "APPLICANT", new BigDecimal("18500"), null, "Ansökan")));
		when(calculationFeederMock.incomeRows(eq(ERRAND_ID), any())).thenReturn(List.of(
			FaNormIncomeEntity.create().withTypeId(11).withApplicantProcessAmount(new BigDecimal("18500"))));
		when(calculationFeederMock.applicationExpenseRows(eq(ERRAND_ID), any())).thenReturn(
			List.of(FaNormExpenseEntity.create().withCostType("RENT").withAppliedAmount(new BigDecimal("9000")).withProcessAmount(new BigDecimal("8000"))));
		when(calculationFeederMock.personRows(eq(ERRAND_ID), any())).thenReturn(List.of(FaNormPersonEntity.create().withPartyId("p1").withProcessDays(30)));
		when(calculationServiceMock.selectNormId("199001011234", month)).thenReturn(7);
		when(calculationServiceMock.commitEffective(eq("199001011234"), eq(month), any(CalculationHeader.class), any(), any(), any())).thenReturn(5001);

		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);
		final var response = service.commitFromApplication(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getCalculationId()).isEqualTo(5001);

		// The application's declared incomes are mapped to the neutral ApplicationIncome (recipient → role) and handed to
		// the existing income pipeline — not a parallel calculation engine.
		final ArgumentCaptor<List<ApplicationIncome>> incomeCaptor = ArgumentCaptor.captor();
		verify(calculationServiceMock).applicationIncomeLines(eq("199001011234"), incomeCaptor.capture());
		assertThat(incomeCaptor.getValue()).extracting(ApplicationIncome::incomeType, income -> income.role().name())
			.containsExactly(tuple("SALARY", "APPLICANT"), tuple("SWISH_DEPOSITS", "CO_APPLICANT"));

		final ArgumentCaptor<List<EffectiveIncome>> effectiveCaptor = ArgumentCaptor.captor();
		verify(calculationServiceMock).commitEffective(eq("199001011234"), eq(month), any(CalculationHeader.class), effectiveCaptor.capture(), any(), any());
		assertThat(effectiveCaptor.getValue()).singleElement().satisfies(income -> assertThat(income.typeId()).isEqualTo(11));
		verify(rpaServiceMock).enqueue(eq(MUNICIPALITY_ID), eq(ERRAND_ID), any());
	}

	@Test
	void commitFromApplicationYields404WhenErrandMissing() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of("199001011234"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());
		final var request = CalculationRequest.create().withApplicant(APPLICANT_PARTY_ID).withApplicationMonth("2026-06").withErrandId(ERRAND_ID);

		assertThatThrownBy(() -> service.commitFromApplication(MUNICIPALITY_ID, NAMESPACE, request))
			.isInstanceOf(ThrowableProblem.class).hasFieldOrPropertyWithValue("status", NOT_FOUND);
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
