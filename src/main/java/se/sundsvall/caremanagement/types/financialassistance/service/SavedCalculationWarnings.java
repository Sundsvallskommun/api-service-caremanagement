package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationExpenseView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationIncomeView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaWarningEntity;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

/**
 * Which of the draft warnings the caseworker has since dealt with in the normberäkning saved in Lifecare. Once the
 * calculation is saved the draft is frozen, so the draft refresh can no longer close them; this checks each against the
 * calculation instead. Only warnings that name something the calculation shows are judged — an income or expense type,
 * a household member by name, a type entered twice — and a warning is closed only on a positive match. Anything that
 * cannot be told from the calculation (an income with no Lifecare type, a norm chosen from the application) is left
 * for the caseworker to close. No I/O.
 */
final class SavedCalculationWarnings {

	private SavedCalculationWarnings() {}

	/** Whether the saved calculation shows that the caseworker has dealt with this warning. */
	static boolean resolved(final FaWarningEntity warning, final CalculationView calculation) {
		final var subject = subject(warning.getSourceKey());
		return switch (ofNullable(warning.getType()).orElse("")) {
			case WarningService.TYPE_NEW_INCOME -> hasText(subject) && incomeRows(calculation, subject) > 0;
			case WarningService.TYPE_INCOME_DROPPED -> hasText(subject) && (incomeRows(calculation, subject) == 0);
			case WarningService.TYPE_NEW_EXPENSE -> hasText(subject) && hasExpense(calculation, expenseType(subject));
			case WarningService.TYPE_NEW_PERSON -> hasText(subject) && hasPerson(calculation, subject);
			case WarningService.TYPE_INCOME_DUPLICATED -> duplicatedType(warning.getMessage()).filter(type -> incomeRows(calculation, type) <= 1).isPresent();
			default -> false;
		};
	}

	/** The name a draft warning's source key carries — the row label up to its qualifier. */
	private static String subject(final String sourceKey) {
		return ofNullable(sourceKey).map(String::trim).orElse("");
	}

	/** An expense label is "type – specification"; the calculation lists the type. */
	private static String expenseType(final String label) {
		return label.split(" – ", 2)[0].trim();
	}

	/** The income type a duplicate warning names, read back from its text. */
	static Optional<String> duplicatedType(final String message) {
		return ofNullable(message)
			.filter(text -> text.startsWith(DraftService.DUPLICATE_MESSAGE_PREFIX) && text.contains(DraftService.DUPLICATE_MESSAGE_INFIX))
			.map(text -> text.substring(DraftService.DUPLICATE_MESSAGE_PREFIX.length(), text.indexOf(DraftService.DUPLICATE_MESSAGE_INFIX)).trim())
			.filter(type -> hasText(type));
	}

	private static long incomeRows(final CalculationView calculation, final String type) {
		return orEmpty(calculation.incomes()).stream()
			.filter(Objects::nonNull)
			.filter(income -> same(income.type(), type))
			.filter(SavedCalculationWarnings::hasAmount)
			.count();
	}

	private static boolean hasAmount(final CalculationIncomeView income) {
		return nonZero(income.amountApplicant()) || nonZero(income.amountCoApplicant());
	}

	private static boolean hasExpense(final CalculationView calculation, final String type) {
		return Stream.concat(orEmpty(calculation.expenses()).stream(), orEmpty(calculation.specialExpenses()).stream())
			.filter(Objects::nonNull)
			.map(CalculationExpenseView::type)
			.anyMatch(expense -> same(expense, type));
	}

	private static boolean hasPerson(final CalculationView calculation, final String name) {
		return orEmpty(calculation.persons()).stream()
			.filter(Objects::nonNull)
			.map(CalculationPersonView::name)
			.anyMatch(person -> same(person, name));
	}

	private static boolean same(final String first, final String second) {
		return (first != null) && (second != null) && first.trim().toLowerCase(Locale.ROOT).equals(second.trim().toLowerCase(Locale.ROOT));
	}

	private static boolean nonZero(final BigDecimal amount) {
		return (amount != null) && (amount.signum() != 0);
	}

	private static <T> List<T> orEmpty(final List<T> list) {
		return ofNullable(list).orElseGet(List::of);
	}
}
