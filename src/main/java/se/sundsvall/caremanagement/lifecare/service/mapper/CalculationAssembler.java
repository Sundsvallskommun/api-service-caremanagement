package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationInvestigationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationNormDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PostCalculationBodyRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.integration.FamilyCareDates;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationHeader;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationSections;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.lifecare.integration.FamilyCareDates.startOfDay;
import static se.sundsvall.caremanagement.lifecare.service.mapper.MapperUtil.normalize;

/**
 * Assembles the full FamilyCare {@link PostCalculationBodyRequest} for an SSBTEK-driven calculation by combining the
 * FamilyCare calculation proposal (the service / investigation / norm / actualisation links FamilyCare offers for the
 * person) with the prepared income rows and the application month.
 *
 * <p>
 * Sprint defaults where the proposal offers a choice: the first service, investigation and (when mandatory)
 * actualisation are taken, and the norm covering the application month — falling back to the first. The calculation
 * spans the application month. Expenses are left to the caseworker and household size to FamilyCare (left unset →
 * FamilyCare derives it from the proposal's household). These selections are intentionally simple and isolated here so
 * they are easy to refine once real FamilyCare proposals are available.
 */
public final class CalculationAssembler {

	private CalculationAssembler() {}

	/**
	 * Build the FamilyCare calculation body for one applicant and application month.
	 *
	 * @param  applicantPersonId  the applicant's personnummer (the FamilyCare calculation owner)
	 * @param  proposal           the FamilyCare calculation proposal supplying the link ids; may be {@code null}
	 * @param  calculationIncomes the prepared FamilyCare income rows; may be {@code null}
	 * @param  applicationMonth   the month the application concerns
	 * @return                    the assembled {@link PostCalculationBodyRequest}
	 */
	public static PostCalculationBodyRequest assemble(
		final String applicantPersonId,
		final PersonBasedCalculationProposalDTO proposal,
		final List<PersonBasedCalculationIncomePostDTO> calculationIncomes,
		final YearMonth applicationMonth,
		final List<String> normNames) {

		final var monthStart = applicationMonth.atDay(1);
		final var body = new PostCalculationBodyRequest()
			.personId(applicantPersonId)
			.calculationDate(startOfDay(monthStart))
			.calculationFromDate(startOfDay(monthStart))
			.calculationToDate(startOfDay(applicationMonth.atEndOfMonth()))
			.calculationIncomes(ofNullable(calculationIncomes).orElseGet(List::of));

		ofNullable(proposal).ifPresent(p -> {
			firstServiceId(p).ifPresent(body::serviceId);
			firstInvestigationId(p).ifPresent(body::investigationId);
			normIdForMonth(p, monthStart, normNames).ifPresent(body::normId);
			mandatoryAktualiseringId(p).ifPresent(body::aktualiseringId);
		});

		return body;
	}

	/**
	 * Build the full three-section FamilyCare calculation body — incomes (subtracted), expenses (added) and the
	 * household persons (the norm base) — for one applicant and application month. Reuses the income + proposal-link
	 * selection of {@link #assemble(String, PersonBasedCalculationProposalDTO, List, YearMonth)}; adds the expenses and
	 * persons and, when given, overrides the proposal-selected norm with the one chosen on the draft header.
	 *
	 * @param  applicantPersonId the applicant's personnummer (the FamilyCare calculation owner)
	 * @param  proposal          the FamilyCare calculation proposal supplying the link ids; may be {@code null}
	 * @param  sections          the income/expense/special-expense/person rows + draft header; fields may be {@code null}
	 * @param  applicationMonth  the month the application concerns
	 * @return                   the assembled {@link PostCalculationBodyRequest}
	 */
	public static PostCalculationBodyRequest assemble(
		final String applicantPersonId,
		final PersonBasedCalculationProposalDTO proposal,
		final CalculationSections sections,
		final YearMonth applicationMonth,
		final List<String> normNames) {

		final var body = assemble(applicantPersonId, proposal, sections.incomes(), applicationMonth, normNames);
		ofNullable(sections.expenses()).ifPresent(body::calculationExpenses);
		ofNullable(sections.specialExpenses()).ifPresent(body::calculationSpecialExpenses);
		ofNullable(sections.persons()).ifPresent(body::calculationPersons);
		ofNullable(sections.header()).ifPresent(h -> applyHeader(body, h));
		return body;
	}

	/**
	 * Apply the draft header onto the body — the chosen norm overrides the proposal selection, dates + household when
	 * set.
	 */
	private static void applyHeader(final PostCalculationBodyRequest body, final CalculationHeader header) {
		ofNullable(header.normId()).ifPresent(body::normId);
		ofNullable(header.calculationFromDate()).map(FamilyCareDates::startOfDay).ifPresent(body::calculationFromDate);
		ofNullable(header.calculationToDate()).map(FamilyCareDates::startOfDay).ifPresent(body::calculationToDate);
		ofNullable(header.calculationDate()).map(FamilyCareDates::startOfDay).ifPresent(body::calculationDate);
		ofNullable(header.hasCustomHouseholdSize()).ifPresent(body::hasCustomHouseholdSize);
		ofNullable(header.householdSize()).ifPresent(body::householdSize);
	}

	/**
	 * The norm id for the application month — the one the application's {@code normType} names among those covering
	 * the month; see {@link #normIdForMonth}.
	 */
	public static Optional<Integer> selectNormId(final PersonBasedCalculationProposalDTO proposal, final YearMonth applicationMonth,
		final List<String> normNames) {
		return ofNullable(proposal).flatMap(p -> normIdForMonth(p, applicationMonth.atDay(1), normNames));
	}

	private static Optional<Integer> firstServiceId(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal.getServices()).orElseGet(List::of).stream()
			.map(PersonBasedCalculationServiceDTO::getId)
			.filter(Objects::nonNull)
			.findFirst();
	}

	private static Optional<Integer> firstInvestigationId(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal.getInvestigations()).orElseGet(List::of).stream()
			.map(PersonBasedCalculationInvestigationDTO::getId)
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

	/** Only link an actualisation when FamilyCare says one is mandatory; then take the first offered. */
	private static Optional<Integer> mandatoryAktualiseringId(final PersonBasedCalculationProposalDTO proposal) {
		if (!Boolean.TRUE.equals(proposal.getAktualiseringMandatory())) {
			return Optional.empty();
		}
		return ofNullable(proposal.getAktualiserings()).orElseGet(List::of).stream()
			.map(PersonBasedCalculationAktualiseringDTO::getId)
			.filter(Objects::nonNull)
			.findFirst();
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
