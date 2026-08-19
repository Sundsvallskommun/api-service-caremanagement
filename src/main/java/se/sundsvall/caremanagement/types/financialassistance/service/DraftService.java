package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaCalculationDraftRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormExpenseRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormIncomeRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormPersonRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.CalculationDraftMapper;
import se.sundsvall.caremanagement.types.financialassistance.service.model.DraftChanges;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * The editable draft calculation across its sections — persons, incomes, expenses and other living costs (the expense
 * bucket {@code SPECIAL_EXPENSE}) — mirroring the Lifecare Calculation tabs. The daily prepare {@link #refresh
 * refreshes}
 * the process columns from the freshly computed rows (delegated per section to {@link SectionReconciler}); the per-row
 * caseworker operations touch only the caseworker columns and the soft-delete flag, and {@link #patchHeader} sets the
 * norm, the calculation dates and the custom household size (common costs). The entity ↔ API mapping lives in
 * {@link CalculationDraftMapper}; effective value = caseworker value when set, otherwise process value.
 */
@Service
public class DraftService {

	private static final String NO_DRAFT_FOR_ERRAND = "No draft calculation for errand";

	private final FaCalculationDraftRepository calculationDraftRepository;
	private final FaNormIncomeRepository incomeRepository;
	private final FaNormExpenseRepository expenseRepository;
	private final FaNormPersonRepository personRepository;
	private final SectionReconciler sectionReconciler;

	DraftService(final FaCalculationDraftRepository calculationDraftRepository, final FaNormIncomeRepository incomeRepository,
		final FaNormExpenseRepository expenseRepository, final FaNormPersonRepository personRepository, final SectionReconciler sectionReconciler) {
		this.calculationDraftRepository = calculationDraftRepository;
		this.incomeRepository = incomeRepository;
		this.expenseRepository = expenseRepository;
		this.personRepository = personRepository;
		this.sectionReconciler = sectionReconciler;
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Process path — the daily refresh. Touches process columns + inserts only; caseworker columns and soft-deletes
	// survive.
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Refresh the draft from the freshly computed process rows. Upserts the header (application month, selected norm and
	 * the calculation date window derived from the month) and reconciles each section, returning what changed so the
	 * caller can raise warnings.
	 */
	@Transactional
	public DraftChanges refresh(final String errandId, final String applicationMonth, final Integer normId, final List<String> normType,
		final List<FaNormPersonEntity> freshPersons, final List<FaNormIncomeEntity> freshIncomes, final List<FaNormExpenseEntity> freshExpenses) {

		upsertHeader(errandId, applicationMonth, normId, normType);

		final var persons = sectionReconciler.reconcilePersons(errandId, freshPersons);
		final var incomes = sectionReconciler.reconcileIncomes(errandId, freshIncomes);
		final var expenses = sectionReconciler.reconcileExpenses(errandId, freshExpenses);

		return new DraftChanges(incomes.added(), incomes.dropped(), expenses.added(), expenses.dropped(), persons.added(), persons.dropped());
	}

	private void upsertHeader(final String errandId, final String applicationMonth, final Integer normId, final List<String> normType) {
		final var header = calculationDraftRepository.findById(errandId).orElseGet(() -> FaCalculationDraftEntity.create().withErrandId(errandId));
		ofNullable(applicationMonth).filter(StringUtils::hasText).ifPresent(month -> {
			header.setApplicationMonth(month);
			final var parsed = YearMonth.parse(month);
			header.setCalculationFromDate(parsed.atDay(1));
			header.setCalculationToDate(parsed.atEndOfMonth());
		});
		ofNullable(normId).ifPresent(header::setNormId);
		ofNullable(normType).filter(list -> !list.isEmpty()).ifPresent(header::setNormType);
		header.setCalculationDate(LocalDate.now(ZoneId.systemDefault()));
		calculationDraftRepository.save(header);
	}

	/** Caseworker edit of the header — the norm, the calculation date window and the custom household size. */
	@Transactional
	public CalculationDraft patchHeader(final String errandId, final NormHeaderInput input) {
		final var header = calculationDraftRepository.findById(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NO_DRAFT_FOR_ERRAND));
		ofNullable(input.getNormId()).ifPresent(header::setNormId);
		ofNullable(input.getNormType()).filter(list -> !list.isEmpty()).ifPresent(header::setNormType);
		ofNullable(input.getCalculationFromDate()).ifPresent(header::setCalculationFromDate);
		ofNullable(input.getCalculationToDate()).ifPresent(header::setCalculationToDate);
		ofNullable(input.getCalculationDate()).ifPresent(header::setCalculationDate);
		ofNullable(input.getHasCustomHouseholdSize()).ifPresent(header::setHasCustomHouseholdSize);
		ofNullable(input.getHouseholdSize()).ifPresent(header::setHouseholdSize);
		calculationDraftRepository.save(header);
		return loadDraft(errandId);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Read — the full draft view a caseworker sees in Draken.
	// ------------------------------------------------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public CalculationDraft get(final String errandId) {
		return loadDraft(errandId);
	}

	// Shared read used by both the public read endpoint and the write methods. Kept out of @Transactional self-invocation:
	// a write method calling the readOnly get() via 'this' would bypass the proxy and ignore the readOnly hint anyway.
	private CalculationDraft loadDraft(final String errandId) {
		final var header = calculationDraftRepository.findById(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NO_DRAFT_FOR_ERRAND));
		return CalculationDraftMapper.toCalculationDraft(header,
			incomeRepository.findByErrandId(errandId), expenseRepository.findByErrandId(errandId), personRepository.findByErrandId(errandId));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Caseworker path — per-row edits. Touch only caseworker columns / soft-delete; never the process columns.
	// ------------------------------------------------------------------------------------------------------------------

	@Transactional
	public NormIncomeRow addIncome(final String errandId, final NormIncomeInput input) {
		requireHeader(errandId);
		final var entity = incomeRepository.save(CalculationDraftMapper.toNewIncomeEntity(errandId, incomeRepository.nextPositionForErrand(errandId), input));
		return CalculationDraftMapper.toIncomeRow(entity);
	}

	@Transactional
	public NormIncomeRow patchIncome(final String errandId, final String rowId, final NormIncomeInput input) {
		final var entity = requireIncome(errandId, rowId);
		entity.setApplicantCaseworkerAmount(input.getApplicantCaseworkerAmount());
		entity.setCoapplicantCaseworkerAmount(input.getCoapplicantCaseworkerAmount());
		entity.setNote(input.getNote());
		return CalculationDraftMapper.toIncomeRow(incomeRepository.save(entity));
	}

	@Transactional
	public NormIncomeRow setIncomeDeleted(final String errandId, final String rowId, final boolean deleted) {
		final var entity = requireIncome(errandId, rowId);
		entity.setDeleted(deleted);
		return CalculationDraftMapper.toIncomeRow(incomeRepository.save(entity));
	}

	@Transactional
	public NormExpenseRow addExpense(final String errandId, final NormExpenseInput input) {
		requireHeader(errandId);
		final var entity = expenseRepository.save(CalculationDraftMapper.toNewExpenseEntity(errandId, expenseRepository.nextPositionForErrand(errandId), input));
		return CalculationDraftMapper.toExpenseRow(entity);
	}

	@Transactional
	public NormExpenseRow patchExpense(final String errandId, final String rowId, final NormExpenseInput input) {
		final var entity = requireExpense(errandId, rowId);
		// appliedAmount is the citizen's declared (ansökt) figure and is write-once — preserved across the daily refresh
		// (see SectionReconciler#copyExpenseProcess). Only overwrite it when the patch actually supplies a value, so a
		// partial patch that omits it cannot silently erase it (which the refresh would then never restore).
		ofNullable(input.getAppliedAmount()).ifPresent(entity::setAppliedAmount);
		entity.setCaseworkerAmount(input.getCaseworkerAmount());
		entity.setNote(input.getNote());
		return CalculationDraftMapper.toExpenseRow(expenseRepository.save(entity));
	}

	@Transactional
	public NormExpenseRow setExpenseDeleted(final String errandId, final String rowId, final boolean deleted) {
		final var entity = requireExpense(errandId, rowId);
		entity.setDeleted(deleted);
		return CalculationDraftMapper.toExpenseRow(expenseRepository.save(entity));
	}

	@Transactional
	public NormPersonRow addPerson(final String errandId, final NormPersonInput input) {
		requireHeader(errandId);
		final var entity = personRepository.save(CalculationDraftMapper.toNewPersonEntity(errandId, personRepository.nextPositionForErrand(errandId), input));
		return CalculationDraftMapper.toPersonRow(entity);
	}

	@Transactional
	public NormPersonRow patchPerson(final String errandId, final String rowId, final NormPersonInput input) {
		final var entity = requirePerson(errandId, rowId);
		entity.setCaseworkerDays(input.getCaseworkerDays());
		ofNullable(input.getIncluded()).ifPresent(entity::setIncluded);
		entity.setDeviationFromDate(input.getDeviationFromDate());
		entity.setDeviationToDate(input.getDeviationToDate());
		entity.setNormInterval(input.getNormInterval());
		entity.setJobStimulusAmount(input.getJobStimulusAmount());
		entity.setNote(input.getNote());
		return CalculationDraftMapper.toPersonRow(personRepository.save(entity));
	}

	@Transactional
	public NormPersonRow setPersonDeleted(final String errandId, final String rowId, final boolean deleted) {
		final var entity = requirePerson(errandId, rowId);
		entity.setDeleted(deleted);
		return CalculationDraftMapper.toPersonRow(personRepository.save(entity));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Commit path — the effective (live, non-deleted) rows posted to Lifecare on a decision.
	// ------------------------------------------------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public Optional<FaCalculationDraftEntity> header(final String errandId) {
		return calculationDraftRepository.findById(errandId);
	}

	@Transactional(readOnly = true)
	public List<FaNormIncomeEntity> liveIncomes(final String errandId) {
		return incomeRepository.findByErrandId(errandId).stream().filter(row -> !row.isDeleted()).toList();
	}

	@Transactional(readOnly = true)
	public List<FaNormExpenseEntity> liveExpenses(final String errandId) {
		return expenseRepository.findByErrandId(errandId).stream().filter(row -> !row.isDeleted()).toList();
	}

	@Transactional(readOnly = true)
	public List<FaNormPersonEntity> livePersons(final String errandId) {
		return personRepository.findByErrandId(errandId).stream().filter(row -> !row.isDeleted() && row.isIncluded()).toList();
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Internals
	// ------------------------------------------------------------------------------------------------------------------

	private void requireHeader(final String errandId) {
		if (!calculationDraftRepository.existsById(errandId)) {
			throw Problem.valueOf(NOT_FOUND, NO_DRAFT_FOR_ERRAND);
		}
	}

	private FaNormIncomeEntity requireIncome(final String errandId, final String rowId) {
		return incomeRepository.findByIdAndErrandId(rowId, errandId).orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No such income row on the errand's draft"));
	}

	private FaNormExpenseEntity requireExpense(final String errandId, final String rowId) {
		return expenseRepository.findByIdAndErrandId(rowId, errandId).orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No such expense row on the errand's draft"));
	}

	private FaNormPersonEntity requirePerson(final String errandId, final String rowId) {
		return personRepository.findByIdAndErrandId(rowId, errandId).orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No such person row on the errand's draft"));
	}
}
