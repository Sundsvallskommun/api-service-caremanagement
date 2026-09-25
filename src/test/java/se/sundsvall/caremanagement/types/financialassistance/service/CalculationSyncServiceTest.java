package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationIncomeView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.AppliedSsbtekChange;
import se.sundsvall.caremanagement.types.financialassistance.api.model.AppliedSsbtekChanges;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SsbtekChange;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaCalculationSyncRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationSyncEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationSyncService.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationSyncService.ROLE_CO_APPLICANT;

@ExtendWith(MockitoExtension.class)
class CalculationSyncServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "cb20c51f-fcf3-42c0-b613-de563634a8ec";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final Integer CALCULATION_ID = 4242;
	private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
	private static final LocalDate TO = LocalDate.of(2026, 9, 30);
	private static final OffsetDateTime WRITTEN = OffsetDateTime.parse("2026-09-01T03:00:00+02:00");

	@Mock
	private FaCalculationSyncRepository syncRepositoryMock;

	@Mock
	private FinancialAssistanceRepository financialAssistanceRepositoryMock;

	@Mock
	private DraftService draftServiceMock;

	@Mock
	private HouseholdPartyService householdPartyServiceMock;

	@Mock
	private LifecareCaseHistoryService lifecareCaseHistoryServiceMock;

	@Mock
	private WarningService warningServiceMock;

	@Mock
	private ErrandService errandServiceMock;

	@InjectMocks
	private CalculationSyncService service;

	// ------------------------------------------------------------------------------------------------------------------
	// compare — the rule deciding what may be written without asking
	// ------------------------------------------------------------------------------------------------------------------

	@Test
	void changedAmountTheSystemWroteIsAuto() {
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "12400", "11900"));

		final var changes = CalculationSyncService.compare(rows, calculation(false, income("Lön", "11900", null)));

		assertThat(changes).singleElement().satisfies(change -> {
			assertThat(change.kind()).isEqualTo("CHANGE");
			assertThat(change.mode()).isEqualTo("AUTO");
			assertThat(change.reason()).isNull();
			assertThat(change.incomeType()).isEqualTo("Lön");
			assertThat(change.ssbtekAmount()).isEqualByComparingTo("12400");
			assertThat(change.lifecareAmount()).isEqualByComparingTo("11900");
		});
	}

	@Test
	void changedAmountACaseworkerEditedIsConfirm() {
		// The system wrote 11 900, the calculation now holds 12 000: someone changed it in Lifecare.
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "12400", "11900"));

		final var changes = CalculationSyncService.compare(rows, calculation(false, income("Lön", "12000", null)));

		assertThat(changes).extracting(SsbtekChange::mode, SsbtekChange::reason).containsExactly(tuple("CONFIRM", "EDITED"));
	}

	@Test
	void changedAmountTheCaseworkerOverrodeBeforeTheProposalIsConfirm() {
		// Written with no amount: the proposal carried the caseworker's figure, not the system's.
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "12400", null));

		final var changes = CalculationSyncService.compare(rows, calculation(false, income("Lön", "12000", null)));

		assertThat(changes).extracting(SsbtekChange::mode, SsbtekChange::reason).containsExactly(tuple("CONFIRM", "EDITED"));
	}

	@Test
	void changedAmountWithoutABaselineIsConfirm() {
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "12400", null).withSystemWrittenAt(null));

		final var changes = CalculationSyncService.compare(rows, calculation(false, income("Lön", "11900", null)));

		assertThat(changes).extracting(SsbtekChange::mode, SsbtekChange::reason).containsExactly(tuple("CONFIRM", "NO_BASELINE"));
	}

	@Test
	void changedAmountOnATypeWithSeveralRowsIsConfirm() {
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "12400", "11900"));

		final var changes = CalculationSyncService.compare(rows, calculation(false, income("Lön", "6000", null), income("lön ", "5900", null)));

		assertThat(changes).singleElement().satisfies(change -> {
			assertThat(change.lifecareAmount()).isEqualByComparingTo("11900");
			assertThat(change.mode()).isEqualTo("CONFIRM");
			assertThat(change.reason()).isEqualTo("MULTIPLE_ROWS");
		});
	}

	@Test
	void newIncomeIsAutoWhenTheCalculationHasABaseline() {
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "11900", "11900"), sync("bostadsbidrag", ROLE_APPLICANT, "1850", null).withSystemWrittenAt(null));

		final var changes = CalculationSyncService.compare(rows, calculation(false, income("Lön", "11900", null)));

		assertThat(changes).singleElement().satisfies(change -> {
			assertThat(change.kind()).isEqualTo("ADD");
			assertThat(change.mode()).isEqualTo("AUTO");
			assertThat(change.lifecareAmount()).isNull();
			assertThat(change.ssbtekAmount()).isEqualByComparingTo("1850");
		});
	}

	@Test
	void newIncomeWithoutAnyBaselineIsConfirm() {
		final var rows = List.of(sync("bostadsbidrag", ROLE_APPLICANT, "1850", null).withSystemWrittenAt(null));

		final var changes = CalculationSyncService.compare(rows, calculation(false));

		assertThat(changes).extracting(SsbtekChange::mode, SsbtekChange::reason).containsExactly(tuple("CONFIRM", "NO_BASELINE"));
	}

	@Test
	void incomeTheSystemWroteAndACaseworkerRemovedIsConfirm() {
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "11900", "11900"));

		final var changes = CalculationSyncService.compare(rows, calculation(false));

		assertThat(changes).extracting(SsbtekChange::kind, SsbtekChange::mode, SsbtekChange::reason).containsExactly(tuple("ADD", "CONFIRM", "REMOVED"));
	}

	@Test
	void incomeSsbtekNoLongerReportsIsAlwaysConfirm() {
		final var rows = List.of(sync("lön", ROLE_APPLICANT, null, "11900"));

		final var changes = CalculationSyncService.compare(rows, calculation(false, income("Lön", "11900", null)));

		assertThat(changes).extracting(SsbtekChange::kind, SsbtekChange::mode, SsbtekChange::reason, SsbtekChange::ssbtekAmount)
			.containsExactly(tuple("GONE", "CONFIRM", "GONE_FROM_SSBTEK", null));
	}

	@Test
	void incomeTheCaseworkerAddedThemselvesIsLeftAlone() {
		// Neither SSBTEK nor the system ever had it: a caseworker's own income is none of the sync's business.
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "11900", "11900"));

		final var changes = CalculationSyncService.compare(rows, calculation(false, income("Lön", "11900", null), income("Underhållsstöd", "1573", null)));

		assertThat(changes).isEmpty();
	}

	@Test
	void gonePreviouslyWrittenAwayIsNotFlaggedAgain() {
		// The BFF took the income out on an earlier run (written, no amount); SSBTEK still lacks it and so does the
		// calculation.
		final var rows = List.of(sync("lön", ROLE_APPLICANT, null, null));

		assertThat(CalculationSyncService.compare(rows, calculation(false))).isEmpty();
	}

	@Test
	void everythingOnAFinalCalculationIsConfirm() {
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "12400", "11900"), sync("bostadsbidrag", ROLE_APPLICANT, "1850", null).withSystemWrittenAt(null));

		final var changes = CalculationSyncService.compare(rows, calculation(true, income("Lön", "11900", null)));

		assertThat(changes).extracting(SsbtekChange::kind, SsbtekChange::mode, SsbtekChange::reason)
			.containsExactly(tuple("ADD", "CONFIRM", "FINAL"), tuple("CHANGE", "CONFIRM", "FINAL"));
	}

	@Test
	void sidesAreComparedSeparately() {
		// The applicant's side matches; the co-applicant's has moved.
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "11900", "11900"), sync("lön", ROLE_CO_APPLICANT, "9000", "8500"));

		final var changes = CalculationSyncService.compare(rows, calculation(false, income("Lön", "11900", "8500")));

		assertThat(changes).extracting(SsbtekChange::role, SsbtekChange::kind, SsbtekChange::mode).containsExactly(tuple("CO_APPLICANT", "CHANGE", "AUTO"));
	}

	@Test
	void amountsMatchingToTheOreAreNoChange() {
		final var rows = List.of(sync("lön", ROLE_APPLICANT, "11900.004", "11900"));

		assertThat(CalculationSyncService.compare(rows, calculation(false, income("Lön", "11900.00", "0")))).isEmpty();
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Recording
	// ------------------------------------------------------------------------------------------------------------------

	@Test
	void seedFromProposalRecordsWhatTheSystemWrote() {
		final var draft = List.of(
			FaNormIncomeEntity.create().withTypeId(20).withTypeName("Lön").withApplicantProcessAmount(new BigDecimal("11900")),
			// The caseworker's figure went into the proposal, not the system's.
			FaNormIncomeEntity.create().withTypeId(21).withTypeName("Bostadsbidrag").withApplicantProcessAmount(new BigDecimal("1850"))
				.withApplicantCaseworkerAmount(new BigDecimal("1900")),
			// Withheld on purpose.
			FaNormIncomeEntity.create().withTypeId(22).withTypeName("Barnbidrag").withApplicantProcessAmount(new BigDecimal("1250")).withDeleted(true),
			// Nothing on either side, and a row without a type name, are not recorded.
			FaNormIncomeEntity.create().withTypeId(23).withTypeName("Studiemedel"),
			FaNormIncomeEntity.create().withTypeId(24).withApplicantProcessAmount(new BigDecimal("100")));

		service.seedFromProposal(ERRAND_ID, draft);

		final var saved = savedRows();
		assertThat(saved).extracting(FaCalculationSyncEntity::getIncomeTypeKey, FaCalculationSyncEntity::getRole, FaCalculationSyncEntity::getIncomeTypeId)
			.containsExactlyInAnyOrder(tuple("lön", ROLE_APPLICANT, 20), tuple("bostadsbidrag", ROLE_APPLICANT, 21), tuple("barnbidrag", ROLE_APPLICANT, 22));
		assertThat(saved).allSatisfy(row -> {
			assertThat(row.getErrandId()).isEqualTo(ERRAND_ID);
			assertThat(row.getSystemWrittenAt()).isNotNull();
			assertThat(row.getSsbtekReadAt()).isNotNull();
		});
		assertThat(row(saved, "lön").getSystemWrittenAmount()).isEqualByComparingTo("11900");
		assertThat(row(saved, "bostadsbidrag").getSystemWrittenAmount()).isNull();
		assertThat(row(saved, "bostadsbidrag").getSsbtekAmount()).isEqualByComparingTo("1850");
		assertThat(row(saved, "barnbidrag").getSystemWrittenAmount()).isNull();
	}

	@Test
	void seedFromProposalTreatsTwoRowsOfOneTypeAsNotTheSystemsOwn() {
		final var draft = List.of(
			FaNormIncomeEntity.create().withTypeName("Lön").withApplicantProcessAmount(new BigDecimal("6000")),
			FaNormIncomeEntity.create().withTypeName("Lön").withApplicantProcessAmount(new BigDecimal("5900")));

		service.seedFromProposal(ERRAND_ID, draft);

		assertThat(savedRows()).singleElement().satisfies(row -> {
			assertThat(row.getSsbtekAmount()).isEqualByComparingTo("11900");
			assertThat(row.getSystemWrittenAmount()).isNull();
		});
	}

	@Test
	void recordSsbtekUpdatesAddsAndClearsAmounts() {
		final var lon = sync("lön", ROLE_APPLICANT, "11900", "11900");
		final var barnbidrag = sync("barnbidrag", ROLE_APPLICANT, "1250", "1250");
		when(syncRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(lon, barnbidrag));

		service.recordSsbtek(ERRAND_ID, List.of(
			FaNormIncomeEntity.create().withTypeId(20).withTypeName("Lön").withApplicantProcessAmount(new BigDecimal("12400")),
			FaNormIncomeEntity.create().withTypeId(21).withTypeName("Bostadsbidrag").withCoapplicantProcessAmount(new BigDecimal("1850")),
			// Only process amounts count: what a caseworker typed is not what SSBTEK said.
			FaNormIncomeEntity.create().withTypeId(25).withTypeName("Swish").withApplicantCaseworkerAmount(new BigDecimal("500"))));

		final var saved = savedRows();
		assertThat(saved).extracting(FaCalculationSyncEntity::getIncomeTypeKey, FaCalculationSyncEntity::getRole)
			.containsExactlyInAnyOrder(tuple("lön", ROLE_APPLICANT), tuple("bostadsbidrag", ROLE_CO_APPLICANT), tuple("barnbidrag", ROLE_APPLICANT));
		assertThat(row(saved, "lön").getSsbtekAmount()).isEqualByComparingTo("12400");
		assertThat(row(saved, "lön").getSystemWrittenAmount()).isEqualByComparingTo("11900");
		assertThat(row(saved, "bostadsbidrag").getIncomeTypeId()).isEqualTo(21);
		assertThat(row(saved, "bostadsbidrag").getSystemWrittenAt()).isNull();
		// No longer reported: the amount is cleared, and the read time says SSBTEK was asked.
		assertThat(row(saved, "barnbidrag").getSsbtekAmount()).isNull();
		assertThat(row(saved, "barnbidrag").getSsbtekReadAt()).isAfter(WRITTEN);
	}

	@Test
	void recordSsbtekSumsTwoLinesOfOneType() {
		when(syncRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		service.recordSsbtek(ERRAND_ID, List.of(
			FaNormIncomeEntity.create().withTypeName("Lön").withApplicantProcessAmount(new BigDecimal("6000")),
			FaNormIncomeEntity.create().withTypeName("lön").withApplicantProcessAmount(new BigDecimal("5900"))));

		assertThat(savedRows()).singleElement().satisfies(row -> assertThat(row.getSsbtekAmount()).isEqualByComparingTo("11900"));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Warnings
	// ------------------------------------------------------------------------------------------------------------------

	@Test
	void reconcileWarningsRaisesOneWarningPerChange() {
		when(lifecareCaseHistoryServiceMock.listCalculations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, FROM, TO))
			.thenReturn(List.of(calculation(false, income("Lön", "11900", null))));
		when(syncRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(sync("lön", ROLE_APPLICANT, "12400", "11900")));

		service.reconcileWarnings(MUNICIPALITY_ID, ERRAND_ID, APPLICANT_PARTY_ID, CALCULATION_ID, FROM, TO);

		final ArgumentCaptor<List<WarningService.WarningInput>> captor = ArgumentCaptor.captor();
		verify(warningServiceMock).reconcileByTypes(eq(ERRAND_ID), eq(Set.of(WarningService.TYPE_SSBTEK_CALCULATION_DIFF)), captor.capture());
		assertThat(captor.getValue()).singleElement().satisfies(warning -> {
			assertThat(warning.type()).isEqualTo(WarningService.TYPE_SSBTEK_CALCULATION_DIFF);
			assertThat(warning.sourceKey()).isEqualTo("ssbtek-sync:APPLICANT:lön:12400");
			assertThat(warning.message()).isEqualTo("SSBTEK har ändrats: Lön (sökande) är 12400 kr, normberäkningen i Lifecare har 11900 kr");
		});
	}

	@Test
	void reconcileWarningsLeavesTheWarningsAloneWhenLifecareCannotBeRead() {
		when(lifecareCaseHistoryServiceMock.listCalculations(any(), any(), any(), any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "down"));

		service.reconcileWarnings(MUNICIPALITY_ID, ERRAND_ID, APPLICANT_PARTY_ID, CALCULATION_ID, FROM, TO);

		verifyNoInteractions(warningServiceMock);
	}

	@Test
	void reconcileWarningsLeavesTheWarningsAloneWhenTheCalculationIsNotFound() {
		when(lifecareCaseHistoryServiceMock.listCalculations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, FROM, TO)).thenReturn(List.of(calculation(9999, false)));

		service.reconcileWarnings(MUNICIPALITY_ID, ERRAND_ID, APPLICANT_PARTY_ID, CALCULATION_ID, FROM, TO);

		verifyNoInteractions(warningServiceMock);
	}

	@Test
	void warningTexts() {
		final var add = CalculationSyncService.toWarning(new SsbtekChange("ADD", "AUTO", null, ROLE_CO_APPLICANT, 21, "Bostadsbidrag", new BigDecimal("1850.00"), null));
		final var gone = CalculationSyncService.toWarning(new SsbtekChange("GONE", "CONFIRM", "GONE_FROM_SSBTEK", ROLE_APPLICANT, 22, "Barnbidrag", null, new BigDecimal("1250.00")));

		assertThat(add.message()).isEqualTo("Ny inkomst i SSBTEK som saknas i normberäkningen i Lifecare: Bostadsbidrag (medsökande) 1850 kr");
		assertThat(add.sourceKey()).isEqualTo("ssbtek-sync:CO_APPLICANT:bostadsbidrag:1850");
		assertThat(gone.message()).isEqualTo("Barnbidrag (sökande) finns i normberäkningen i Lifecare (1250 kr) men rapporteras inte längre i SSBTEK");
		assertThat(gone.sourceKey()).isEqualTo("ssbtek-sync:APPLICANT:barnbidrag:-");
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Draken path
	// ------------------------------------------------------------------------------------------------------------------

	@Test
	void changesReadsTheCalculationLive() {
		linked();
		final var lon = sync("lön", ROLE_APPLICANT, "12400", "11900");
		when(syncRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(lon));
		when(lifecareCaseHistoryServiceMock.listCalculations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, FROM, TO))
			.thenReturn(List.of(calculation(false, income("Lön", "11900", null))));

		final var result = service.changes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result.calculationId()).isEqualTo(CALCULATION_ID);
		assertThat(result.isFinal()).isFalse();
		assertThat(result.comparedAt()).isNotNull();
		assertThat(result.ssbtekReadAt()).isEqualTo(lon.getSsbtekReadAt());
		assertThat(result.changes()).extracting(SsbtekChange::kind, SsbtekChange::mode).containsExactly(tuple("CHANGE", "AUTO"));
		// A read: nothing is written and no warning is touched.
		verify(syncRepositoryMock, never()).saveAll(any());
		verifyNoInteractions(warningServiceMock);
	}

	@Test
	void changesIs502WhenLifecareCannotBeRead() {
		linked();
		when(lifecareCaseHistoryServiceMock.listCalculations(any(), any(), any(), any())).thenThrow(new IllegalStateException("down"));

		assertThatThrownBy(() -> service.changes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);
	}

	@Test
	void changesIs404WhenTheCalculationIsNotFound() {
		linked();
		when(lifecareCaseHistoryServiceMock.listCalculations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, FROM, TO)).thenReturn(List.of());

		assertThatThrownBy(() -> service.changes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void changesIs404WithoutALinkedCalculation() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)));

		assertThatThrownBy(() -> service.changes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verifyNoInteractions(lifecareCaseHistoryServiceMock);
	}

	@Test
	void changesIs404AfterTheDraftIsPurged() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withLifecareCalculationId(CALCULATION_ID)));
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.changes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void changesIs404WithoutAnApplicant() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withLifecareCalculationId(CALCULATION_ID)));
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.of(FaCalculationDraftEntity.create().withCalculationFromDate(FROM).withCalculationToDate(TO)));
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new HouseholdPartyService.Household(Optional.empty(), false, Optional.empty(), Optional.empty()));

		assertThatThrownBy(() -> service.changes(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void appliedRecordsWhatTheBffWroteAndUpdatesTheWarnings() {
		linked();
		final var lon = sync("lön", ROLE_APPLICANT, "12400", "11900");
		final var barnbidrag = sync("barnbidrag", ROLE_APPLICANT, null, "1250");
		when(syncRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(lon, barnbidrag));
		when(lifecareCaseHistoryServiceMock.listCalculations(MUNICIPALITY_ID, APPLICANT_PARTY_ID, FROM, TO))
			.thenReturn(List.of(calculation(false, income("Lön", "12400", null))));

		service.applied(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new AppliedSsbtekChanges(CALCULATION_ID, List.of(
			new AppliedSsbtekChange(ROLE_APPLICANT, "Lön", new BigDecimal("12400")),
			new AppliedSsbtekChange(ROLE_APPLICANT, "Barnbidrag", null),
			new AppliedSsbtekChange(ROLE_APPLICANT, "Bostadsbidrag", new BigDecimal("1850")))));

		final var saved = savedRows();
		assertThat(row(saved, "lön").getSystemWrittenAmount()).isEqualByComparingTo("12400");
		assertThat(row(saved, "lön").getSystemWrittenAt()).isAfter(WRITTEN);
		// Taken out: written, with no amount.
		assertThat(row(saved, "barnbidrag").getSystemWrittenAmount()).isNull();
		assertThat(row(saved, "barnbidrag").getSystemWrittenAt()).isAfter(WRITTEN);
		// An income careM had no row for yet gets one.
		assertThat(row(saved, "bostadsbidrag").getIncomeTypeName()).isEqualTo("Bostadsbidrag");
		assertThat(row(saved, "bostadsbidrag").getSystemWrittenAmount()).isEqualByComparingTo("1850");
		verify(warningServiceMock).reconcileByTypes(eq(ERRAND_ID), eq(Set.of(WarningService.TYPE_SSBTEK_CALCULATION_DIFF)), any());
	}

	@Test
	void appliedIs409ForAnotherCalculation() {
		linked();
		final var request = new AppliedSsbtekChanges(5555, List.of(new AppliedSsbtekChange(ROLE_APPLICANT, "Lön", BigDecimal.ONE)));

		assertThatThrownBy(() -> service.applied(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request))
			.isInstanceOf(Problem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);
		verify(syncRepositoryMock, never()).saveAll(any());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------------------------------------------------------

	private void linked() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(Errand.create().withId(ERRAND_ID));
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withLifecareCalculationId(CALCULATION_ID)));
		when(draftServiceMock.header(ERRAND_ID)).thenReturn(Optional.of(FaCalculationDraftEntity.create().withCalculationFromDate(FROM).withCalculationToDate(TO)));
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(new HouseholdPartyService.Household(Optional.of(APPLICANT_PARTY_ID), false, Optional.empty(), Optional.empty()));
	}

	/**
	 * A sync row; {@code written} null means the system wrote the income with no amount (it chose not to, or took it out).
	 */
	private static FaCalculationSyncEntity sync(final String typeKey, final String role, final String ssbtek, final String written) {
		return FaCalculationSyncEntity.create()
			.withErrandId(ERRAND_ID)
			.withIncomeTypeKey(typeKey)
			.withIncomeTypeName(Character.toUpperCase(typeKey.charAt(0)) + typeKey.substring(1))
			.withRole(role)
			.withSsbtekAmount(amount(ssbtek))
			.withSsbtekReadAt(WRITTEN.plusDays(1))
			.withSystemWrittenAmount(amount(written))
			.withSystemWrittenAt(WRITTEN);
	}

	private static CalculationIncomeView income(final String type, final String applicant, final String coApplicant) {
		return new CalculationIncomeView(type, amount(applicant), null, amount(coApplicant), null);
	}

	private static CalculationView calculation(final boolean isFinal, final CalculationIncomeView... incomes) {
		return new CalculationView(CALCULATION_ID, "Riksnorm 2026", "2026-09-01", "2026-09-30", null, null, null, null, null, null, null, null, isFinal,
			List.of(), List.of(incomes), List.of(), List.of());
	}

	private static CalculationView calculation(final Integer id, final boolean isFinal) {
		return new CalculationView(id, null, null, null, null, null, null, null, null, null, null, null, isFinal, List.of(), List.of(), List.of(), List.of());
	}

	private static BigDecimal amount(final String value) {
		return Optional.ofNullable(value).map(BigDecimal::new).orElse(null);
	}

	private List<FaCalculationSyncEntity> savedRows() {
		final ArgumentCaptor<Iterable<FaCalculationSyncEntity>> captor = ArgumentCaptor.captor();
		verify(syncRepositoryMock).saveAll(captor.capture());
		final var rows = new ArrayList<FaCalculationSyncEntity>();
		captor.getValue().forEach(rows::add);
		return rows;
	}

	private static FaCalculationSyncEntity row(final List<FaCalculationSyncEntity> rows, final String typeKey) {
		return rows.stream().filter(row -> typeKey.equals(row.getIncomeTypeKey())).findFirst().orElseThrow();
	}
}
