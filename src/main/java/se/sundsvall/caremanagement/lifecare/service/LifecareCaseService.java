package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.CommonCalculationExpenseDTO;
import generated.se.sundsvall.lifecarefamilycare.CommonCalculationIncomeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionPersonDTO;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;
import se.sundsvall.caremanagement.lifecare.service.mapper.ExpenseTypeMapper;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;

import static java.lang.Boolean.TRUE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.StringUtils.hasText;

/**
 * Answers financial-assistance-routing questions about a person from Lifecare FamilyCare. Wraps
 * {@link LifecareFamilyCareIntegration},
 * reading
 * actualisations, decision and calculations over a lookback window ending at the reference date, and reduces them to
 * a
 * domain {@link LifecareCaseSummary}. FamilyCare's date strings (from/to periods) and generated DTOs never leave this
 * module.
 *
 * <p>
 * Calls propagate the integration's {@code BAD_GATEWAY} problem on failure — the caller decides whether to treat the
 * lookup as best-effort.
 * </p>
 */
@Service
public class LifecareCaseService {

	/** Guard against a malformed decision period (e.g. from 2000 to 2030) producing an unbounded month set. */
	private static final int MAX_MONTHS_PER_DECISION = 36;

	private final LifecareFamilyCareIntegration lifecareFamilyCareIntegration;
	private final int lookbackMonths;

	LifecareCaseService(final LifecareFamilyCareIntegration lifecareFamilyCareIntegration,
		@Value("${integration.lifecare-familycare.lookback-months:13}") final int lookbackMonths) {
		this.lifecareFamilyCareIntegration = lifecareFamilyCareIntegration;
		this.lookbackMonths = lookbackMonths;
	}

	/**
	 * Summarises the person's financial assistance footprint in FamilyCare over the lookback window ending at
	 * {@code referenceDate}.
	 *
	 * @param  personId      the person's personal identity number
	 * @param  referenceDate the date the routing is evaluated against (bounds the lookback window)
	 * @return               the distilled summary; never {@code null}
	 */
	public LifecareCaseSummary summarize(final String personId, final LocalDate referenceDate) {
		final var start = referenceDate.minusMonths(lookbackMonths).format(ISO_LOCAL_DATE);
		final var end = referenceDate.format(ISO_LOCAL_DATE);

		final var actualisations = ofNullable(lifecareFamilyCareIntegration.getActualisations(personId, start, end))
			.map(ApiPaginationCompositePersonBasedAktualiseringDTO::getResult)
			.orElseGet(List::of);

		final var decisions = ofNullable(lifecareFamilyCareIntegration.getDecisions(personId, start, end))
			.map(ApiPaginationCompositePersonBasedDecisionDTO::getResult)
			.orElseGet(List::of);

		final var calculations = ofNullable(lifecareFamilyCareIntegration.getCalculations(personId, start, end))
			.map(ApiPaginationCompositePersonBasedCalculationDTO::getResult)
			.orElseGet(List::of);

		final var hasFootprint = !actualisations.isEmpty() || !decisions.isEmpty() || !calculations.isEmpty();

		final var decisionMonths = new TreeSet<YearMonth>();
		decisions.forEach(decision -> decisionMonths.addAll(monthsCovered(decision)));

		final var latestDecision = latestDecision(decisions);

		return new LifecareCaseSummary(
			hasFootprint,
			Set.copyOf(decisionMonths),
			latestDecision.map(LifecareCaseService::periodOf).orElse(null),
			!calculations.isEmpty(),
			latestDecision.map(LifecareCaseService::hasCoApplicant).orElse(false));
	}

	/**
	 * Whether the person is flagged with protected identity in Lifecare FamilyCare — protected address (skyddad
	 * population register/retained registration) or protected registration (confidentiality marking). Propagates the
	 * integration's
	 * {@code BAD_GATEWAY} problem on failure; the caller decides whether to treat the lookup as best-effort.
	 *
	 * @param  personId the person's personal identity number
	 * @return          {@code true} when either protection flag is set; {@code false} when neither is set or FamilyCare has
	 *                  no
	 *                  record
	 */
	public boolean hasProtectedIdentity(final String personId) {
		return ofNullable(lifecareFamilyCareIntegration.getPerson(personId))
			.map(person -> TRUE.equals(person.getAddressProtection()) || TRUE.equals(person.getProtectedRegistration()))
			.orElse(false);
	}

	/**
	 * The household roster from the person's most recent calculation over the lookback window, paired with the
	 * co-applicant flagged on the most recent decision — the basis for a financial assistance renewal pre-fill. Propagates
	 * the
	 * integration's {@code BAD_GATEWAY} problem on failure; the caller decides whether to treat the lookup as best-effort.
	 *
	 * @param  personId      the applicant's personal identity number
	 * @param  referenceDate the date the lookup is evaluated against (bounds the lookback window)
	 * @return               the roster (applicant, co-applicant and the calculation members); members empty when none
	 */
	public LifecareRoster latestRoster(final String personId, final LocalDate referenceDate) {
		final var start = referenceDate.minusMonths(lookbackMonths).format(ISO_LOCAL_DATE);
		final var end = referenceDate.format(ISO_LOCAL_DATE);

		final var calculations = ofNullable(lifecareFamilyCareIntegration.getCalculations(personId, start, end))
			.map(ApiPaginationCompositePersonBasedCalculationDTO::getResult)
			.orElseGet(List::of);

		final var decisions = ofNullable(lifecareFamilyCareIntegration.getDecisions(personId, start, end))
			.map(ApiPaginationCompositePersonBasedDecisionDTO::getResult)
			.orElseGet(List::of);

		final var members = latestCalculation(calculations)
			.map(PersonBasedCalculationDTO::getCalculationPersonDTOs)
			.orElseGet(List::of).stream()
			.filter(person -> hasText(person.getPersonId()))
			.map(person -> new LifecareRoster.Member(person.getPersonId(), person.getName()))
			.toList();

		final var coApplicant = latestDecision(decisions)
			.flatMap(LifecareCaseService::coApplicantPersonId)
			.orElse(null);

		return new LifecareRoster(personId, coApplicant, members);
	}

	/**
	 * The distinct FamilyCare income-type names on the person's most recent calculation strictly before
	 * {@code applicationMonth} — the baseline for the financial assistance "all last month's values present" completeness
	 * check. Empty when
	 * there is no prior calculation. Propagates the integration's {@code BAD_GATEWAY} problem on failure; the caller
	 * decides whether to treat the lookup as best-effort.
	 *
	 * @param  personId         the applicant's personal identity number
	 * @param  applicationMonth the month being applied for; only calculations before it are considered
	 * @return                  the previous calculation's distinct income-type names, or empty
	 */
	public List<String> previousCalculationIncomeTypes(final String personId, final YearMonth applicationMonth) {
		final var referenceDate = applicationMonth.atDay(1);
		final var start = referenceDate.minusMonths(lookbackMonths).format(ISO_LOCAL_DATE);
		final var end = referenceDate.format(ISO_LOCAL_DATE);

		final var calculations = ofNullable(lifecareFamilyCareIntegration.getCalculations(personId, start, end))
			.map(ApiPaginationCompositePersonBasedCalculationDTO::getResult)
			.orElseGet(List::of).stream()
			.filter(calculation -> (periodOf(calculation) != null) && periodOf(calculation).isBefore(applicationMonth))
			.toList();

		return latestCalculation(calculations)
			.map(PersonBasedCalculationDTO::getCalculationIncomesDTOs)
			.orElseGet(List::of).stream()
			.map(CommonCalculationIncomeDTO::getType)
			.filter(StringUtils::hasText)
			.distinct()
			.toList();
	}

	/**
	 * The household on the person's most recent calculation strictly before {@code applicationMonth} — its person ids,
	 * member count and norm sum — the baseline the current application's household is compared against to warn on drift.
	 * Empty when there is no prior calculation. Propagates the integration's {@code BAD_GATEWAY} problem on failure; the
	 * caller decides whether to treat the lookup as best-effort.
	 *
	 * @param  personId         the applicant's personal identity number
	 * @param  applicationMonth the month being applied for; only calculations before it are considered
	 * @return                  the previous household (empty when none)
	 */
	public PreviousHousehold previousHousehold(final String personId, final YearMonth applicationMonth) {
		final var referenceDate = applicationMonth.atDay(1);
		final var start = referenceDate.minusMonths(lookbackMonths).format(ISO_LOCAL_DATE);
		final var end = referenceDate.format(ISO_LOCAL_DATE);

		final var calculations = ofNullable(lifecareFamilyCareIntegration.getCalculations(personId, start, end))
			.map(ApiPaginationCompositePersonBasedCalculationDTO::getResult)
			.orElseGet(List::of).stream()
			.filter(calculation -> (periodOf(calculation) != null) && periodOf(calculation).isBefore(applicationMonth))
			.toList();

		final var latest = latestCalculation(calculations);
		final var personIds = latest
			.map(PersonBasedCalculationDTO::getCalculationPersonDTOs)
			.orElseGet(List::of).stream()
			.map(PersonBasedCalculationPersonDTO::getPersonId)
			.filter(StringUtils::hasText)
			.collect(toSet());
		final var normSum = latest.map(PersonBasedCalculationDTO::getNormSum).orElse(null);
		final var housingCost = latest
			.map(PersonBasedCalculationDTO::getCalculationExpensesDTOs)
			.orElseGet(List::of).stream()
			.filter(expense -> isHousing(expense.getType()))
			.map(LifecareCaseService::expenseAmount)
			.filter(Objects::nonNull)
			.reduce(Double::sum)
			.orElse(null);

		return new PreviousHousehold(personIds, personIds.size(), normSum, housingCost);
	}

	/**
	 * The approved amount per financial assistance cost type on the person's most recent previous calculation — read from
	 * the regular
	 * (UTGIFTER) expense array and mapped back from each FamilyCare type name via {@link ExpenseTypeMapper}. Empty when
	 * there is
	 * no previous calculation. Feeds the expense rule tree's "godkänt belopp föregående månad" (best-effort: an FamilyCare
	 * name
	 * with no financial assistance cost type is skipped; the special-expense (LEVNADSKOSTNADER I ÖVRIGT) array is untyped
	 * in the FamilyCare spec
	 * and not read, so those types start without history).
	 *
	 * @param  personId         the applicant's personal identity number
	 * @param  applicationMonth the month being applied for; only calculations before it are considered
	 * @return                  approved amount keyed by financial assistance cost type (empty when none)
	 */
	public Map<String, Double> previousExpenseAmounts(final String personId, final YearMonth applicationMonth) {
		final var referenceDate = applicationMonth.atDay(1);
		final var start = referenceDate.minusMonths(lookbackMonths).format(ISO_LOCAL_DATE);
		final var end = referenceDate.format(ISO_LOCAL_DATE);

		final var calculations = ofNullable(lifecareFamilyCareIntegration.getCalculations(personId, start, end))
			.map(ApiPaginationCompositePersonBasedCalculationDTO::getResult)
			.orElseGet(List::of).stream()
			.filter(calculation -> (periodOf(calculation) != null) && periodOf(calculation).isBefore(applicationMonth))
			.toList();

		final var latest = latestCalculation(calculations);
		final var amounts = new HashMap<String, Double>();
		latest.map(PersonBasedCalculationDTO::getCalculationExpensesDTOs).orElseGet(List::of)
			.forEach(expense -> ExpenseTypeMapper.costTypeForFcName(expense.getType()).ifPresent(costType -> {
				final var amount = expenseAmount(expense);
				if (amount != null) {
					amounts.merge(costType, amount, Double::sum);
				}
			}));
		return amounts;
	}

	/** The previous housing cost — Rent/housing expense rows, matched on the FamilyCare type name (best-effort). */
	private static boolean isHousing(final String type) {
		if (type == null) {
			return false;
		}
		final var lower = type.toLowerCase();
		return lower.contains("rent") || lower.contains("housing");
	}

	/** The decided (approved) amount of an expense, falling back to the applied amount. */
	private static Double expenseAmount(final CommonCalculationExpenseDTO expense) {
		return ofNullable(expense.getApprovedAmount()).orElseGet(expense::getAppliedAmount);
	}

	/** The decision with the most recent period (to/from), used to read the current household constellation. */
	private static Optional<PersonBasedDecisionDTO> latestDecision(final List<PersonBasedDecisionDTO> decisions) {
		return decisions.stream()
			.filter(decision -> periodOf(decision) != null)
			.max(comparing(LifecareCaseService::periodOf));
	}

	/** Whether a decision included a co-applicant — a flagged participant or the scalar {@code coApplicant} field. */
	private static boolean hasCoApplicant(final PersonBasedDecisionDTO decision) {
		final var flagged = ofNullable(decision.getDecisionPersonDTOs()).orElseGet(List::of).stream()
			.filter(person -> Boolean.TRUE.equals(person.getIsCoApplicant()))
			.map(PersonBasedDecisionPersonDTO::getPersonId)
			.anyMatch(StringUtils::hasText);
		return flagged || hasText(decision.getCoApplicant());
	}

	/**
	 * The co-applicant's personal identity number on a decision — a flagged participant, falling back to the scalar field.
	 */
	private static Optional<String> coApplicantPersonId(final PersonBasedDecisionDTO decision) {
		final var flagged = ofNullable(decision.getDecisionPersonDTOs()).orElseGet(List::of).stream()
			.filter(person -> Boolean.TRUE.equals(person.getIsCoApplicant()))
			.map(PersonBasedDecisionPersonDTO::getPersonId)
			.filter(StringUtils::hasText)
			.findFirst();
		return flagged.or(() -> ofNullable(decision.getCoApplicant()).filter(StringUtils::hasText));
	}

	/** The calculation with the most recent period (to/from), whose persons form the household constellation. */
	private static Optional<PersonBasedCalculationDTO> latestCalculation(final List<PersonBasedCalculationDTO> calculations) {
		return calculations.stream()
			.filter(calculation -> periodOf(calculation) != null)
			.max(comparing(LifecareCaseService::periodOf));
	}

	/** The representative period of a calculation — its {@code toDate} month, falling back to {@code fromDate}. */
	private static YearMonth periodOf(final PersonBasedCalculationDTO calculation) {
		return toYearMonth(calculation.getToDate()).or(() -> toYearMonth(calculation.getFromDate())).orElse(null);
	}

	/** The year-months a decision's from/to period covers (bounded; a single side covers just that month). */
	private static List<YearMonth> monthsCovered(final PersonBasedDecisionDTO decision) {
		final var from = toYearMonth(decision.getFromDate());
		final var to = toYearMonth(decision.getToDate());
		if (from.isPresent() && to.isPresent()) {
			final var months = new ArrayList<YearMonth>();
			var cursor = from.get();
			final var last = to.get();
			while (!cursor.isAfter(last) && months.size() < MAX_MONTHS_PER_DECISION) {
				months.add(cursor);
				cursor = cursor.plusMonths(1);
			}
			return months;
		}
		return from.or(() -> to).map(List::of).orElseGet(List::of);
	}

	/** The representative period of a decision — its {@code toDate} month, falling back to {@code fromDate}. */
	private static YearMonth periodOf(final PersonBasedDecisionDTO decision) {
		return toYearMonth(decision.getToDate()).or(() -> toYearMonth(decision.getFromDate())).orElse(null);
	}

	/** Lenient year-month extraction from FamilyCare's date strings ("yyyy-MM-dd", "yyyy-MM", or an ISO datetime). */
	static Optional<YearMonth> toYearMonth(final String value) {
		if (!hasText(value)) {
			return Optional.empty();
		}
		final var datePart = value.trim().split("[T ]", 2)[0];
		final var parts = datePart.split("-");
		if (parts.length < 2) {
			return Optional.empty();
		}
		try {
			return Optional.of(YearMonth.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
		} catch (final NumberFormatException | DateTimeException e) {
			return Optional.empty();
		}
	}
}
