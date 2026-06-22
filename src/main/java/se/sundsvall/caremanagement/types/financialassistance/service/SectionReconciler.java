package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The calculation draft merge — the one place the process-vs-caseworker ownership invariant is enforced. On every
 * daily prepare each section ({@code persons}, {@code incomes}, {@code expenses}) is reconciled against the freshly
 * computed process rows:
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
 * It is deliberately storage-agnostic — the caller supplies how to read the key, copy the process columns, label a row
 * and persist it — so the same logic serves all three sections and is unit-testable without a database.
 */
final class SectionReconciler {

	private SectionReconciler() {}

	/**
	 * The outcome of a section reconcile: the labels of rows newly added by the process and of system rows that
	 * disappeared.
	 */
	record Diff(List<String> added, List<String> dropped) {}

	/**
	 * Reconcile one section's fresh process rows against the existing rows.
	 *
	 * @param  existing        the rows currently stored for the section (system + caseworker, deleted + live)
	 * @param  fresh           the freshly computed process rows (each already stamped {@code origin = SYSTEM} + errandId)
	 * @param  keyOf           the section's identity key (what makes two rows "the same row")
	 * @param  isSystem        whether a row was created by the process (only these are matched/refreshed/dropped)
	 * @param  copyProcessInto copies the process columns from the fresh row (2nd arg) into the stored row (1st arg)
	 * @param  labelOf         a human label for a row, used in the added/dropped warnings
	 * @param  persist         persists a row (insert for new fresh rows, update for refreshed ones)
	 * @return                 the added + dropped labels
	 */
	static <E> Diff reconcile(
		final List<E> existing,
		final List<E> fresh,
		final Function<E, String> keyOf,
		final Predicate<E> isSystem,
		final BiConsumer<E, E> copyProcessInto,
		final Function<E, String> labelOf,
		final Consumer<E> persist) {

		final var systemByKey = new LinkedHashMap<String, E>();
		for (final var row : existing) {
			if (isSystem.test(row)) {
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
}
