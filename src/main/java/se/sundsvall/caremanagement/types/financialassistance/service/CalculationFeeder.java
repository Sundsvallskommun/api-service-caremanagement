package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCost;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.RECIPIENT_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.RECIPIENT_CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ROLE_CHILD;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ROLE_VISITATION_CHILD;

/**
 * Builds the freshly computed process rows for the calculation sections from the errand: the income rows (one per
 * FamilyCare
 * income type, with a applicant and co-applicant side) from the operaton-classified incomes, the expense rows from the
 * application's costs — each given a process amount + bucket by the {@link ExpenseRulesService} — and the person
 * rows from the household (visitation child = part-time children). Also compares the household against the previous
 * calculation in Lifecare to produce drift warnings. Every row is stamped {@code origin = SYSTEM}; the
 * {@link DraftService} merge then refreshes only the process columns.
 */
@Service
public class CalculationFeeder {

	private static final int FULL_MONTH_DAYS = 30;
	private static final String RESIDENCE_FULL_TIME = "FULL_TIME";
	private static final String COST_TYPE_RENT = "RENT";
	private static final String CHANGE_HOUSEHOLD_SIZE = "HOUSEHOLD_SIZE";
	private static final String CHANGE_HOUSING_COST = "HOUSING_COST";
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	/** financial assistance cost type → Swedish label for caseworker-facing warning text. */
	private static final Map<String, String> COST_LABEL = Map.ofEntries(
		Map.entry("RENT", "Hyra"),
		Map.entry("ELECTRICITY", "Hushållsel"),
		Map.entry("HOME_INSURANCE", "Hemförsäkring"),
		Map.entry("INTERNET", "Internet"),
		Map.entry("UNEMPLOYMENT_FUND", "A-kasseavgift"),
		Map.entry("UNION_FEE", "Fackavgift"),
		Map.entry("TRAVEL_APPROVED", "Resor"),
		Map.entry("TRAVEL_MEDICAL_TRANSPORT", "Sjukresor/färdtjänst"),
		Map.entry("MEDICAL_CARE", "Hälso- och sjukvård"),
		Map.entry("MEDICINE", "Medicin"),
		Map.entry("OTHER", "Övrigt bistånd"));

	private final ExpenseRulesService expenseRulesService;
	private final RenewalDeltaService renewalDeltaService;

	CalculationFeeder(final ExpenseRulesService expenseRulesService, final RenewalDeltaService renewalDeltaService) {
		this.expenseRulesService = expenseRulesService;
		this.renewalDeltaService = renewalDeltaService;
	}

	/** The fresh expense process rows plus the expense warnings the rules raised for them. */
	public record ExpenseFeed(List<FaNormExpenseEntity> rows, List<WarningService.WarningInput> warnings) {}

	/**
	 * The fresh income process rows — one per FamilyCare income type, the classified lines folded into a applicant +
	 * co-applicant
	 * side. Within each (FamilyCare type, recipient) the amounts are summed: the SSBTEK/classified path arrives pre-summed
	 * (one
	 * line per recipient), but the application/new-application path emits one line per raw declared income, so two
	 * same-type
	 * same-recipient incomes (e.g. two OTHER_INCOME) must be added together rather than dropping all but the first — else
	 * the income is understated and the computed benefit inflated. The first line in a recipient group supplies the
	 * non-amount fields (date, type name, note).
	 */
	public List<FaNormIncomeEntity> incomeRows(final String errandId, final List<FamilyCareIncomeLine> lines) {
		final var byType = ofNullable(lines).orElseGet(List::of).stream()
			.filter(line -> line.typeId() != null)
			.collect(Collectors.groupingBy(FamilyCareIncomeLine::typeId, LinkedHashMap::new, Collectors.toList()));

		return byType.entrySet().stream().map(entry -> {
			final var group = entry.getValue();
			final var applicant = recipientLines(group, RECIPIENT_APPLICANT);
			final var coApplicant = recipientLines(group, RECIPIENT_CO_APPLICANT);
			final var any = group.getFirst();
			return FaNormIncomeEntity.create()
				.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
				.withTypeId(entry.getKey()).withTypeName(any.typeName())
				.withApplicantProcessAmount(sumAmounts(applicant))
				.withApplicantAmountDate(firstDate(applicant))
				.withCoapplicantProcessAmount(sumAmounts(coApplicant))
				.withCoapplicantAmountDate(firstDate(coApplicant))
				.withNote(any.note());
		}).toList();
	}

	/** The lines in a type group belonging to a single recipient, in encounter order. */
	private static List<FamilyCareIncomeLine> recipientLines(final List<FamilyCareIncomeLine> group, final String recipient) {
		return group.stream().filter(line -> recipient.equals(line.recipient())).toList();
	}

	/**
	 * The summed amount across a recipient's lines, or {@code null} when the recipient has no lines at all. Null
	 * individual amounts are skipped; a recipient with only null-amount lines therefore sums to zero.
	 */
	private static BigDecimal sumAmounts(final List<FamilyCareIncomeLine> lines) {
		if (lines.isEmpty()) {
			return null;
		}
		return lines.stream()
			.map(FamilyCareIncomeLine::amount)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * The first line's amount date for a recipient (the non-amount fields come from the first line), {@code null} when
	 * none.
	 */
	private static OffsetDateTime firstDate(final List<FamilyCareIncomeLine> lines) {
		return lines.stream().findFirst().map(FamilyCareIncomeLine::date).orElse(null);
	}

	/**
	 * The fresh expense process rows — one per applied cost, the process amount + bucket coming from the rules — plus
	 * the expense warnings the rules raised: a manual reasonableness assessment ({@code EXPENSE_REVIEW}) for a flagged
	 * cost,
	 * and a cap ({@code EXPENSE_CAPPED}) when the process amount lands below what the citizen applied for. The decision is
	 * evaluated once per cost; the verdict feeds both the row and its warnings.
	 */
	public ExpenseFeed expenseFeed(final String municipalityId, final String errandId, final FinancialAssistanceEntity errand,
		final Map<String, Double> previousAmounts, final Integer applicantAge) {

		final var childCount = ofNullable(errand.getChildren()).orElseGet(List::of).size();
		final var householdCount = householdSize(errand);
		final var previous = ofNullable(previousAmounts).orElseGet(Map::of);

		final var rows = new ArrayList<FaNormExpenseEntity>();
		final var warnings = new ArrayList<WarningService.WarningInput>();

		ofNullable(errand.getCosts()).orElseGet(List::of).forEach(cost -> {
			final var previousApproved = ofNullable(previous.get(cost.getCostType())).map(BigDecimal::valueOf).orElse(null);
			final var verdict = expenseRulesService.verdict(municipalityId, cost.getCostType(), cost.getAppliedAmount(),
				previousApproved, applicantAge, childCount, householdCount);
			rows.add(FaNormExpenseEntity.create()
				.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
				.withCostType(cost.getCostType()).withOtherSubType(cost.getOtherSubType()).withSpecification(cost.getSpecification())
				.withAppliedAmount(cost.getAppliedAmount()).withProcessAmount(verdict.processAmount()).withBucket(verdict.bucket()));
			warnings.addAll(expenseWarnings(cost, verdict));
		});

		return new ExpenseFeed(List.copyOf(rows), List.copyOf(warnings));
	}

	/**
	 * The application's expense rows for the direct new-application commit — applied amount + the cost type's static
	 * bucket,
	 * with no history rule tree (a new application has no previous month) and no warnings. The rule tree applies to the
	 * daily-prepare draft ({@link #expenseFeed}) instead, where there is history and a caseworker review before commit.
	 */
	public List<FaNormExpenseEntity> applicationExpenseRows(final String errandId, final FinancialAssistanceEntity errand) {
		return ofNullable(errand.getCosts()).orElseGet(List::of).stream()
			.map(cost -> FaNormExpenseEntity.create()
				.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
				.withCostType(cost.getCostType()).withOtherSubType(cost.getOtherSubType()).withSpecification(cost.getSpecification())
				.withAppliedAmount(cost.getAppliedAmount()).withProcessAmount(cost.getAppliedAmount())
				.withBucket(ExpenseRulesService.bucketForCostType(cost.getCostType())))
			.toList();
	}

	/** The number of persons in the household — the declared housingPersonCount, else persons + children. */
	private static Integer householdSize(final FinancialAssistanceEntity errand) {
		final var declared = errand.getHousingPersonCount();
		if (declared != null) {
			return declared;
		}
		final var persons = ofNullable(errand.getPersons()).orElseGet(List::of).size();
		final var children = ofNullable(errand.getChildren()).orElseGet(List::of).size();
		return persons + children;
	}

	/** The warnings a single cost's verdict raises — a manual review flag and/or a cap below the applied amount. */
	private static List<WarningService.WarningInput> expenseWarnings(final FaCost cost, final ExpenseRulesService.ExpenseVerdict verdict) {
		final var warnings = new ArrayList<WarningService.WarningInput>();
		final var sourceKey = expenseSourceKey(cost);
		final var label = expenseLabel(cost);

		if (verdict.warning()) {
			final var reason = ofNullable(verdict.rule()).filter(text -> !text.isBlank()).orElse("Utgiften kräver en manuell skälighetsbedömning");
			warnings.add(new WarningService.WarningInput(WarningService.TYPE_EXPENSE_REVIEW, sourceKey, label + ": " + reason));
		}
		if (isCapped(cost.getAppliedAmount(), verdict.processAmount())) {
			warnings.add(new WarningService.WarningInput(WarningService.TYPE_EXPENSE_CAPPED, sourceKey,
				"Kapad kostnad: " + label + " – ansökt " + plain(cost.getAppliedAmount()) + " kr, beviljat " + plain(verdict.processAmount()) + " kr"));
		}
		return warnings;
	}

	private static boolean isCapped(final BigDecimal applied, final BigDecimal process) {
		return (applied != null) && (process != null) && (process.compareTo(applied) < 0);
	}

	/** Stable dedup key for the cost a warning concerns — cost type, plus the other sub-type when present. */
	private static String expenseSourceKey(final FaCost cost) {
		final var sub = cost.getOtherSubType();
		if ((sub == null) || sub.isBlank()) {
			return nz(cost.getCostType());
		}
		return nz(cost.getCostType()) + ":" + sub;
	}

	private static String expenseLabel(final FaCost cost) {
		final var label = COST_LABEL.getOrDefault(cost.getCostType(), nz(cost.getCostType()));
		final var sub = cost.getOtherSubType();
		if ((sub == null) || sub.isBlank()) {
			return label;
		}
		return label + " (" + sub + ")";
	}

	/** Plain (no scientific notation, no trailing zeros) rendering of a non-null amount for warning messages. */
	private static String plain(final BigDecimal amount) {
		return amount.stripTrailingZeros().toPlainString();
	}

	private static String nz(final String value) {
		return ofNullable(value).orElse("");
	}

	/**
	 * The fresh person process rows — applicant + co-applicant (full month) and each child (days in the home; a part-time
	 * child becomes an visitation child). All start {@code included = true}; the caseworker may later exclude one (is
	 * included).
	 */
	public List<FaNormPersonEntity> personRows(final String errandId, final FinancialAssistanceEntity errand) {
		final var rows = new ArrayList<FaNormPersonEntity>();

		ofNullable(errand.getPersons()).orElseGet(List::of).forEach(person -> rows.add(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(person.getPartyId()).withRole(person.getRole()).withProcessDays(FULL_MONTH_DAYS).withIncluded(true)));

		ofNullable(errand.getChildren()).orElseGet(List::of).forEach(child -> rows.add(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(child.getPartyId()).withRole(childRole(child.getResidenceExtent())).withName(childName(child.getFirstName(), child.getLastName()))
			.withProcessDays(ofNullable(child.getDaysInHome()).orElse(FULL_MONTH_DAYS)).withIncluded(true)));

		return rows;
	}

	/**
	 * Renewal delta warnings against the previous calculation in Lifecare — household-size drift (members
	 * added/removed, count changed) and housing-cost drift. Each candidate delta is classified by the
	 * {@code Decision_ateransokanDelta} DMN, which decides — by what changed and how much — whether it is worth flagging
	 * and the note to show; a small change passes silently. New members are surfaced separately as NEW_PERSON warnings
	 * from the merge.
	 */
	public List<WarningService.WarningInput> householdDeltaWarnings(final String municipalityId, final FinancialAssistanceEntity errand,
		final List<FaNormPersonEntity> currentPersons, final PreviousHousehold previous) {

		if ((previous == null) || (previous.memberCount() == 0)) {
			return List.of();
		}

		final var warnings = new ArrayList<WarningService.WarningInput>();
		householdSizeWarning(municipalityId, currentPersons, previous).ifPresent(warnings::add);
		housingCostWarning(municipalityId, errand, previous).ifPresent(warnings::add);
		return List.copyOf(warnings);
	}

	/** The household-size delta (count change + members no longer present), classified by the DMN. */
	private Optional<WarningService.WarningInput> householdSizeWarning(final String municipalityId,
		final List<FaNormPersonEntity> currentPersons, final PreviousHousehold previous) {

		final var currentPartyIds = ofNullable(currentPersons).orElseGet(List::of).stream()
			.map(FaNormPersonEntity::getPartyId).filter(partyId -> (partyId != null) && !partyId.isBlank()).collect(Collectors.toSet());
		final var missingPartyIds = previous.personIds().stream().filter(partyId -> !currentPartyIds.contains(partyId)).toList();
		final var sizeDelta = currentPartyIds.size() - previous.memberCount();

		if ((sizeDelta == 0) && missingPartyIds.isEmpty()) {
			return Optional.empty();
		}

		final var verdict = renewalDeltaService.classify(municipalityId, CHANGE_HOUSEHOLD_SIZE, sizeDelta, BigDecimal.ZERO);
		if (!verdict.warning()) {
			return Optional.empty();
		}

		var detail = "Antal hushållsmedlemmar har ändrats sedan föregående beräkning (tidigare " + previous.memberCount() + ", nu " + currentPartyIds.size() + ")";
		if (!missingPartyIds.isEmpty()) {
			detail += " — saknas nu: " + String.join(", ", missingPartyIds);
		}
		return Optional.of(new WarningService.WarningInput(WarningService.TYPE_HOUSEHOLD_CHANGE, "household-size", withRule(detail, verdict.rule())));
	}

	/** The housing-cost delta (previous Rent vs current applied RENT, as a signed percent), classified by the DMN. */
	private Optional<WarningService.WarningInput> housingCostWarning(final String municipalityId, final FinancialAssistanceEntity errand,
		final PreviousHousehold previous) {

		final var previousCost = previous.housingCost();
		if ((previousCost == null) || (previousCost <= 0)) {
			return Optional.empty();
		}

		final var currentRent = currentRent(errand);
		final var previousBd = BigDecimal.valueOf(previousCost);
		final var percent = currentRent.subtract(previousBd).multiply(HUNDRED).divide(previousBd, 0, RoundingMode.HALF_UP);

		final var verdict = renewalDeltaService.classify(municipalityId, CHANGE_HOUSING_COST, 0, percent);
		if (!verdict.warning()) {
			return Optional.empty();
		}

		final String sign;
		if (percent.signum() >= 0) {
			sign = "+";
		} else {
			sign = "";
		}
		final var detail = "Boendekostnaden har ändrats " + sign + percent + "% (tidigare " + plain(previousBd) + " kr → nu " + plain(currentRent) + " kr)";
		return Optional.of(new WarningService.WarningInput(WarningService.TYPE_HOUSING_COST_CHANGE, "housing-cost", withRule(detail, verdict.rule())));
	}

	/** Sum of the application's reported RENT costs (0 when none). */
	private static BigDecimal currentRent(final FinancialAssistanceEntity errand) {
		return ofNullable(errand.getCosts()).orElseGet(List::of).stream()
			.filter(cost -> COST_TYPE_RENT.equals(cost.getCostType()))
			.map(FaCost::getAppliedAmount)
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static String withRule(final String detail, final String rule) {
		if ((rule == null) || rule.isBlank()) {
			return detail;
		}
		return detail + " — " + rule;
	}

	/** A full-time child is a CHILD; a part-time / other child is an visitation child. */
	private static String childRole(final String residenceExtent) {
		if ((residenceExtent == null) || RESIDENCE_FULL_TIME.equals(residenceExtent)) {
			return ROLE_CHILD;
		}
		return ROLE_VISITATION_CHILD;
	}

	private static String childName(final String firstName, final String lastName) {
		return Stream.of(firstName, lastName)
			.filter(part -> (part != null) && !part.isBlank())
			.collect(Collectors.joining(" "));
	}
}
