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
	private FaCalculationDraftRepository headerRepository;
	@Mock
	private FaNormIncomeRepository incomeRepository;
	@Mock
	private FaNormExpenseRepository expenseRepository;
	@Mock
	private FaNormPersonRepository personRepository;

	@InjectMocks
	private DraftService service;

	@Test
	void effectiveValueHelpersPreferCaseworker() {
		assertThat(DraftService.effectiveAmount(new BigDecimal("5"), new BigDecimal("9"))).isEqualByComparingTo("5");
		assertThat(DraftService.effectiveAmount(null, new BigDecimal("9"))).isEqualByComparingTo("9");
		assertThat(DraftService.effectiveDays(12, 30)).isEqualTo(12);
		assertThat(DraftService.effectiveDays(null, 30)).isEqualTo(30);
	}

	@Test
	void refreshUpsertsHeaderAndReportsAddedRows() {
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.empty());
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		final var income = FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(20).withTypeName("Bostadsbidrag");

		final var changes = service.refresh(ERRAND_ID, "2026-06", 7, List.of("NATIONAL_NORM"), List.of(), List.of(income), List.of());

		assertThat(changes.addedIncomes()).containsExactly("Bostadsbidrag");
		final var header = ArgumentCaptor.forClass(FaCalculationDraftEntity.class);
		verifySaved(header);
		assertThat(header.getValue().getNormId()).isEqualTo(7);
		assertThat(header.getValue().getNormType()).isEqualTo(List.of("NATIONAL_NORM"));
	}

	private void verifySaved(final ArgumentCaptor<FaCalculationDraftEntity> captor) {
		verify(headerRepository).save(captor.capture());
	}

	@Test
	void refreshDoesNotCollapseChildrenWithoutPartyId() {
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.empty());
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		// Three children, none carrying a partyId. Keyed on partyId alone they all collapse to "null|CHILD" and only the
		// last is counted; keyed with the name fallback they stay three distinct rows.
		final var child1 = FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD).withName("Alva Alvsson").withProcessDays(30).withIncluded(true);
		final var child2 = FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD).withName("Bo Bosson").withProcessDays(30).withIncluded(true);
		final var child3 = FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD).withName("Cecilia Cederlund").withProcessDays(30).withIncluded(true);

		final var changes = service.refresh(ERRAND_ID, "2026-06", 7, List.of("NATIONAL_NORM"), List.of(child1, child2, child3), List.of(), List.of());

		assertThat(changes.addedPersons()).hasSize(3);
	}

	@Test
	void refreshKeepsExistingRowPositionAndAppendsNewRow() {
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.empty());
		final var existing = FaNormIncomeEntity.create().withId("inc-0").withErrandId(ERRAND_ID).withOrigin(ORIGIN_SYSTEM).withPosition(0).withTypeId(20)
			.withTypeName("Bostadsbidrag");
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(existing));
		when(incomeRepository.nextPositionForErrand(ERRAND_ID)).thenReturn(1);
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		final var refreshedFresh = FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(20).withTypeName("Bostadsbidrag");
		final var newFresh = FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(99).withTypeName("Underhållsstöd");

		service.refresh(ERRAND_ID, "2026-06", 7, List.of("NATIONAL_NORM"), List.of(), List.of(refreshedFresh, newFresh), List.of());

		assertThat(existing.getPosition()).isZero();  // a refreshed row keeps the position it already had
		assertThat(newFresh.getPosition()).isEqualTo(1);  // a genuinely new row is appended at the next free position
	}

	@Test
	void refreshPreservesCaseworkerEditedAppliedAmountOnSystemExpenseRow() {
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.empty());
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		// a system expense row whose appliedAmount a caseworker has corrected (9999) since the last loop
		final var existing = FaNormExpenseEntity.create().withId("exp-0").withErrandId(ERRAND_ID).withOrigin(ORIGIN_SYSTEM).withPosition(0)
			.withCostType("HOUSING_COST").withAppliedAmount(new BigDecimal("9999")).withProcessAmount(new BigDecimal("8000"));
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(existing));

		// the freshly computed row for the same expense carries the original applied amount (9000) and a new process cap
		final var fresh = FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM)
			.withCostType("HOUSING_COST").withAppliedAmount(new BigDecimal("9000")).withProcessAmount(new BigDecimal("8500"));

		service.refresh(ERRAND_ID, "2026-06", 7, List.of("NATIONAL_NORM"), List.of(), List.of(), List.of(fresh));

		assertThat(existing.getAppliedAmount()).isEqualByComparingTo("9999"); // caseworker edit survives the daily refresh
		assertThat(existing.getProcessAmount()).isEqualByComparingTo("8500"); // process column is still refreshed
	}

	@Test
	void getThrows404WhenNoHeader() {
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void getBuildsViewWithEffectiveValuesAndSumsExcludingDeleted() {
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.of(
			FaCalculationDraftEntity.create().withErrandId(ERRAND_ID).withApplicationMonth("2026-06").withNormId(7).withNormType(List.of("NATIONAL_NORM"))));
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(20).withApplicantProcessAmount(new BigDecimal("1000")).withApplicantCaseworkerAmount(new BigDecimal("1200")),
			FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(21).withApplicantProcessAmount(new BigDecimal("500")).withDeleted(true)));
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("HOUSING_COST").withAppliedAmount(new BigDecimal("9000")).withProcessAmount(new BigDecimal("8000"))));
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(
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
		when(headerRepository.existsById(ERRAND_ID)).thenReturn(true);
		when(incomeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var input = new NormIncomeInput().withTypeId(20).withTypeName("Lön").withApplicantCaseworkerAmount(new BigDecimal("3000")).withNote("manuell");
		final var row = service.addIncome(ERRAND_ID, input);

		assertThat(row.getOrigin()).isEqualTo(ORIGIN_CASEWORKER);
		assertThat(row.getPosition()).isZero(); // first row in the section
		assertThat(row.getApplicantCaseworkerAmount()).isEqualByComparingTo("3000");
		assertThat(row.getApplicantEffectiveAmount()).isEqualByComparingTo("3000");
	}

	@Test
	void addIncomeThrows404WhenNoHeader() {
		when(headerRepository.existsById(ERRAND_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.addIncome(ERRAND_ID, new NormIncomeInput()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void patchIncomeSetsOnlyCaseworkerFields() {
		final var existing = FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withApplicantProcessAmount(new BigDecimal("1000"));
		when(incomeRepository.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(existing));
		when(incomeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var row = service.patchIncome(ERRAND_ID, ROW_ID, new NormIncomeInput().withApplicantCaseworkerAmount(new BigDecimal("1100")).withNote("ok"));

		assertThat(row.getApplicantProcessAmount()).isEqualByComparingTo("1000"); // untouched
		assertThat(row.getApplicantCaseworkerAmount()).isEqualByComparingTo("1100");
		assertThat(row.getNote()).isEqualTo("ok");
	}

	@Test
	void patchIncomeThrows404WhenRowMissing() {
		when(incomeRepository.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.patchIncome(ERRAND_ID, ROW_ID, new NormIncomeInput()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void setIncomeDeletedTogglesFlag() {
		final var existing = FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM);
		when(incomeRepository.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(existing));
		when(incomeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.setIncomeDeleted(ERRAND_ID, ROW_ID, true).isDeleted()).isTrue();
	}

	@Test
	void expenseAndPersonEditPathsWork() {
		when(headerRepository.existsById(ERRAND_ID)).thenReturn(true);
		when(expenseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(personRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(expenseRepository.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withProcessAmount(new BigDecimal("8000"))));
		when(personRepository.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withProcessDays(30)));

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
		when(expenseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(expenseRepository.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(
			FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("HOUSING_COST").withAppliedAmount(new BigDecimal("9000"))));

		// A partial patch (no appliedAmount) must not erase the write-once citizen figure — the daily refresh never restores
		// it.
		final var patched = service.patchExpense(ERRAND_ID, ROW_ID, new NormExpenseInput().withCaseworkerAmount(new BigDecimal("7000")));

		assertThat(patched.getAppliedAmount()).isEqualByComparingTo("9000");
		assertThat(patched.getCaseworkerAmount()).isEqualByComparingTo("7000");
	}

	@Test
	void liveReadersFilterOutSoftDeletedRows() {
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormIncomeEntity.create().withTypeId(20), FaNormIncomeEntity.create().withTypeId(21).withDeleted(true)));
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(FaNormExpenseEntity.create().withCostType("HOUSING_COST")));
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormPersonEntity.create().withPartyId("p1").withIncluded(true), FaNormPersonEntity.create().withPartyId("p2").withIncluded(false)));

		assertThat(service.liveIncomes(ERRAND_ID)).hasSize(1);
		assertThat(service.liveExpenses(ERRAND_ID)).hasSize(1);
		assertThat(service.livePersons(ERRAND_ID)).hasSize(1); // p2 excluded (is included = false)
	}

	@Test
	void headerDelegatesToRepository() {
		final var header = FaCalculationDraftEntity.create().withErrandId(ERRAND_ID);
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.of(header));

		assertThat(service.header(ERRAND_ID)).contains(header);
	}

	@Test
	void patchHeaderUpdatesNormAndHouseholdThenReturnsDraft() {
		final var header = FaCalculationDraftEntity.create().withErrandId(ERRAND_ID);
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.of(header));
		when(headerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());

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
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.patchHeader(ERRAND_ID, new NormHeaderInput()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}
}
