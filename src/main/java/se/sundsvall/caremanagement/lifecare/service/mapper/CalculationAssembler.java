package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationNormDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.util.StringUtils;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.lifecare.service.mapper.MapperUtil.normalize;

/**
 * Chooses the norm for a calculation month from the FamilyCare calculation proposal — the norm catalogue the proposal
 * offers the person, matched by name against the month's window. careM used to assemble and post the whole
 * calculation body from here; that write moved to Draken's BFF, which owns the normberäkning in Lifecare, and only the
 * norm choice for careM's draft remains.
 */
public final class CalculationAssembler {

	private CalculationAssembler() {}

	/**
	 * The norm id for the application month — the one the application's {@code normType} names among those covering
	 * the month; see {@link #normIdForMonth}.
	 */
	public static Optional<Integer> selectNormId(final PersonBasedCalculationProposalDTO proposal, final YearMonth applicationMonth,
		final List<String> normNames) {
		return ofNullable(proposal).flatMap(p -> normIdForMonth(p, applicationMonth.atDay(1), normNames));
	}

	/**
	 * The first norm covering the application month whose name starts with one of {@code normNames} — a strict match,
	 * empty when none does, unlike {@link #selectNormId(PersonBasedCalculationProposalDTO, YearMonth, List)}, which falls
	 * back to the first covering norm. Strict because the name comes from the previous calculation, and falling through
	 * to “the first norm that covers the month” on a miss is the coin toss that once picked Matnorm.
	 */
	public static Optional<Integer> matchingNormId(final PersonBasedCalculationProposalDTO proposal, final YearMonth applicationMonth,
		final List<String> normNames) {
		final var monthStart = applicationMonth.atDay(1);
		final var wanted = ofNullable(normNames).orElseGet(List::of).stream()
			.filter(StringUtils::hasText)
			.toList();
		if ((proposal == null) || wanted.isEmpty()) {
			return Optional.empty();
		}
		return ofNullable(proposal.getNorms()).orElseGet(List::of).stream()
			.filter(norm -> covers(norm, monthStart))
			.filter(norm -> matchesAnyLabel(norm, wanted))
			.map(PersonBasedCalculationNormDTO::getId)
			.filter(Objects::nonNull)
			.findFirst();
	}

	/**
	 * The norm to calculate against: the one the application asked for, among those whose [fromDate, toDate] window
	 * covers the application month.
	 *
	 * <p>
	 * The window alone does not choose. FamilyCare offered four norms for September 2026 — Riksnorm, Matnorm,
	 * Nettonorm and Specnorm — and every one of them covered the month, so "the first that covers" was a coin toss
	 * that landed on Matnorm. Matnorm is a reduced food norm with no row for a single-person household, and
	 * FamilyCare refused the calculation with <em>Saknar norm för angiven hushållsstorlek</em>. The application had
	 * said {@code NATIONAL_NORM} all along; it was simply never read.
	 *
	 * <p>
	 * The caller passes norm <em>names</em> rather than its own norm-type codes: this module serves every errand type
	 * and has no business knowing what {@code NATIONAL_NORM} means. Matching is on the name as a prefix of the
	 * catalogue entry ("Riksnorm" → "Riksnorm 2026"), because the catalogue names carry the year and the labels do
	 * not. A name that matches nothing selects nothing and falls through to the covering-window default.
	 */
	private static Optional<Integer> normIdForMonth(final PersonBasedCalculationProposalDTO proposal, final LocalDate monthStart,
		final List<String> normNames) {

		final var norms = ofNullable(proposal.getNorms()).orElseGet(List::of);
		final var covering = norms.stream().filter(norm -> covers(norm, monthStart)).toList();
		final var wanted = ofNullable(normNames).orElseGet(List::of).stream()
			.filter(StringUtils::hasText)
			.toList();

		return covering.stream()
			.filter(norm -> matchesAnyLabel(norm, wanted))
			.map(PersonBasedCalculationNormDTO::getId)
			.filter(Objects::nonNull)
			.findFirst()
			.or(() -> covering.stream()
				.map(PersonBasedCalculationNormDTO::getId)
				.filter(Objects::nonNull)
				.findFirst())
			.or(() -> norms.stream()
				.map(PersonBasedCalculationNormDTO::getId)
				.filter(Objects::nonNull)
				.findFirst());
	}

	/** Whether the catalogue norm's name starts with any of the requested labels, ignoring case and surrounding space. */
	private static boolean matchesAnyLabel(final PersonBasedCalculationNormDTO norm, final List<String> labels) {
		final var name = normalize(norm.getName());
		return labels.stream().map(MapperUtil::normalize).anyMatch(label -> !label.isEmpty() && name.startsWith(label));
	}

	private static boolean covers(final PersonBasedCalculationNormDTO norm, final LocalDate date) {
		final var from = parseDate(norm.getFromDate());
		final var to = parseDate(norm.getToDate());
		final var notBeforeFrom = (from == null) || !date.isBefore(from);
		final var notAfterTo = (to == null) || !date.isAfter(to);
		return notBeforeFrom && notAfterTo;
	}

	/**
	 * FamilyCare dates are ISO calendar dates, some with a trailing time/offset, so parse the leading {@code yyyy-MM-dd}.
	 */
	private static LocalDate parseDate(final String text) {
		return ofNullable(text)
			.map(String::trim)
			.filter(value -> value.length() >= 10)
			.map(value -> {
				try {
					return LocalDate.parse(value.substring(0, 10));
				} catch (final RuntimeException e) {
					return null;
				}
			})
			.orElse(null);
	}
}
