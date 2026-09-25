package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicationIncome;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

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

	static final String APPLICATION_MESSAGE_TEMPLATE = "%s%s i ansökan saknar inkomsttyp i Lifecare och har inte förts över till normberäkningen – för in den för hand";

	private static final String CO_APPLICANT_SUFFIX = " (medsökande)";
	private static final String RECIPIENT_CO_APPLICANT = "CO_APPLICANT";

	/**
	 * The warnings for the incomes the draft could not take.
	 *
	 * @param  untransferable the transferable incomes no Lifecare income type matched
	 * @param  childNames     the household children's first names by partyId, to name a child's income
	 * @return                the warnings, folded into the daily prepare's reconcile set
	 */
	public List<WarningService.WarningInput> untransferableIncomeWarnings(final List<ClassifiedIncome> untransferable, final Map<String, String> childNames) {
		return ofNullable(untransferable).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(classified -> classified.income() != null)
			.filter(classified -> !normalize(classified.income().benefit()).isEmpty())
			// Several payments of the same benefit to the same person are one fact for the handläggare.
			.collect(toMap(UntransferableIncomeFeeder::sourceKey, classified -> classified, (first, duplicate) -> first, LinkedHashMap::new))
			.entrySet().stream()
			.map(entry -> new WarningService.WarningInput(WarningService.TYPE_INCOME_NOT_TRANSFERABLE, entry.getKey(), message(entry.getValue(), childNames)))
			.toList();
	}

	private static String message(final ClassifiedIncome classified, final Map<String, String> childNames) {
		return MESSAGE_TEMPLATE.formatted(classified.income().benefit(), personSuffix(classified.income(), childNames), classified.calculation());
	}

	/** Keyed per person: a child's income also carries the child's partyId, so two children's warnings stay apart. */
	private static String sourceKey(final ClassifiedIncome classified) {
		final var income = classified.income();
		final var child = ofNullable(income.partyId()).filter(partyId -> income.role() == ApplicantRole.CHILD).map(partyId -> "|" + partyId).orElse("");
		return normalize(income.benefit()) + ofNullable(income.role()).map(role -> "|" + role.name()).orElse("") + child;
	}

	/**
	 * The warnings for the incomes declared in the application that the draft could not take — one per income type and
	 * person, keyed apart from the SSBTEK ones so the two never close each other.
	 *
	 * @param  untransferable the declared incomes no Lifecare income type matched
	 * @return                the warnings, folded into the daily prepare's reconcile set
	 */
	public List<WarningService.WarningInput> untransferableApplicationIncomeWarnings(final List<ApplicationIncome> untransferable) {
		return ofNullable(untransferable).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.collect(toMap(UntransferableIncomeFeeder::applicationSourceKey, income -> income, (first, duplicate) -> first, LinkedHashMap::new))
			.entrySet().stream()
			.map(entry -> new WarningService.WarningInput(WarningService.TYPE_INCOME_NOT_TRANSFERABLE, entry.getKey(), applicationMessage(entry.getValue())))
			.toList();
	}

	private static String applicationSourceKey(final ApplicationIncome income) {
		return "application|" + normalize(income.incomeType()) + "|" + applicationRecipient(income);
	}

	private static String applicationMessage(final ApplicationIncome income) {
		final var suffix = ofNullable(income.recipient()).filter(RECIPIENT_CO_APPLICANT::equals).map(_ -> CO_APPLICANT_SUFFIX).orElse("");
		return APPLICATION_MESSAGE_TEMPLATE.formatted(ofNullable(income.label()).orElse(income.incomeType()), suffix);
	}

	private static String applicationRecipient(final ApplicationIncome income) {
		return ofNullable(income.recipient()).filter(RECIPIENT_CO_APPLICANT::equals).orElse("APPLICANT").toLowerCase(Locale.ROOT);
	}

	private static String personSuffix(final SsbtekIncome income, final Map<String, String> childNames) {
		if (income.role() == ApplicantRole.CO_APPLICANT) {
			return CO_APPLICANT_SUFFIX;
		}
		return income.childSuffix(childNames);
	}

	private static String normalize(final String value) {
		return ofNullable(value).map(text -> text.trim().toLowerCase(Locale.ROOT)).orElse("");
	}
}
