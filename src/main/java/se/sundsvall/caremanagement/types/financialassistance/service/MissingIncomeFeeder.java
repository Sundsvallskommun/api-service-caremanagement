package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * Warns for the incomes SSBTEK reported in the comparison period but not in the control period — verksamhetens
 * "föregående månad = facit": <em>"Varna för allt som fanns föregående månad men inte finns nu"</em>.
 *
 * <p>
 * The judgement is already made in the engine. {@code IncomeRulesEvaluator} carries a comparison-period income
 * forward only when the same benefit has no control-period income at all, and marks it {@code jamforelseperiod}, so
 * the flag on a classified income <em>is</em> the answer to "was this benefit reported last month and not now". This
 * feeder only turns it into the handläggare's sentence.
 * </p>
 *
 * <p>
 * Distinct from {@link WarningService#TYPE_INCOME_MISSING_VS_PREVIOUS_CALCULATION}, which compares against the
 * previous <em>normberäkning</em> in Lifecare — what a caseworker decided to transfer last month. This one compares
 * against the previous <em>SSBTEK answer</em> — what the agencies reported. The two disagree exactly when a
 * caseworker overrode the raw data, which is the case worth telling them about.
 * </p>
 *
 * <p>
 * One warning per benefit, keyed on the benefit itself, so the daily reconcile refreshes it while the income is still
 * missing and auto-closes it the day the income comes back.
 * </p>
 */
@Service
public class MissingIncomeFeeder {

	static final String MESSAGE_TEMPLATE = "%s fanns föregående månad i SSBTEK men saknas nu";

	/**
	 * The missing-income warnings for this month's classified incomes.
	 *
	 * @param  classified the classified incomes as the engine sent them, both periods
	 * @return            the warnings, folded into the daily prepare's reconcile set
	 */
	public List<WarningService.WarningInput> missingIncomeWarnings(final List<ClassifiedIncome> classified) {
		return ofNullable(classified).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(ClassifiedIncome::isFromComparisonPeriod)
			.map(ClassifiedIncome::income)
			.filter(Objects::nonNull)
			.map(SsbtekIncome::benefit)
			.filter(benefit -> !normalize(benefit).isEmpty())
			// Several payments of the same benefit are one fact for the handläggare, and the name is spelled as SSBTEK
			// spells it — the warning has to be recognisable in the answer it came from.
			.collect(toMap(MissingIncomeFeeder::normalize, benefit -> benefit, (first, duplicate) -> first, LinkedHashMap::new))
			.values().stream()
			.map(benefit -> new WarningService.WarningInput(WarningService.TYPE_INCOME_MISSING_PREVIOUS_PERIOD,
				normalize(benefit), MESSAGE_TEMPLATE.formatted(benefit)))
			.toList();
	}

	private static String normalize(final String value) {
		return ofNullable(value).map(text -> text.trim().toLowerCase(Locale.ROOT)).orElse("");
	}
}
