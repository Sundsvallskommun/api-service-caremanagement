package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationExpensePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationInvestigationDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationNormDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationPersonPostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationServiceDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationSpecialExpensePostDTO;
import generated.se.sundsvall.lifecarefc.PostCalculationBodyRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import se.sundsvall.caremanagement.lifecare.service.model.NormberakningHeader;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Optional.ofNullable;

/**
 * Assembles the full FC {@link PostCalculationBodyRequest} for an SSBTEK-driven normberäkning by combining the FC
 * calculation proposal (the service / investigation / norm / aktualisering links FC offers for the person) with the
 * prepared income rows and the application month.
 *
 * <p>
 * Sprint defaults where the proposal offers a choice: the first service, investigation and (when mandatory)
 * aktualisering are taken, and the norm covering the application month — falling back to the first. The calculation
 * spans the application month. Expenses are left to the handläggare and household size to FC (left unset → FC derives
 * it
 * from the proposal's household). These selections are intentionally simple and isolated here so they are easy to
 * refine
 * once real FC proposals are available.
 */
public final class NormberakningAssembler {

	private NormberakningAssembler() {}

	/**
	 * Build the FC normberäkning body for one applicant and application month.
	 *
	 * @param  applicantPersonId  the applicant's personnummer (the FC calculation owner)
	 * @param  proposal           the FC calculation proposal supplying the link ids; may be {@code null}
	 * @param  calculationIncomes the prepared FC income rows; may be {@code null}
	 * @param  applicationMonth   the month the application concerns
	 * @return                    the assembled {@link PostCalculationBodyRequest}
	 */
	public static PostCalculationBodyRequest assemble(
		final String applicantPersonId,
		final PersonBasedCalculationProposalDTO proposal,
		final List<PersonBasedCalculationIncomePostDTO> calculationIncomes,
		final YearMonth applicationMonth) {

		final var monthStart = applicationMonth.atDay(1);
		final var body = new PostCalculationBodyRequest()
			.personId(applicantPersonId)
			.calculationDate(monthStart.format(ISO_LOCAL_DATE))
			.calculationFromDate(monthStart.format(ISO_LOCAL_DATE))
			.calculationToDate(applicationMonth.atEndOfMonth().format(ISO_LOCAL_DATE))
			.calculationIncomes(ofNullable(calculationIncomes).orElseGet(List::of));

		ofNullable(proposal).ifPresent(p -> {
			firstServiceId(p).ifPresent(body::serviceId);
			firstInvestigationId(p).ifPresent(body::investigationId);
			normIdForMonth(p, monthStart).ifPresent(body::normId);
			mandatoryAktualiseringId(p).ifPresent(body::aktualiseringId);
		});

		return body;
	}

	/**
	 * Build the full three-section FC normberäkning body — incomes (subtracted), expenses (added) and the household
	 * persons (the norm base) — for one applicant and application month. Reuses the income + proposal-link selection of
	 * {@link #assemble(String, PersonBasedCalculationProposalDTO, List, YearMonth)}; adds the expenses and persons and,
	 * when given, overrides the proposal-selected norm with the one chosen on the draft.
	 *
	 * @param  applicantPersonId  the applicant's personnummer (the FC calculation owner)
	 * @param  proposal           the FC calculation proposal supplying the link ids; may be {@code null}
	 * @param  calculationIncomes the effective FC income rows; may be {@code null}
	 * @param  expenses           the effective FC expense rows; may be {@code null}
	 * @param  persons            the household person rows; may be {@code null}
	 * @param  normIdOverride     the norm chosen on the draft, overriding the proposal selection; may be {@code null}
	 * @param  applicationMonth   the month the application concerns
	 * @return                    the assembled {@link PostCalculationBodyRequest}
	 */
	public static PostCalculationBodyRequest assemble(
		final String applicantPersonId,
		final PersonBasedCalculationProposalDTO proposal,
		final List<PersonBasedCalculationIncomePostDTO> calculationIncomes,
		final List<PersonBasedCalculationExpensePostDTO> expenses,
		final List<PersonBasedCalculationSpecialExpensePostDTO> specialExpenses,
		final List<PersonBasedCalculationPersonPostDTO> persons,
		final NormberakningHeader header,
		final YearMonth applicationMonth) {

		final var body = assemble(applicantPersonId, proposal, calculationIncomes, applicationMonth);
		ofNullable(expenses).ifPresent(body::calculationExpenses);
		ofNullable(specialExpenses).ifPresent(body::calculationSpecialExpenses);
		ofNullable(persons).ifPresent(body::calculationPersons);
		ofNullable(header).ifPresent(h -> applyHeader(body, h));
		return body;
	}

	/**
	 * Apply the draft header onto the body — the chosen norm overrides the proposal selection, dates + household when set.
	 */
	private static void applyHeader(final PostCalculationBodyRequest body, final NormberakningHeader header) {
		ofNullable(header.normId()).ifPresent(body::normId);
		ofNullable(header.calculationFromDate()).map(date -> date.format(ISO_LOCAL_DATE)).ifPresent(body::calculationFromDate);
		ofNullable(header.calculationToDate()).map(date -> date.format(ISO_LOCAL_DATE)).ifPresent(body::calculationToDate);
		ofNullable(header.calculationDate()).map(date -> date.format(ISO_LOCAL_DATE)).ifPresent(body::calculationDate);
		ofNullable(header.hasCustomHouseholdSize()).ifPresent(body::hasCustomHouseholdSize);
		ofNullable(header.householdSize()).ifPresent(body::householdSize);
	}

	/** The norm id the proposal offers for the application month (the window covering it, else the first), or empty. */
	public static Optional<Integer> selectNormId(final PersonBasedCalculationProposalDTO proposal, final YearMonth applicationMonth) {
		return ofNullable(proposal).flatMap(p -> normIdForMonth(p, applicationMonth.atDay(1)));
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

	/** The norm whose [fromDate, toDate] window covers the application month, falling back to the first offered norm. */
	private static Optional<Integer> normIdForMonth(final PersonBasedCalculationProposalDTO proposal, final LocalDate monthStart) {
		final var norms = ofNullable(proposal.getNorms()).orElseGet(List::of);
		return norms.stream()
			.filter(norm -> covers(norm, monthStart))
			.map(PersonBasedCalculationNormDTO::getId)
			.filter(Objects::nonNull)
			.findFirst()
			.or(() -> norms.stream()
				.map(PersonBasedCalculationNormDTO::getId)
				.filter(Objects::nonNull)
				.findFirst());
	}

	/** Only link an aktualisering when FC says one is mandatory; then take the first offered. */
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

	/** FC dates are ISO calendar dates, some with a trailing time/offset, so parse the leading {@code yyyy-MM-dd}. */
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
