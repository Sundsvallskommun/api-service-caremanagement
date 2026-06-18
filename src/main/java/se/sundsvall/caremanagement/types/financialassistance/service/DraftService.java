package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningDraft;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormExpenseRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormIncomeRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormPersonRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormberakningDraftRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormberakningDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.model.DraftChanges;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ORIGIN_HANDLAGGARE;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ORIGIN_SYSTEM;

/**
 * The editable draft normberäkning across its three sections — personer, inkomster and utgifter. The daily prepare
 * {@link #refresh refreshes} the process columns of each section from the freshly computed rows, while the per-row
 * handläggare operations ({@link #patchIncome}, {@link #softDeleteExpense}, {@link #addPerson}, …) touch only the
 * handläggare columns and the soft-delete flag. The merge invariant lives in {@link SectionReconciler}; this service
 * wires it to the three repositories and maps entities to the API view (effective value = handläggare value when set,
 * otherwise process value).
 */
@Service
public class DraftService {

	private final FaNormberakningDraftRepository headerRepository;
	private final FaNormIncomeRepository incomeRepository;
	private final FaNormExpenseRepository expenseRepository;
	private final FaNormPersonRepository personRepository;

	DraftService(final FaNormberakningDraftRepository headerRepository, final FaNormIncomeRepository incomeRepository,
		final FaNormExpenseRepository expenseRepository, final FaNormPersonRepository personRepository) {
		this.headerRepository = headerRepository;
		this.incomeRepository = incomeRepository;
		this.expenseRepository = expenseRepository;
		this.personRepository = personRepository;
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Process path — the daily refresh. Touches process columns + inserts only; handläggare columns and soft-deletes
	// survive.
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Refresh the draft from the freshly computed process rows. Upserts the header (application month + selected norm) and
	 * reconciles each section, returning what changed so the caller can raise warnings.
	 */
	@Transactional
	public DraftChanges refresh(final String errandId, final String applicationMonth, final Integer normId, final String normType,
		final List<FaNormPersonEntity> freshPersons, final List<FaNormIncomeEntity> freshIncomes, final List<FaNormExpenseEntity> freshExpenses) {

		upsertHeader(errandId, applicationMonth, normId, normType);

		final var persons = SectionReconciler.reconcile(personRepository.findByErrandId(errandId), nullSafe(freshPersons),
			DraftService::personKey, isSystem(FaNormPersonEntity::getOrigin), DraftService::copyPersonProcess, DraftService::personLabel, personRepository::save);

		final var incomes = SectionReconciler.reconcile(incomeRepository.findByErrandId(errandId), nullSafe(freshIncomes),
			DraftService::incomeKey, isSystem(FaNormIncomeEntity::getOrigin), DraftService::copyIncomeProcess, DraftService::incomeLabel, incomeRepository::save);

		final var expenses = SectionReconciler.reconcile(expenseRepository.findByErrandId(errandId), nullSafe(freshExpenses),
			DraftService::expenseKey, isSystem(FaNormExpenseEntity::getOrigin), DraftService::copyExpenseProcess, DraftService::expenseLabel, expenseRepository::save);

		return new DraftChanges(incomes.added(), incomes.dropped(), expenses.added(), expenses.dropped(), persons.added(), persons.dropped());
	}

	private void upsertHeader(final String errandId, final String applicationMonth, final Integer normId, final String normType) {
		final var header = headerRepository.findById(errandId).orElseGet(() -> FaNormberakningDraftEntity.create().withErrandId(errandId));
		ofNullable(applicationMonth).filter(StringUtils::hasText).ifPresent(header::setApplicationMonth);
		ofNullable(normId).ifPresent(header::setNormId);
		ofNullable(normType).filter(StringUtils::hasText).ifPresent(header::setNormType);
		headerRepository.save(header);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Read — the full draft view a handläggare sees in Draken.
	// ------------------------------------------------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public NormberakningDraft get(final String errandId) {
		final var header = headerRepository.findById(errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No draft normberäkning for errand"));

		final var incomes = incomeRepository.findByErrandId(errandId).stream().map(DraftService::toIncomeRow).toList();
		final var expenses = expenseRepository.findByErrandId(errandId).stream().map(DraftService::toExpenseRow).toList();
		final var persons = personRepository.findByErrandId(errandId).stream().map(DraftService::toPersonRow).toList();

		return NormberakningDraft.create()
			.withErrandId(header.getErrandId())
			.withApplicationMonth(header.getApplicationMonth())
			.withNormId(header.getNormId())
			.withNormType(header.getNormType())
			.withPersons(persons)
			.withIncomes(incomes)
			.withExpenses(expenses)
			.withIncomeSum(sum(incomes.stream().filter(row -> !row.isDeleted()).map(NormIncomeRow::getEffectiveAmount)))
			.withExpenseSum(sum(expenses.stream().filter(row -> !row.isDeleted()).map(NormExpenseRow::getEffectiveAmount)))
			.withCreated(header.getCreated())
			.withUpdated(header.getUpdated());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Handläggare path — per-row edits. Touch only handläggare columns / soft-delete; never the process columns.
	// ------------------------------------------------------------------------------------------------------------------

	@Transactional
	public NormIncomeRow addIncome(final String errandId, final NormIncomeInput input) {
		requireHeader(errandId);
		final var entity = incomeRepository.save(FaNormIncomeEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_HANDLAGGARE)
			.withTypeId(input.getTypeId()).withTypeName(input.getTypeName()).withRecipient(input.getRecipient())
			.withHandlaggareAmount(input.getHandlaggareAmount()).withHandlaggareAmountDate(input.getHandlaggareAmountDate()).withNote(input.getNote()));
		return toIncomeRow(entity);
	}

	@Transactional
	public NormIncomeRow patchIncome(final String errandId, final String rowId, final NormIncomeInput input) {
		final var entity = requireIncome(errandId, rowId);
		entity.setHandlaggareAmount(input.getHandlaggareAmount());
		entity.setHandlaggareAmountDate(input.getHandlaggareAmountDate());
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
			.withErrandId(errandId).withOrigin(ORIGIN_HANDLAGGARE)
			.withCostType(input.getCostType()).withOtherSubType(input.getOtherSubType()).withSpecification(input.getSpecification())
			.withHandlaggareAmount(input.getHandlaggareAmount()).withNote(input.getNote()));
		return toExpenseRow(entity);
	}

	@Transactional
	public NormExpenseRow patchExpense(final String errandId, final String rowId, final NormExpenseInput input) {
		final var entity = requireExpense(errandId, rowId);
		entity.setHandlaggareAmount(input.getHandlaggareAmount());
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
			.withErrandId(errandId).withOrigin(ORIGIN_HANDLAGGARE)
			.withPartyId(input.getPartyId()).withRole(input.getRole()).withName(input.getName())
			.withHandlaggareDays(input.getHandlaggareDays()).withNote(input.getNote()));
		return toPersonRow(entity);
	}

	@Transactional
	public NormPersonRow patchPerson(final String errandId, final String rowId, final NormPersonInput input) {
		final var entity = requirePerson(errandId, rowId);
		entity.setHandlaggareDays(input.getHandlaggareDays());
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
	// Commit path — the effective (live, non-deleted) rows posted to Lifecare on a beslut.
	// ------------------------------------------------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public Optional<FaNormberakningDraftEntity> header(final String errandId) {
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
		return personRepository.findByErrandId(errandId).stream().filter(row -> !row.isDeleted()).toList();
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Effective-value helpers
	// ------------------------------------------------------------------------------------------------------------------

	public static BigDecimal effectiveAmount(final BigDecimal handlaggareAmount, final BigDecimal processAmount) {
		return handlaggareAmount != null ? handlaggareAmount : processAmount;
	}

	public static Integer effectiveDays(final Integer handlaggareDays, final Integer processDays) {
		return handlaggareDays != null ? handlaggareDays : processDays;
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Internals
	// ------------------------------------------------------------------------------------------------------------------

	private void requireHeader(final String errandId) {
		if (!headerRepository.existsById(errandId)) {
			throw Problem.valueOf(NOT_FOUND, "No draft normberäkning for errand");
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

	private static <E> List<E> nullSafe(final List<E> list) {
		return ofNullable(list).orElseGet(List::of);
	}

	private static <E> java.util.function.Predicate<E> isSystem(final java.util.function.Function<E, String> originOf) {
		return entity -> ORIGIN_SYSTEM.equals(originOf.apply(entity));
	}

	private static BigDecimal sum(final java.util.stream.Stream<BigDecimal> amounts) {
		return amounts.filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// --- identity keys ---

	private static String incomeKey(final FaNormIncomeEntity e) {
		return e.getTypeId() + "|" + e.getRecipient();
	}

	private static String expenseKey(final FaNormExpenseEntity e) {
		return e.getCostType() + "|" + ofNullable(e.getOtherSubType()).orElse("") + "|" + ofNullable(e.getSpecification()).orElse("");
	}

	private static String personKey(final FaNormPersonEntity e) {
		return e.getPartyId() + "|" + e.getRole();
	}

	// --- process-column copy (refresh) ---

	private static void copyIncomeProcess(final FaNormIncomeEntity target, final FaNormIncomeEntity fresh) {
		target.setTypeName(fresh.getTypeName());
		target.setProcessAmount(fresh.getProcessAmount());
		target.setProcessAmountDate(fresh.getProcessAmountDate());
	}

	private static void copyExpenseProcess(final FaNormExpenseEntity target, final FaNormExpenseEntity fresh) {
		target.setAppliedAmount(fresh.getAppliedAmount());
		target.setProcessAmount(fresh.getProcessAmount());
	}

	private static void copyPersonProcess(final FaNormPersonEntity target, final FaNormPersonEntity fresh) {
		target.setName(fresh.getName());
		target.setProcessDays(fresh.getProcessDays());
	}

	// --- warning labels ---

	private static String incomeLabel(final FaNormIncomeEntity e) {
		return ofNullable(e.getTypeName()).orElse("Inkomst") + " (" + e.getRecipient() + ")";
	}

	private static String expenseLabel(final FaNormExpenseEntity e) {
		return ofNullable(e.getCostType()).orElse("Utgift") + ofNullable(e.getSpecification()).map(spec -> " – " + spec).orElse("");
	}

	private static String personLabel(final FaNormPersonEntity e) {
		return ofNullable(e.getName()).orElse(ofNullable(e.getPartyId()).orElse("Person")) + " (" + e.getRole() + ")";
	}

	// --- entity -> view row ---

	private static NormIncomeRow toIncomeRow(final FaNormIncomeEntity e) {
		final var effective = effectiveAmount(e.getHandlaggareAmount(), e.getProcessAmount());
		return NormIncomeRow.create()
			.withId(e.getId()).withOrigin(e.getOrigin()).withTypeId(e.getTypeId()).withTypeName(e.getTypeName()).withRecipient(e.getRecipient())
			.withProcessAmount(e.getProcessAmount()).withProcessAmountDate(e.getProcessAmountDate())
			.withHandlaggareAmount(e.getHandlaggareAmount()).withHandlaggareAmountDate(e.getHandlaggareAmountDate())
			.withEffectiveAmount(effective).withDeleted(e.isDeleted()).withNote(e.getNote())
			.withCreated(e.getCreated()).withUpdated(e.getUpdated());
	}

	private static NormExpenseRow toExpenseRow(final FaNormExpenseEntity e) {
		final var effective = effectiveAmount(e.getHandlaggareAmount(), e.getProcessAmount());
		return NormExpenseRow.create()
			.withId(e.getId()).withOrigin(e.getOrigin()).withCostType(e.getCostType()).withOtherSubType(e.getOtherSubType()).withSpecification(e.getSpecification())
			.withAppliedAmount(e.getAppliedAmount()).withProcessAmount(e.getProcessAmount()).withHandlaggareAmount(e.getHandlaggareAmount())
			.withEffectiveAmount(effective).withDeleted(e.isDeleted()).withNote(e.getNote())
			.withCreated(e.getCreated()).withUpdated(e.getUpdated());
	}

	private static NormPersonRow toPersonRow(final FaNormPersonEntity e) {
		final var effective = effectiveDays(e.getHandlaggareDays(), e.getProcessDays());
		return NormPersonRow.create()
			.withId(e.getId()).withOrigin(e.getOrigin()).withPartyId(e.getPartyId()).withRole(e.getRole()).withName(e.getName())
			.withProcessDays(e.getProcessDays()).withHandlaggareDays(e.getHandlaggareDays()).withEffectiveDays(effective)
			.withDeleted(e.isDeleted()).withNote(e.getNote()).withCreated(e.getCreated()).withUpdated(e.getUpdated());
	}
}
