package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
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
import se.sundsvall.caremanagement.types.financialassistance.service.model.DraftChanges;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.BUCKET_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.BUCKET_SPECIAL_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_CASEWORKER;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_SYSTEM;

/**
 * The editable draft calculation across its sections — persons, incomes, expenses and other living costs
 * (the expense bucket {@code SPECIAL_EXPENSE}) — mirroring the Lifecare Calculation tabs. The daily prepare
 * {@link #refresh refreshes} the process columns from the freshly computed rows; the per-row caseworker operations
 * touch only the caseworker columns and the soft-delete flag, and {@link #patchHeader} sets the norm, the calculation
 * dates and the custom household size (common costs). The merge invariant lives in {@link SectionReconciler};
 * effective value = caseworker value when set, otherwise process value.
 */
@Service
public class DraftService {

	private final FaCalculationDraftRepository headerRepository;
	private final FaNormIncomeRepository incomeRepository;
	private final FaNormExpenseRepository expenseRepository;
	private final FaNormPersonRepository personRepository;

	DraftService(final FaCalculationDraftRepository headerRepository, final FaNormIncomeRepository incomeRepository,
		final FaNormExpenseRepository expenseRepository, final FaNormPersonRepository personRepository) {
		this.headerRepository = headerRepository;
		this.incomeRepository = incomeRepository;
		this.expenseRepository = expenseRepository;
		this.personRepository = personRepository;
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
	public DraftChanges refresh(final String errandId, final String applicationMonth, final Integer normId, final String normType,
		final List<FaNormPersonEntity> freshPersons, final List<FaNormIncomeEntity> freshIncomes, final List<FaNormExpenseEntity> freshExpenses) {

		upsertHeader(errandId, applicationMonth, normId, normType);

		// New rows inserted by the reconcile get the next stable position appended after the existing rows; refreshed rows
		// keep the position they already have.
		final var personSaver = positioningSaver(personRepository.nextPositionForErrand(errandId), FaNormPersonEntity::getPosition, FaNormPersonEntity::setPosition,
			personRepository::save);
		final var incomeSaver = positioningSaver(incomeRepository.nextPositionForErrand(errandId), FaNormIncomeEntity::getPosition, FaNormIncomeEntity::setPosition,
			incomeRepository::save);
		final var expenseSaver = positioningSaver(expenseRepository.nextPositionForErrand(errandId), FaNormExpenseEntity::getPosition, FaNormExpenseEntity::setPosition,
			expenseRepository::save);

		final var persons = SectionReconciler.reconcile(personRepository.findByErrandId(errandId), nullSafe(freshPersons),
			DraftService::personKey, isSystem(FaNormPersonEntity::getOrigin), DraftService::copyPersonProcess, DraftService::personLabel, personSaver);

		final var incomes = SectionReconciler.reconcile(incomeRepository.findByErrandId(errandId), nullSafe(freshIncomes),
			DraftService::incomeKey, isSystem(FaNormIncomeEntity::getOrigin), DraftService::copyIncomeProcess, DraftService::incomeLabel, incomeSaver);

		final var expenses = SectionReconciler.reconcile(expenseRepository.findByErrandId(errandId), nullSafe(freshExpenses),
			DraftService::expenseKey, isSystem(FaNormExpenseEntity::getOrigin), DraftService::copyExpenseProcess, DraftService::expenseLabel, expenseSaver);

		return new DraftChanges(incomes.added(), incomes.dropped(), expenses.added(), expenses.dropped(), persons.added(), persons.dropped());
	}

	private void upsertHeader(final String errandId, final String applicationMonth, final Integer normId, final String normType) {
		final var header = headerRepository.findById(errandId).orElseGet(() -> FaCalculationDraftEntity.create().withErrandId(errandId));
		ofNullable(applicationMonth).filter(StringUtils::hasText).ifPresent(month -> {
			header.setApplicationMonth(month);
			final var parsed = YearMonth.parse(month);
			header.setCalculationFromDate(parsed.atDay(1));
			header.setCalculationToDate(parsed.atEndOfMonth());
		});
		ofNullable(normId).ifPresent(header::setNormId);
		ofNullable(normType).filter(StringUtils::hasText).ifPresent(header::setNormType);
		header.setCalculationDate(LocalDate.now());
		headerRepository.save(header);
	}

	/** Caseworker edit of the header — the norm, the calculation date window and the custom household size. */
	@Transactional
	public CalculationDraft patchHeader(final String errandId, final NormHeaderInput input) {
		final var header = headerRepository.findById(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No draft calculation for errand"));
		ofNullable(input.getNormId()).ifPresent(header::setNormId);
		ofNullable(input.getNormType()).filter(StringUtils::hasText).ifPresent(header::setNormType);
		ofNullable(input.getCalculationFromDate()).ifPresent(header::setCalculationFromDate);
		ofNullable(input.getCalculationToDate()).ifPresent(header::setCalculationToDate);
		ofNullable(input.getCalculationDate()).ifPresent(header::setCalculationDate);
		ofNullable(input.getHasCustomHouseholdSize()).ifPresent(header::setHasCustomHouseholdSize);
		ofNullable(input.getHouseholdSize()).ifPresent(header::setHouseholdSize);
		headerRepository.save(header);
		return get(errandId);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Read — the full draft view a caseworker sees in Draken.
	// ------------------------------------------------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public CalculationDraft get(final String errandId) {
		final var header = headerRepository.findById(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No draft calculation for errand"));

		final var incomes = incomeRepository.findByErrandId(errandId).stream()
			.sorted(comparing(FaNormIncomeEntity::getPosition, nullsLast(naturalOrder()))).map(DraftService::toIncomeRow).toList();
		final var allExpenses = expenseRepository.findByErrandId(errandId).stream()
			.sorted(comparing(FaNormExpenseEntity::getPosition, nullsLast(naturalOrder()))).map(DraftService::toExpenseRow).toList();
		final var expenses = allExpenses.stream().filter(row -> !BUCKET_SPECIAL_EXPENSE.equals(row.getBucket())).toList();
		final var specialExpenses = allExpenses.stream().filter(row -> BUCKET_SPECIAL_EXPENSE.equals(row.getBucket())).toList();
		final var persons = personRepository.findByErrandId(errandId).stream()
			.sorted(comparing(FaNormPersonEntity::getPosition, nullsLast(naturalOrder()))).map(DraftService::toPersonRow).toList();

		return CalculationDraft.create()
			.withErrandId(header.getErrandId())
			.withApplicationMonth(header.getApplicationMonth())
			.withNormId(header.getNormId())
			.withNormType(header.getNormType())
			.withCalculationFromDate(header.getCalculationFromDate())
			.withCalculationToDate(header.getCalculationToDate())
			.withCalculationDate(header.getCalculationDate())
			.withHasCustomHouseholdSize(header.getHasCustomHouseholdSize())
			.withHouseholdSize(header.getHouseholdSize())
			.withPersons(persons)
			.withIncomes(incomes)
			.withExpenses(expenses)
			.withSpecialExpenses(specialExpenses)
			.withIncomeSum(sum(incomes.stream().filter(row -> !row.isDeleted()).flatMap(DraftService::incomeEffectiveAmounts)))
			.withExpenseSum(sum(expenses.stream().filter(row -> !row.isDeleted()).map(NormExpenseRow::getEffectiveAmount)))
			.withSpecialExpenseSum(sum(specialExpenses.stream().filter(row -> !row.isDeleted()).map(NormExpenseRow::getEffectiveAmount)))
			.withCreated(header.getCreated())
			.withUpdated(header.getUpdated());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Caseworker path — per-row edits. Touch only caseworker columns / soft-delete; never the process columns.
	// ------------------------------------------------------------------------------------------------------------------

	@Transactional
	public NormIncomeRow addIncome(final String errandId, final NormIncomeInput input) {
		requireHeader(errandId);
		final var entity = incomeRepository.save(FaNormIncomeEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_CASEWORKER).withPosition(incomeRepository.nextPositionForErrand(errandId))
			.withTypeId(input.getTypeId()).withTypeName(input.getTypeName())
			.withApplicantCaseworkerAmount(input.getApplicantCaseworkerAmount()).withApplicantAmountDate(input.getApplicantAmountDate())
			.withCoapplicantCaseworkerAmount(input.getCoapplicantCaseworkerAmount()).withCoapplicantAmountDate(input.getCoapplicantAmountDate())
			.withNote(input.getNote()));
		return toIncomeRow(entity);
	}

	@Transactional
	public NormIncomeRow patchIncome(final String errandId, final String rowId, final NormIncomeInput input) {
		final var entity = requireIncome(errandId, rowId);
		entity.setApplicantCaseworkerAmount(input.getApplicantCaseworkerAmount());
		entity.setCoapplicantCaseworkerAmount(input.getCoapplicantCaseworkerAmount());
		entity.setNote(input.getNote());
		return toIncomeRow(incomeRepository.save(entity));
	}

	@Transactional
	public NormIncomeRow setIncomeDeleted(final String errandId, final String rowId, final boolean deleted) {
		final var entity = requireIncome(errandId, rowId);
		entity.setDeleted(deleted);
		return toIncomeRow(incomeRepository.save(entity));
	}

	@Transactional
	public NormExpenseRow addExpense(final String errandId, final NormExpenseInput input) {
		requireHeader(errandId);
		final var entity = expenseRepository.save(FaNormExpenseEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_CASEWORKER).withPosition(expenseRepository.nextPositionForErrand(errandId)).withBucket(bucketOrDefault(input.getBucket()))
			.withCostType(input.getCostType()).withOtherSubType(input.getOtherSubType()).withSpecification(input.getSpecification())
			.withAppliedAmount(input.getAppliedAmount()).withCaseworkerAmount(input.getCaseworkerAmount()).withNote(input.getNote()));
		return toExpenseRow(entity);
	}

	@Transactional
	public NormExpenseRow patchExpense(final String errandId, final String rowId, final NormExpenseInput input) {
		final var entity = requireExpense(errandId, rowId);
		entity.setAppliedAmount(input.getAppliedAmount());
		entity.setCaseworkerAmount(input.getCaseworkerAmount());
		entity.setNote(input.getNote());
		return toExpenseRow(expenseRepository.save(entity));
	}

	@Transactional
	public NormExpenseRow setExpenseDeleted(final String errandId, final String rowId, final boolean deleted) {
		final var entity = requireExpense(errandId, rowId);
		entity.setDeleted(deleted);
		return toExpenseRow(expenseRepository.save(entity));
	}

	@Transactional
	public NormPersonRow addPerson(final String errandId, final NormPersonInput input) {
		requireHeader(errandId);
		final var entity = personRepository.save(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_CASEWORKER).withPosition(personRepository.nextPositionForErrand(errandId))
			.withPartyId(input.getPartyId()).withRole(input.getRole()).withName(input.getName())
			.withCaseworkerDays(input.getCaseworkerDays()).withIncluded(input.getIncluded() == null || input.getIncluded())
			.withDeviationFromDate(input.getDeviationFromDate()).withDeviationToDate(input.getDeviationToDate())
			.withNormInterval(input.getNormInterval()).withJobStimulusAmount(input.getJobStimulusAmount()).withNote(input.getNote()));
		return toPersonRow(entity);
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
		return toPersonRow(personRepository.save(entity));
	}

	@Transactional
	public NormPersonRow setPersonDeleted(final String errandId, final String rowId, final boolean deleted) {
		final var entity = requirePerson(errandId, rowId);
		entity.setDeleted(deleted);
		return toPersonRow(personRepository.save(entity));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Commit path — the effective (live, non-deleted) rows posted to Lifecare on a decision.
	// ------------------------------------------------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public Optional<FaCalculationDraftEntity> header(final String errandId) {
		return headerRepository.findById(errandId);
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
	// Effective-value helpers
	// ------------------------------------------------------------------------------------------------------------------

	public static BigDecimal effectiveAmount(final BigDecimal caseworkerAmount, final BigDecimal processAmount) {
		return caseworkerAmount != null ? caseworkerAmount : processAmount;
	}

	public static Integer effectiveDays(final Integer caseworkerDays, final Integer processDays) {
		return caseworkerDays != null ? caseworkerDays : processDays;
	}

	/**
	 * A save consumer that stamps a stable position on any row that doesn't have one yet, handing out consecutive
	 * positions from {@code start} (the next free position for the errand) so new rows append after the existing ones.
	 */
	private static <E> Consumer<E> positioningSaver(final int start, final Function<E, Integer> getPosition, final BiConsumer<E, Integer> setPosition,
		final Consumer<E> save) {
		final var next = new AtomicInteger(start);
		return entity -> {
			if (getPosition.apply(entity) == null) {
				setPosition.accept(entity, next.getAndIncrement());
			}
			save.accept(entity);
		};
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Internals
	// ------------------------------------------------------------------------------------------------------------------

	private void requireHeader(final String errandId) {
		if (!headerRepository.existsById(errandId)) {
			throw Problem.valueOf(NOT_FOUND, "No draft calculation for errand");
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

	private static String bucketOrDefault(final String bucket) {
		return BUCKET_SPECIAL_EXPENSE.equals(bucket) ? BUCKET_SPECIAL_EXPENSE : BUCKET_EXPENSE;
	}

	private static <E> List<E> nullSafe(final List<E> list) {
		return ofNullable(list).orElseGet(List::of);
	}

	private static <E> Predicate<E> isSystem(final Function<E, String> originOf) {
		return entity -> ORIGIN_SYSTEM.equals(originOf.apply(entity));
	}

	private static Stream<BigDecimal> incomeEffectiveAmounts(final NormIncomeRow row) {
		return Stream.of(row.getApplicantEffectiveAmount(), row.getCoapplicantEffectiveAmount());
	}

	private static BigDecimal sum(final Stream<BigDecimal> amounts) {
		return amounts.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// --- identity keys ---

	private static String incomeKey(final FaNormIncomeEntity e) {
		return String.valueOf(e.getTypeId());
	}

	private static String expenseKey(final FaNormExpenseEntity e) {
		return e.getCostType() + "|" + ofNullable(e.getOtherSubType()).orElse("") + "|" + ofNullable(e.getSpecification()).orElse("") + "|" + ofNullable(e.getBucket()).orElse("");
	}

	private static String personKey(final FaNormPersonEntity e) {
		return e.getPartyId() + "|" + e.getRole();
	}

	// --- process-column copy (refresh) ---

	private static void copyIncomeProcess(final FaNormIncomeEntity target, final FaNormIncomeEntity fresh) {
		target.setTypeName(fresh.getTypeName());
		target.setApplicantProcessAmount(fresh.getApplicantProcessAmount());
		target.setApplicantAmountDate(fresh.getApplicantAmountDate());
		target.setCoapplicantProcessAmount(fresh.getCoapplicantProcessAmount());
		target.setCoapplicantAmountDate(fresh.getCoapplicantAmountDate());
	}

	private static void copyExpenseProcess(final FaNormExpenseEntity target, final FaNormExpenseEntity fresh) {
		// appliedAmount is write-once — it is what the citizen applied for, set when the row is first inserted and editable
		// by a caseworker (patchExpense). The daily refresh must NOT overwrite it, or a caseworker correction on a system
		// row is lost next loop. Only the genuine process columns (the rules cap + its bucket) refresh.
		target.setProcessAmount(fresh.getProcessAmount());
		target.setBucket(fresh.getBucket());
	}

	private static void copyPersonProcess(final FaNormPersonEntity target, final FaNormPersonEntity fresh) {
		target.setName(fresh.getName());
		target.setProcessDays(fresh.getProcessDays());
	}

	// --- warning labels ---

	private static String incomeLabel(final FaNormIncomeEntity e) {
		return ofNullable(e.getTypeName()).orElse("Income");
	}

	private static String expenseLabel(final FaNormExpenseEntity e) {
		return ofNullable(e.getCostType()).orElse("Expense") + ofNullable(e.getSpecification()).map(spec -> " – " + spec).orElse("");
	}

	private static String personLabel(final FaNormPersonEntity e) {
		return ofNullable(e.getName()).orElse(ofNullable(e.getPartyId()).orElse("Person")) + " (" + e.getRole() + ")";
	}

	// --- entity -> view row ---

	private static NormIncomeRow toIncomeRow(final FaNormIncomeEntity e) {
		return NormIncomeRow.create()
			.withId(e.getId()).withOrigin(e.getOrigin()).withPosition(e.getPosition()).withTypeId(e.getTypeId()).withTypeName(e.getTypeName())
			.withApplicantProcessAmount(e.getApplicantProcessAmount()).withApplicantCaseworkerAmount(e.getApplicantCaseworkerAmount())
			.withApplicantEffectiveAmount(effectiveAmount(e.getApplicantCaseworkerAmount(), e.getApplicantProcessAmount())).withApplicantAmountDate(e.getApplicantAmountDate())
			.withCoapplicantProcessAmount(e.getCoapplicantProcessAmount()).withCoapplicantCaseworkerAmount(e.getCoapplicantCaseworkerAmount())
			.withCoapplicantEffectiveAmount(effectiveAmount(e.getCoapplicantCaseworkerAmount(), e.getCoapplicantProcessAmount())).withCoapplicantAmountDate(e.getCoapplicantAmountDate())
			.withDeleted(e.isDeleted()).withNote(e.getNote()).withCreated(e.getCreated()).withUpdated(e.getUpdated());
	}

	private static NormExpenseRow toExpenseRow(final FaNormExpenseEntity e) {
		final var effective = effectiveAmount(e.getCaseworkerAmount(), e.getProcessAmount());
		return NormExpenseRow.create()
			.withId(e.getId()).withOrigin(e.getOrigin()).withPosition(e.getPosition()).withBucket(e.getBucket()).withCostType(e.getCostType()).withOtherSubType(e.getOtherSubType())
			.withSpecification(e.getSpecification())
			.withAppliedAmount(e.getAppliedAmount()).withProcessAmount(e.getProcessAmount()).withCaseworkerAmount(e.getCaseworkerAmount())
			.withEffectiveAmount(effective).withDeleted(e.isDeleted()).withNote(e.getNote())
			.withCreated(e.getCreated()).withUpdated(e.getUpdated());
	}

	private static NormPersonRow toPersonRow(final FaNormPersonEntity e) {
		final var effective = effectiveDays(e.getCaseworkerDays(), e.getProcessDays());
		return NormPersonRow.create()
			.withId(e.getId()).withOrigin(e.getOrigin()).withPosition(e.getPosition()).withPartyId(e.getPartyId()).withRole(e.getRole()).withName(e.getName())
			.withProcessDays(e.getProcessDays()).withCaseworkerDays(e.getCaseworkerDays()).withEffectiveDays(effective)
			.withIncluded(e.isIncluded()).withDeviationFromDate(e.getDeviationFromDate()).withDeviationToDate(e.getDeviationToDate())
			.withNormInterval(e.getNormInterval()).withJobStimulusAmount(e.getJobStimulusAmount())
			.withDeleted(e.isDeleted()).withNote(e.getNote()).withCreated(e.getCreated()).withUpdated(e.getUpdated());
	}
}
