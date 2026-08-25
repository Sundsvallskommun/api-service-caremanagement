package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaCalculationDraftRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormExpenseRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormIncomeRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormPersonRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_CASEWORKER;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ROLE_CHILD;

@ExtendWith(MockitoExtension.class)
class DraftServiceTest {

	private static final String ERRAND_ID = "errand-1";
	private static final String ROW_ID = "row-1";

	@Mock
	private FaCalculationDraftRepository headerRepositoryMock;
	@Mock
	private FaNormIncomeRepository incomeRepositoryMock;
	@Mock
	private FaNormExpenseRepository expenseRepositoryMock;
	@Mock
	private FaNormPersonRepository personRepositoryMock;
	@Mock
	private SectionReconciler sectionReconcilerMock;

	@InjectMocks
	private DraftService service;

	@Test
	void refreshUpsertsHeaderThenDelegatesEachSectionAndAssemblesChanges() {
		when(headerRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());

		final var freshPersons = List.of(FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD));
		final var freshIncomes = List.of(FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(20));
		final var freshExpenses = List.<FaNormExpenseEntity>of();
		when(sectionReconcilerMock.reconcilePersons(ERRAND_ID, freshPersons)).thenReturn(new SectionReconciler.Diff(List.of(), List.of("Gone child")));
		when(sectionReconcilerMock.reconcileIncomes(ERRAND_ID, freshIncomes)).thenReturn(new SectionReconciler.Diff(List.of("Bostadsbidrag"), List.of()));
		when(sectionReconcilerMock.reconcileExpenses(ERRAND_ID, freshExpenses)).thenReturn(new SectionReconciler.Diff(List.of("Rent"), List.of("Old rent")));

		final var changes = service.refresh(ERRAND_ID, "2026-06", 7, List.of("NATIONAL_NORM"), freshPersons, freshIncomes, freshExpenses);

		// each section's Diff is assembled into DraftChanges in section order
		assertThat(changes.addedIncomes()).containsExactly("Bostadsbidrag");
		assertThat(changes.droppedIncomes()).isEmpty();
		assertThat(changes.addedExpenses()).containsExactly("Rent");
		assertThat(changes.droppedExpenses()).containsExactly("Old rent");
		assertThat(changes.addedPersons()).isEmpty();
		assertThat(changes.droppedPersons()).containsExactly("Gone child");

		// the header is upserted with the norm before the sections are reconciled
		final var header = ArgumentCaptor.forClass(FaCalculationDraftEntity.class);
		verify(headerRepositoryMock).save(header.capture());
		assertThat(header.getValue().getNormId()).isEqualTo(7);
		assertThat(header.getValue().getNormType()).isEqualTo(List.of("NATIONAL_NORM"));
	}

	@Test
	void getThrows404WhenNoHeader() {
		when(headerRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No draft calculation for errand");
	}

	@Test
	void getBuildsViewWithEffectiveValuesAndSumsExcludingDeleted() {
		when(headerRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(
			FaCalculationDraftEntity.create().withErrandId(ERRAND_ID).withApplicationMonth("2026-06").withNormId(7).withNormType(List.of("NATIONAL_NORM"))));
		when(incomeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(20).withApplicantProcessAmount(new BigDecimal("1000")).withApplicantCaseworkerAmount(new BigDecimal("1200")),
			FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(21).withApplicantProcessAmount(new BigDecimal("500")).withDeleted(true)));
		when(expenseRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("HOUSING_COST").withAppliedAmount(new BigDecimal("9000")).withProcessAmount(new BigDecimal("8000"))));
		when(personRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD).withProcessDays(15).withCaseworkerDays(20)));

		final var draft = service.get(ERRAND_ID);

		assertThat(draft.getNormId()).isEqualTo(7);
		assertThat(draft.getIncomes()).hasSize(2);
		assertThat(draft.getIncomes().getFirst().getApplicantEffectiveAmount()).isEqualByComparingTo("1200"); // caseworker wins
		assertThat(draft.getExpenses().getFirst().getEffectiveAmount()).isEqualByComparingTo("8000"); // process (no override)
		assertThat(draft.getPersons().getFirst().getEffectiveDays()).isEqualTo(20);
		assertThat(draft.getIncomeSum()).isEqualByComparingTo("1200"); // deleted income (500) excluded
		assertThat(draft.getExpenseSum()).isEqualByComparingTo("8000");
	}

	@Test
	void addIncomeRequiresHeaderThenSavesCaseworkerRow() {
		when(headerRepositoryMock.existsById(ERRAND_ID)).thenReturn(true);
		when(incomeRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var input = new NormIncomeInput().withTypeId(20).withTypeName("Lön").withApplicantCaseworkerAmount(new BigDecimal("3000")).withNote("manuell");
		final var row = service.addIncome(ERRAND_ID, input);

		assertThat(row.getOrigin()).isEqualTo(ORIGIN_CASEWORKER);
		assertThat(row.getPosition()).isZero(); // first row in the section
		assertThat(row.getApplicantCaseworkerAmount()).isEqualByComparingTo("3000");
		assertThat(row.getApplicantEffectiveAmount()).isEqualByComparingTo("3000");
	}

	@Test
	void addIncomeThrows404WhenNoHeader() {
		when(headerRepositoryMock.existsById(ERRAND_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.addIncome(ERRAND_ID, new NormIncomeInput()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No draft calculation for errand");
	}

	@Test
	void patchIncomeSetsOnlyCaseworkerFields() {
		final var existing = FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withApplicantProcessAmount(new BigDecimal("1000"));
		when(incomeRepositoryMock.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(existing));
		when(incomeRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var row = service.patchIncome(ERRAND_ID, ROW_ID, new NormIncomeInput().withApplicantCaseworkerAmount(new BigDecimal("1100")).withNote("ok"));

		assertThat(row.getApplicantProcessAmount()).isEqualByComparingTo("1000"); // untouched
		assertThat(row.getApplicantCaseworkerAmount()).isEqualByComparingTo("1100");
		assertThat(row.getNote()).isEqualTo("ok");
	}

	@Test
	void patchIncomeThrows404WhenRowMissing() {
		when(incomeRepositoryMock.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.patchIncome(ERRAND_ID, ROW_ID, new NormIncomeInput()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No such income row on the errand's draft");
	}

	@Test
	void setIncomeDeletedTogglesFlag() {
		final var existing = FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM);
		when(incomeRepositoryMock.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(existing));
		when(incomeRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.setIncomeDeleted(ERRAND_ID, ROW_ID, true).isDeleted()).isTrue();
	}

	@Test
	void expenseAndPersonEditPathsWork() {
		when(headerRepositoryMock.existsById(ERRAND_ID)).thenReturn(true);
		when(expenseRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(personRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(expenseRepositoryMock.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withProcessAmount(new BigDecimal("8000"))));
		when(personRepositoryMock.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withProcessDays(30)));

		final var added = service.addExpense(ERRAND_ID, new NormExpenseInput().withCostType("HOUSING_COST").withAppliedAmount(new BigDecimal("9000")).withCaseworkerAmount(new BigDecimal("7500")));
		assertThat(added.getEffectiveAmount()).isEqualByComparingTo("7500");
		assertThat(added.getAppliedAmount()).isEqualByComparingTo("9000"); // applied amount honoured on create
		final var patched = service.patchExpense(ERRAND_ID, ROW_ID, new NormExpenseInput().withAppliedAmount(new BigDecimal("9999")).withCaseworkerAmount(new BigDecimal("7000")));
		assertThat(patched.getCaseworkerAmount()).isEqualByComparingTo("7000");
		assertThat(patched.getAppliedAmount()).isEqualByComparingTo("9999"); // applied amount honoured on patch
		assertThat(service.setExpenseDeleted(ERRAND_ID, ROW_ID, true).isDeleted()).isTrue();

		assertThat(service.addPerson(ERRAND_ID, new NormPersonInput().withPartyId("p1").withRole(ROLE_CHILD).withCaseworkerDays(10)).getEffectiveDays()).isEqualTo(10);
		assertThat(service.patchPerson(ERRAND_ID, ROW_ID, new NormPersonInput().withCaseworkerDays(12)).getCaseworkerDays()).isEqualTo(12);
		assertThat(service.setPersonDeleted(ERRAND_ID, ROW_ID, true).isDeleted()).isTrue();
	}

	@Test
	void patchExpenseWithoutAppliedAmountPreservesTheCitizensFigure() {
		when(expenseRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(expenseRepositoryMock.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(
			FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("HOUSING_COST").withAppliedAmount(new BigDecimal("9000"))));

		// A partial patch (no appliedAmount) must not erase the write-once citizen figure — the daily refresh never restores
		// it.
		final var patched = service.patchExpense(ERRAND_ID, ROW_ID, new NormExpenseInput().withCaseworkerAmount(new BigDecimal("7000")));

		assertThat(patched.getAppliedAmount()).isEqualByComparingTo("9000");
		assertThat(patched.getCaseworkerAmount()).isEqualByComparingTo("7000");
	}

	@Test
	void liveReadersFilterOutSoftDeletedRows() {
		when(incomeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormIncomeEntity.create().withTypeId(20), FaNormIncomeEntity.create().withTypeId(21).withDeleted(true)));
		when(expenseRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(FaNormExpenseEntity.create().withCostType("HOUSING_COST")));
		when(personRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormPersonEntity.create().withPartyId("p1").withIncluded(true), FaNormPersonEntity.create().withPartyId("p2").withIncluded(false)));

		assertThat(service.liveIncomes(ERRAND_ID)).hasSize(1);
		assertThat(service.liveExpenses(ERRAND_ID)).hasSize(1);
		assertThat(service.livePersons(ERRAND_ID)).hasSize(1); // p2 excluded (is included = false)
	}

	@Test
	void headerDelegatesToRepository() {
		final var header = FaCalculationDraftEntity.create().withErrandId(ERRAND_ID);
		when(headerRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(header));

		assertThat(service.header(ERRAND_ID)).contains(header);
	}

	@Test
	void patchHeaderUpdatesNormAndHouseholdThenReturnsDraft() {
		final var header = FaCalculationDraftEntity.create().withErrandId(ERRAND_ID);
		when(headerRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(header));
		when(headerRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(incomeRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(expenseRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(personRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		final var draft = service.patchHeader(ERRAND_ID, new NormHeaderInput().withNormId(9).withNormType(List.of("NATIONAL_NORM"))
			.withCalculationFromDate(LocalDate.of(2026, JUNE, 1)).withCalculationToDate(LocalDate.of(2026, JUNE, 30)).withCalculationDate(LocalDate.of(2026, JUNE, 18))
			.withHasCustomHouseholdSize(true).withHouseholdSize(1));

		assertThat(header.getNormId()).isEqualTo(9);
		assertThat(header.getHasCustomHouseholdSize()).isTrue();
		assertThat(header.getHouseholdSize()).isEqualTo(1);
		assertThat(draft.getNormId()).isEqualTo(9);
	}

	@Test
	void patchHeaderThrows404WhenNoHeader() {
		when(headerRepositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.patchHeader(ERRAND_ID, new NormHeaderInput()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: No draft calculation for errand");
	}
}
