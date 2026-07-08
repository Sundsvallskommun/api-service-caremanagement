package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormExpenseRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormIncomeRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormPersonRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_CASEWORKER;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ROLE_CHILD;

/**
 * The calculation merge invariant — the heart of the process-vs-caseworker ownership design. Exercised through
 * {@link SectionReconciler#reconcileIncomes} (the section-agnostic merge), with one person-specific case for the
 * partyId-less children key fallback.
 */
@ExtendWith(MockitoExtension.class)
class SectionReconcilerTest {

	private static final String ERRAND_ID = "errand-1";

	@Mock
	private FaNormPersonRepository personRepository;
	@Mock
	private FaNormIncomeRepository incomeRepository;
	@Mock
	private FaNormExpenseRepository expenseRepository;

	@InjectMocks
	private SectionReconciler sectionReconciler;

	@Test
	void freshSystemRowWithNoMatchIsInsertedAndReportedAsAdded() {
		final var fresh = systemRow(20, "Bostadsbidrag", "1850");
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		final var diff = sectionReconciler.reconcileIncomes(ERRAND_ID, List.of(fresh));

		assertThat(diff.added()).containsExactly("Bostadsbidrag");
		assertThat(diff.dropped()).isEmpty();
		verify(incomeRepository).save(fresh);
	}

	@Test
	void matchedSystemRowRefreshesTheProcessColumnAndPreservesTheCaseworkerValue() {
		final var existing = systemRow(20, "Bostadsbidrag", "1850").withApplicantCaseworkerAmount(new BigDecimal("1900")).withNote("ok");
		final var fresh = systemRow(20, "Bostadsbidrag", "2000");
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(new ArrayList<>(List.of(existing)));

		final var diff = sectionReconciler.reconcileIncomes(ERRAND_ID, List.of(fresh));

		assertThat(diff.added()).isEmpty();
		assertThat(diff.dropped()).isEmpty();
		assertThat(existing.getApplicantProcessAmount()).isEqualByComparingTo("2000"); // process refreshed
		assertThat(existing.getApplicantCaseworkerAmount()).isEqualByComparingTo("1900"); // caseworker value untouched
		assertThat(existing.getNote()).isEqualTo("ok"); // note untouched
		verify(incomeRepository).save(existing);
	}

	@Test
	void softDeletedSystemRowStaysDeletedAndIsNotResurrected() {
		final var existing = systemRow(20, "Bostadsbidrag", "1850").withDeleted(true);
		final var fresh = systemRow(20, "Bostadsbidrag", "2000");
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(new ArrayList<>(List.of(existing)));

		final var diff = sectionReconciler.reconcileIncomes(ERRAND_ID, List.of(fresh));

		assertThat(diff.added()).isEmpty(); // not re-added
		assertThat(existing.isDeleted()).isTrue(); // still deleted — never resurrected
		assertThat(existing.getApplicantProcessAmount()).isEqualByComparingTo("2000"); // process still refreshed in place
		verify(incomeRepository).save(existing);
	}

	@Test
	void caseworkerAddedRowIsNeverMatchedOrRefreshedByTheProcess() {
		final var caseworkerRow = systemRow(20, "Bostadsbidrag", null).withOrigin(ORIGIN_CASEWORKER).withApplicantCaseworkerAmount(new BigDecimal("500"));
		final var fresh = systemRow(20, "Bostadsbidrag", "2000");
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(new ArrayList<>(List.of(caseworkerRow)));

		final var diff = sectionReconciler.reconcileIncomes(ERRAND_ID, List.of(fresh));

		assertThat(diff.added()).containsExactly("Bostadsbidrag"); // the fresh row is inserted as new
		assertThat(caseworkerRow.getApplicantProcessAmount()).isNull(); // caseworker row untouched
		verify(incomeRepository).save(fresh); // only the new system row is persisted
		verify(incomeRepository, never()).save(caseworkerRow);
	}

	@Test
	void systemRowThatDisappearsFromTheFreshSetIsKeptAndReportedAsDropped() {
		final var existing = systemRow(20, "Bostadsbidrag", "1850");
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(new ArrayList<>(List.of(existing)));

		final var diff = sectionReconciler.reconcileIncomes(ERRAND_ID, List.of());

		assertThat(diff.dropped()).containsExactly("Bostadsbidrag");
		assertThat(diff.added()).isEmpty();
		verify(incomeRepository, never()).save(existing); // not auto-deleted, not re-saved
	}

	@Test
	void reconcilePersonsKeepsChildrenWithoutPartyIdDistinct() {
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		// Three children, none carrying a partyId. Keyed on partyId alone they all collapse to "null|CHILD" and only the
		// last is counted; keyed with the name fallback they stay three distinct rows.
		final var child1 = FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD).withName("Alva Alvsson").withProcessDays(30).withIncluded(true);
		final var child2 = FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD).withName("Bo Bosson").withProcessDays(30).withIncluded(true);
		final var child3 = FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD).withName("Cecilia Cederlund").withProcessDays(30).withIncluded(true);

		final var diff = sectionReconciler.reconcilePersons(ERRAND_ID, List.of(child1, child2, child3));

		assertThat(diff.added()).hasSize(3);
	}

	@Test
	void newRowGetsTheNextPositionWhileARefreshedRowKeepsItsOwn() {
		final var existing = systemRow(20, "Bostadsbidrag", "1850").withPosition(0); // already positioned
		final var refreshedFresh = systemRow(20, "Bostadsbidrag", "2000"); // matches existing (same typeId)
		final var newFresh = systemRow(99, "Underhållsstöd", "500"); // brand new
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(new ArrayList<>(List.of(existing)));
		when(incomeRepository.nextPositionForErrand(ERRAND_ID)).thenReturn(1);

		sectionReconciler.reconcileIncomes(ERRAND_ID, List.of(refreshedFresh, newFresh));

		assertThat(existing.getPosition()).isZero(); // a refreshed row keeps the position it already had
		assertThat(newFresh.getPosition()).isEqualTo(1); // a genuinely new row is appended at the next free position
	}

	@Test
	void reconcileExpensesRefreshesProcessColumnsButPreservesTheWriteOnceAppliedAmount() {
		// a system expense row whose appliedAmount a caseworker has corrected (9999) since the last loop
		final var existing = FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("HOUSING_COST")
			.withAppliedAmount(new BigDecimal("9999")).withProcessAmount(new BigDecimal("8000"));
		// the freshly computed row for the same expense carries the original applied amount (9000) and a new process cap
		final var fresh = FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("HOUSING_COST")
			.withAppliedAmount(new BigDecimal("9000")).withProcessAmount(new BigDecimal("8500"));
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(new ArrayList<>(List.of(existing)));

		sectionReconciler.reconcileExpenses(ERRAND_ID, List.of(fresh));

		assertThat(existing.getAppliedAmount()).isEqualByComparingTo("9999"); // caseworker edit survives the daily refresh
		assertThat(existing.getProcessAmount()).isEqualByComparingTo("8500"); // process column is still refreshed
		verify(expenseRepository).save(existing);
	}

	@Test
	void reconcileExpensesLabelsAddedAndDroppedRowsForTheWarnings() {
		// existing system expense disappears from the fresh set (dropped); a different fresh expense appears (added)
		final var existing = FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("FOOD");
		final var fresh = FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("MEDICINE").withSpecification("Glasses");
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(new ArrayList<>(List.of(existing)));

		final var diff = sectionReconciler.reconcileExpenses(ERRAND_ID, List.of(fresh));

		assertThat(diff.added()).containsExactly("MEDICINE – Glasses"); // costType + specification
		assertThat(diff.dropped()).containsExactly("FOOD"); // costType only, no specification
		verify(expenseRepository).save(fresh);
	}

	@Test
	void nullFreshListIsTreatedAsEmpty() {
		final var existing = systemRow(20, "Bostadsbidrag", "1850");
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(new ArrayList<>(List.of(existing)));

		final var diff = sectionReconciler.reconcileIncomes(ERRAND_ID, null);

		assertThat(diff.added()).isEmpty();
		assertThat(diff.dropped()).containsExactly("Bostadsbidrag");
		verify(incomeRepository, never()).save(existing);
	}

	private static FaNormIncomeEntity systemRow(final Integer typeId, final String typeName, final String processAmount) {
		return FaNormIncomeEntity.create()
			.withOrigin(ORIGIN_SYSTEM)
			.withTypeId(typeId)
			.withTypeName(typeName)
			.withApplicantProcessAmount(processAmount == null ? null : new BigDecimal(processAmount));
	}
}
