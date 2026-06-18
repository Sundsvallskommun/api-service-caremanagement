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
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormExpenseRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormIncomeRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormPersonRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormberakningDraftRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormberakningDraftEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ORIGIN_HANDLAGGARE;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ROLE_CHILD;

@ExtendWith(MockitoExtension.class)
class DraftServiceTest {

	private static final String ERRAND_ID = "errand-1";
	private static final String ROW_ID = "row-1";

	@Mock
	private FaNormberakningDraftRepository headerRepository;
	@Mock
	private FaNormIncomeRepository incomeRepository;
	@Mock
	private FaNormExpenseRepository expenseRepository;
	@Mock
	private FaNormPersonRepository personRepository;

	@InjectMocks
	private DraftService service;

	@Test
	void effectiveValueHelpersPreferHandlaggare() {
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

		final var changes = service.refresh(ERRAND_ID, "2026-06", 7, "RIKSNORM", List.of(), List.of(income), List.of());

		assertThat(changes.addedIncomes()).containsExactly("Bostadsbidrag");
		final var header = ArgumentCaptor.forClass(FaNormberakningDraftEntity.class);
		verifySaved(header);
		assertThat(header.getValue().getNormId()).isEqualTo(7);
		assertThat(header.getValue().getNormType()).isEqualTo("RIKSNORM");
	}

	private void verifySaved(final ArgumentCaptor<FaNormberakningDraftEntity> captor) {
		org.mockito.Mockito.verify(headerRepository).save(captor.capture());
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
			FaNormberakningDraftEntity.create().withErrandId(ERRAND_ID).withApplicationMonth("2026-06").withNormId(7).withNormType("RIKSNORM")));
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(20).withApplicantProcessAmount(new BigDecimal("1000")).withApplicantHandlaggareAmount(new BigDecimal("1200")),
			FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(21).withApplicantProcessAmount(new BigDecimal("500")).withDeleted(true)));
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("RENT").withAppliedAmount(new BigDecimal("9000")).withProcessAmount(new BigDecimal("8000"))));
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD).withProcessDays(15).withHandlaggareDays(20)));

		final var draft = service.get(ERRAND_ID);

		assertThat(draft.getNormId()).isEqualTo(7);
		assertThat(draft.getIncomes()).hasSize(2);
		assertThat(draft.getIncomes().getFirst().getApplicantEffectiveAmount()).isEqualByComparingTo("1200"); // handläggare wins
		assertThat(draft.getExpenses().getFirst().getEffectiveAmount()).isEqualByComparingTo("8000"); // process (no override)
		assertThat(draft.getPersons().getFirst().getEffectiveDays()).isEqualTo(20);
		assertThat(draft.getIncomeSum()).isEqualByComparingTo("1200"); // deleted income (500) excluded
		assertThat(draft.getExpenseSum()).isEqualByComparingTo("8000");
	}

	@Test
	void addIncomeRequiresHeaderThenSavesHandlaggareRow() {
		when(headerRepository.existsById(ERRAND_ID)).thenReturn(true);
		when(incomeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var input = new NormIncomeInput().withTypeId(20).withTypeName("Lön").withApplicantHandlaggareAmount(new BigDecimal("3000")).withNote("manuell");
		final var row = service.addIncome(ERRAND_ID, input);

		assertThat(row.getOrigin()).isEqualTo(ORIGIN_HANDLAGGARE);
		assertThat(row.getApplicantHandlaggareAmount()).isEqualByComparingTo("3000");
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
	void patchIncomeSetsOnlyHandlaggareFields() {
		final var existing = FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withApplicantProcessAmount(new BigDecimal("1000"));
		when(incomeRepository.findByIdAndErrandId(ROW_ID, ERRAND_ID)).thenReturn(Optional.of(existing));
		when(incomeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var row = service.patchIncome(ERRAND_ID, ROW_ID, new NormIncomeInput().withApplicantHandlaggareAmount(new BigDecimal("1100")).withNote("ok"));

		assertThat(row.getApplicantProcessAmount()).isEqualByComparingTo("1000"); // untouched
		assertThat(row.getApplicantHandlaggareAmount()).isEqualByComparingTo("1100");
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

		assertThat(service.addExpense(ERRAND_ID, new NormExpenseInput().withCostType("RENT").withHandlaggareAmount(new BigDecimal("7500"))).getEffectiveAmount()).isEqualByComparingTo("7500");
		assertThat(service.patchExpense(ERRAND_ID, ROW_ID, new NormExpenseInput().withHandlaggareAmount(new BigDecimal("7000"))).getHandlaggareAmount()).isEqualByComparingTo("7000");
		assertThat(service.setExpenseDeleted(ERRAND_ID, ROW_ID, true).isDeleted()).isTrue();

		assertThat(service.addPerson(ERRAND_ID, new NormPersonInput().withPartyId("p1").withRole(ROLE_CHILD).withHandlaggareDays(10)).getEffectiveDays()).isEqualTo(10);
		assertThat(service.patchPerson(ERRAND_ID, ROW_ID, new NormPersonInput().withHandlaggareDays(12)).getHandlaggareDays()).isEqualTo(12);
		assertThat(service.setPersonDeleted(ERRAND_ID, ROW_ID, true).isDeleted()).isTrue();
	}

	@Test
	void liveReadersFilterOutSoftDeletedRows() {
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormIncomeEntity.create().withTypeId(20), FaNormIncomeEntity.create().withTypeId(21).withDeleted(true)));
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(FaNormExpenseEntity.create().withCostType("RENT")));
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			FaNormPersonEntity.create().withPartyId("p1").withIncluded(true), FaNormPersonEntity.create().withPartyId("p2").withIncluded(false)));

		assertThat(service.liveIncomes(ERRAND_ID)).hasSize(1);
		assertThat(service.liveExpenses(ERRAND_ID)).hasSize(1);
		assertThat(service.livePersons(ERRAND_ID)).hasSize(1); // p2 excluded (omfattas = false)
	}

	@Test
	void headerDelegatesToRepository() {
		final var header = FaNormberakningDraftEntity.create().withErrandId(ERRAND_ID);
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.of(header));

		assertThat(service.header(ERRAND_ID)).contains(header);
	}

	@Test
	void patchHeaderUpdatesNormAndHouseholdThenReturnsDraft() {
		final var header = FaNormberakningDraftEntity.create().withErrandId(ERRAND_ID);
		when(headerRepository.findById(ERRAND_ID)).thenReturn(Optional.of(header));
		when(headerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(incomeRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(expenseRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());
		when(personRepository.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		final var draft = service.patchHeader(ERRAND_ID, new NormHeaderInput().withNormId(9).withNormType("RIKSNORM")
			.withCalculationFromDate(LocalDate.of(2026, 6, 1)).withCalculationToDate(LocalDate.of(2026, 6, 30)).withCalculationDate(LocalDate.of(2026, 6, 18))
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
