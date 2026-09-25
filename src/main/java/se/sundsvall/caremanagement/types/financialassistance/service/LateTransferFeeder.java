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
 * Warns for the comparison-period incomes that the previous month's normberäkning did not contain and this month's
 * transfer therefore picks up — verksamhetens regelverk (revision 2026-09-22): <em>”Finns inkomster i
 * jämförelseperioden som inte är överförda? Nej = gör ingenting. Ja = för över till normberäkning och generera
 * varning”</em>.
 *
 * <p>
 * This is the one rule in the whole regelverk that <em>both</em> changes the normberäkning and warns about it, and
 * that is the reason it exists. The transfer itself has been in place all along — {@code withoutAlreadyTransferred}
 * keeps a comparison-period income precisely when last month's calculation had no income of its type — but it moved
 * money silently. The warning makes the move visible so the handläggare can disagree with it.
 * </p>
 *
 * <p>
 * The set is not re-derived here: {@code CalculationService.lateTransferredComparisonIncomes} runs the same filter the
 * transfer runs, so the warning cannot drift from what was actually transferred.
 * </p>
 *
 * <p>
 * Distinct from {@link IncomeChangeFeeder}, which can name the same income type as new since the previous
 * normberäkning. The two overlap by design and say different things: this one that money moved into the calculation
 * from the comparison period, that one that the type's amount differs from what was counted last time.
 * </p>
 *
 * <p>
 * One warning per benefit, keyed on the benefit, so the daily reconcile refreshes it while the income is still being
 * carried and closes it once the month rolls on and the income is no longer a late arrival.
 * </p>
 */
@Service
public class LateTransferFeeder {

	static final String MESSAGE_TEMPLATE = "%s i SSBTEK från föregående månad var inte överförd till normberäkningen men har nu förts över – kontrollera om den ska vara med i normberäkningen";

	/**
	 * The late-transfer warnings for the comparison-period incomes being transferred this month.
	 *
	 * @param  lateTransferred the comparison-period incomes the previous calculation did not contain
	 * @return                 the warnings, folded into the daily prepare's reconcile set
	 */
	public List<WarningService.WarningInput> lateTransferWarnings(final List<ClassifiedIncome> lateTransferred) {
		return ofNullable(lateTransferred).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.map(ClassifiedIncome::income)
			.filter(Objects::nonNull)
			.map(SsbtekIncome::benefit)
			.filter(benefit -> !normalize(benefit).isEmpty())
			// Several payments of the same benefit are one fact for the handläggare, and the name is spelled as SSBTEK
			// spells it — the warning has to be recognisable in the answer it came from.
			.collect(toMap(LateTransferFeeder::normalize, benefit -> benefit, (first, duplicate) -> first, LinkedHashMap::new))
			.values().stream()
			.map(benefit -> new WarningService.WarningInput(WarningService.TYPE_INCOME_TRANSFERRED_LATE,
				normalize(benefit), MESSAGE_TEMPLATE.formatted(benefit)))
			.toList();
	}

	private static String normalize(final String value) {
		return ofNullable(value).map(text -> text.trim().toLowerCase(Locale.ROOT)).orElse("");
	}
}
