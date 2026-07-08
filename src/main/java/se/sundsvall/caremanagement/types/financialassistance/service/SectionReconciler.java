package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormExpenseRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormIncomeRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormPersonRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_SYSTEM;

/**
 * The calculation draft merge — the one place the process-vs-caseworker ownership invariant is enforced. On every daily
 * prepare each section is reconciled against the freshly computed process rows via a dedicated method
 * ({@link #reconcilePersons}, {@link #reconcileIncomes}, {@link #reconcileExpenses}):
 *
 * <ul>
 * <li>a fresh row matching an existing <em>system</em> row (by the section's identity key) refreshes only its process
 * columns — the caseworker's value, the note and the soft-delete flag are left untouched, so a soft-deleted row keeps
 * its fresh process value but is never resurrected;</li>
 * <li>a fresh row with no match is inserted as a new system row and reported as added (the caller raises a NEW_*
 * warning);</li>
 * <li>an existing system row no longer in the fresh set is kept and reported as dropped (the caller raises a "no longer
 * reported" warning) — never auto-deleted;</li>
 * <li>caseworker-added rows are never matched, refreshed or dropped by the process.</li>
 * </ul>
 *
 * The three public methods are concrete per section (identity key, process-column copy and label all spelled out next
 * to the merge); the shared {@link #merge} carries the invariant once for all three.
 */
@Component
class SectionReconciler {

	private final FaNormPersonRepository personRepository;
	private final FaNormIncomeRepository incomeRepository;
	private final FaNormExpenseRepository expenseRepository;

	SectionReconciler(final FaNormPersonRepository personRepository, final FaNormIncomeRepository incomeRepository, final FaNormExpenseRepository expenseRepository) {
		this.personRepository = personRepository;
		this.incomeRepository = incomeRepository;
		this.expenseRepository = expenseRepository;
	}

	Diff reconcilePersons(final String errandId, final List<FaNormPersonEntity> fresh) {
		final var saver = positioningSaver(personRepository.nextPositionForErrand(errandId), FaNormPersonEntity::getPosition, FaNormPersonEntity::setPosition, personRepository::save);
		return merge(personRepository.findByErrandId(errandId), nullSafe(fresh),
			SectionReconciler::personKey, FaNormPersonEntity::getOrigin, SectionReconciler::copyPersonProcess, SectionReconciler::personLabel, saver);
	}

	Diff reconcileIncomes(final String errandId, final List<FaNormIncomeEntity> fresh) {
		final var saver = positioningSaver(incomeRepository.nextPositionForErrand(errandId), FaNormIncomeEntity::getPosition, FaNormIncomeEntity::setPosition, incomeRepository::save);
		return merge(incomeRepository.findByErrandId(errandId), nullSafe(fresh),
			SectionReconciler::incomeKey, FaNormIncomeEntity::getOrigin, SectionReconciler::copyIncomeProcess, SectionReconciler::incomeLabel, saver);
	}

	Diff reconcileExpenses(final String errandId, final List<FaNormExpenseEntity> fresh) {
		final var saver = positioningSaver(expenseRepository.nextPositionForErrand(errandId), FaNormExpenseEntity::getPosition, FaNormExpenseEntity::setPosition, expenseRepository::save);
		return merge(expenseRepository.findByErrandId(errandId), nullSafe(fresh),
			SectionReconciler::expenseKey, FaNormExpenseEntity::getOrigin, SectionReconciler::copyExpenseProcess, SectionReconciler::expenseLabel, saver);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// The shared merge invariant — an implementation detail behind the three concrete methods above. New rows inserted
	// by the reconcile get the next stable position appended after the existing rows (via the saver); refreshed rows keep
	// the position they already have.
	// ------------------------------------------------------------------------------------------------------------------

	private static <E> Diff merge(
		final List<E> existing,
		final List<E> fresh,
		final Function<E, String> keyOf,
		final Function<E, String> originOf,
		final BiConsumer<E, E> copyProcessInto,
		final Function<E, String> labelOf,
		final Consumer<E> persist) {

		final var systemByKey = new LinkedHashMap<String, E>();
		for (final var row : existing) {
			if (ORIGIN_SYSTEM.equals(originOf.apply(row))) {
				systemByKey.putIfAbsent(keyOf.apply(row), row);
			}
		}

		final var freshKeys = new LinkedHashSet<String>();
		final var added = new ArrayList<String>();
		for (final var freshRow : fresh) {
			final var key = keyOf.apply(freshRow);
			freshKeys.add(key);
			final var match = systemByKey.get(key);
			if (match != null) {
				copyProcessInto.accept(match, freshRow); // refresh process columns only — caseworker value + deleted untouched
				persist.accept(match);
			} else {
				persist.accept(freshRow); // a genuinely new process row
				added.add(labelOf.apply(freshRow));
			}
		}

		final var dropped = systemByKey.entrySet().stream()
			.filter(entry -> !freshKeys.contains(entry.getKey()))
			.map(entry -> labelOf.apply(entry.getValue()))
			.toList();

		return new Diff(added, dropped);
	}

	/**
	 * A save consumer that stamps a stable position on any row that doesn't have one yet, handing out consecutive
	 * positions from {@code start} (the next free position for the errand) so new rows append after the existing ones.
	 */
	// S4276: getPosition must return a nullable Integer (null = "no position yet", the condition this method keys on);
	// ToIntFunction's primitive int cannot represent that null, so the general Function is required here.
	@SuppressWarnings("java:S4276")
	private static <E> Consumer<E> positioningSaver(final int start, final Function<E, Integer> getPosition, final ObjIntConsumer<E> setPosition, final Consumer<E> save) {
		final var next = new AtomicInteger(start);
		return entity -> {
			if (getPosition.apply(entity) == null) {
				setPosition.accept(entity, next.getAndIncrement());
			}
			save.accept(entity);
		};
	}

	private static <E> List<E> nullSafe(final List<E> list) {
		return ofNullable(list).orElseGet(List::of);
	}

	// --- identity keys ---

	private static String incomeKey(final FaNormIncomeEntity e) {
		return String.valueOf(e.getTypeId());
	}

	private static String expenseKey(final FaNormExpenseEntity e) {
		return e.getCostType() + "|" + ofNullable(e.getOtherSubType()).orElse("") + "|" + ofNullable(e.getSpecification()).orElse("") + "|" + ofNullable(e.getBucket()).orElse("");
	}

	private static String personKey(final FaNormPersonEntity e) {
		// partyId is the identity when present (applicants always carry one); children may have none, so fall back to the
		// name. Without the fallback several partyId-less children collapse onto one "null|<role>" key and only the last
		// listed is counted/reconciled.
		final var identity = ofNullable(e.getPartyId()).filter(StringUtils::hasText).orElseGet(e::getName);
		return identity + "|" + e.getRole();
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

	/**
	 * The outcome of a section reconciled: the labels of rows newly added by the process and of system rows that
	 * disappeared.
	 */
	record Diff(List<String> added, List<String> dropped) {}
}
