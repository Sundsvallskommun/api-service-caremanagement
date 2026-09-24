package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * Warns for the incomes the regelverk says to transfer that the draft could not take, because no Lifecare income type
 * matches their category.
 *
 * <p>
 * Before this existed such an income was dropped without a trace: the draft simply had no row for it, which the
 * handläggare reads as "the person has no such income" — and leaving an income out raises the amount granted. The set
 * comes from {@code CalculationService.untransferableIncomes}, which runs the transfer's own filter, so the warning
 * names exactly what the draft is missing.
 * </p>
 *
 * <p>
 * One warning per benefit and person, keyed on both, so the daily reconcile keeps it open while the income still has
 * nowhere to go and closes it once it is transferred or no longer reported.
 * </p>
 */
@Service
public class UntransferableIncomeFeeder {

	static final String MESSAGE_TEMPLATE = "%s%s i SSBTEK ska tas med i normberäkningen men saknar inkomsttyp i Lifecare (%s) och har inte förts över – för in den för hand";

	private static final String CO_APPLICANT_SUFFIX = " (medsökande)";

	/**
	 * The warnings for the incomes the draft could not take.
	 *
	 * @param  untransferable the transferable incomes no Lifecare income type matched
	 * @return                the warnings, folded into the daily prepare's reconcile set
	 */
	public List<WarningService.WarningInput> untransferableIncomeWarnings(final List<ClassifiedIncome> untransferable) {
		return ofNullable(untransferable).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(classified -> classified.income() != null)
			.filter(classified -> !normalize(classified.income().benefit()).isEmpty())
			// Several payments of the same benefit to the same person are one fact for the handläggare.
			.collect(toMap(UntransferableIncomeFeeder::sourceKey, classified -> classified, (first, duplicate) -> first, LinkedHashMap::new))
			.entrySet().stream()
			.map(entry -> new WarningService.WarningInput(WarningService.TYPE_INCOME_NOT_TRANSFERABLE, entry.getKey(), message(entry.getValue())))
			.toList();
	}

	private static String message(final ClassifiedIncome classified) {
		return MESSAGE_TEMPLATE.formatted(classified.income().benefit(), personSuffix(classified.income().role()), classified.calculation());
	}

	private static String sourceKey(final ClassifiedIncome classified) {
		return normalize(classified.income().benefit()) + ofNullable(classified.income().role()).map(role -> "|" + role.name()).orElse("");
	}

	private static String personSuffix(final ApplicantRole role) {
		if (role == ApplicantRole.CO_APPLICANT) {
			return CO_APPLICANT_SUFFIX;
		}
		return "";
	}

	private static String normalize(final String value) {
		return ofNullable(value).map(text -> text.trim().toLowerCase(Locale.ROOT)).orElse("");
	}
}
