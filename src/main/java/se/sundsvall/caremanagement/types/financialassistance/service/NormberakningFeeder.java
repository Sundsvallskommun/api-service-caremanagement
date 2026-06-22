package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.service.model.FcIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCost;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.RECIPIENT_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.RECIPIENT_CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ROLE_CHILD;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ROLE_UMGANGESBARN;

/**
 * Builds the freshly computed process rows for the normberäkning sections from the errand: the income rows (one per FC
 * income type, with a sökande and medsökande side) from the operaton-classified incomes, the expense rows from the
 * application's costs — each given a process amount + bucket by the {@link ExpenseRegelverkService} — and the person
 * rows from the household (umgängesbarn = part-time children). Also compares the household against the previous
 * normberäkning in Lifecare to produce drift warnings. Every row is stamped {@code origin = SYSTEM}; the
 * {@link DraftService} merge then refreshes only the process columns.
 */
@Service
public class NormberakningFeeder {

	private static final int FULL_MONTH_DAYS = 30;
	private static final String RESIDENCE_FULL_TIME = "FULL_TIME";
	private static final String COST_TYPE_RENT = "RENT";
	private static final String CHANGE_HOUSEHOLD_SIZE = "HOUSEHOLD_SIZE";
	private static final String CHANGE_HOUSING_COST = "HOUSING_COST";
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final ExpenseRegelverkService expenseRegelverkService;
	private final AteransokanDeltaService ateransokanDeltaService;

	NormberakningFeeder(final ExpenseRegelverkService expenseRegelverkService, final AteransokanDeltaService ateransokanDeltaService) {
		this.expenseRegelverkService = expenseRegelverkService;
		this.ateransokanDeltaService = ateransokanDeltaService;
	}

	/** The fresh expense process rows plus the expense warnings the regelverk raised for them. */
	public record ExpenseFeed(List<FaNormExpenseEntity> rows, List<WarningService.WarningInput> warnings) {}

	/**
	 * The fresh income process rows — one per FC income type, the classified lines folded into a sökande + medsökande side.
	 */
	public List<FaNormIncomeEntity> incomeRows(final String errandId, final List<FcIncomeLine> lines) {
		final var byType = ofNullable(lines).orElseGet(List::of).stream()
			.filter(line -> line.typeId() != null)
			.collect(Collectors.groupingBy(FcIncomeLine::typeId, LinkedHashMap::new, Collectors.toList()));

		return byType.entrySet().stream().map(entry -> {
			final var group = entry.getValue();
			final var applicant = group.stream().filter(line -> RECIPIENT_APPLICANT.equals(line.recipient())).findFirst();
			final var coApplicant = group.stream().filter(line -> RECIPIENT_CO_APPLICANT.equals(line.recipient())).findFirst();
			final var any = group.getFirst();
			return FaNormIncomeEntity.create()
				.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
				.withTypeId(entry.getKey()).withTypeName(any.typeName())
				.withApplicantProcessAmount(applicant.map(FcIncomeLine::amount).orElse(null))
				.withApplicantAmountDate(applicant.map(FcIncomeLine::date).orElse(null))
				.withCoapplicantProcessAmount(coApplicant.map(FcIncomeLine::amount).orElse(null))
				.withCoapplicantAmountDate(coApplicant.map(FcIncomeLine::date).orElse(null))
				.withNote(any.note());
		}).toList();
	}

	/**
	 * The fresh expense process rows — one per applied cost, the process amount + bucket coming from the regelverk — plus
	 * the expense warnings the regelverk raised: a manual skälighetsbedömning ({@code EXPENSE_REVIEW}) for a flagged cost,
	 * and a cap ({@code EXPENSE_CAPPED}) when the process amount lands below what the citizen applied for. The decision is
	 * evaluated once per cost; the verdict feeds both the row and its warnings.
	 */
	public ExpenseFeed expenseFeed(final String municipalityId, final String errandId, final FinancialAssistanceEntity errand) {
		final var housingForm = errand.getHousingForm();
		final var housingPersonCount = errand.getHousingPersonCount();
		final var normType = errand.getNormType();

		final var rows = new ArrayList<FaNormExpenseEntity>();
		final var warnings = new ArrayList<WarningService.WarningInput>();

		ofNullable(errand.getCosts()).orElseGet(List::of).forEach(cost -> {
			final var verdict = expenseRegelverkService.verdict(municipalityId, cost.getCostType(), cost.getOtherSubType(),
				housingForm, housingPersonCount, normType, cost.getAppliedAmount());
			rows.add(FaNormExpenseEntity.create()
				.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
				.withCostType(cost.getCostType()).withOtherSubType(cost.getOtherSubType()).withSpecification(cost.getSpecification())
				.withAppliedAmount(cost.getAppliedAmount()).withProcessAmount(verdict.processAmount()).withBucket(verdict.bucket()));
			warnings.addAll(expenseWarnings(cost, verdict));
		});

		return new ExpenseFeed(List.copyOf(rows), List.copyOf(warnings));
	}

	/** The warnings a single cost's verdict raises — a manual review flag and/or a cap below the applied amount. */
	private static List<WarningService.WarningInput> expenseWarnings(final FaCost cost, final ExpenseRegelverkService.ExpenseVerdict verdict) {
		final var warnings = new ArrayList<WarningService.WarningInput>();
		final var sourceKey = expenseSourceKey(cost);
		final var label = expenseLabel(cost);

		if (verdict.varning()) {
			final var reason = ofNullable(verdict.regel()).filter(text -> !text.isBlank()).orElse("Utgiften kräver manuell skälighetsbedömning");
			warnings.add(new WarningService.WarningInput(WarningService.TYPE_EXPENSE_REVIEW, sourceKey, label + ": " + reason));
		}
		if (isCapped(cost.getAppliedAmount(), verdict.processAmount())) {
			warnings.add(new WarningService.WarningInput(WarningService.TYPE_EXPENSE_CAPPED, sourceKey,
				"Kapad kostnad: " + label + " – ansökt " + plain(cost.getAppliedAmount()) + " kr, beviljas " + plain(verdict.processAmount()) + " kr"));
		}
		return warnings;
	}

	private static boolean isCapped(final BigDecimal applied, final BigDecimal process) {
		return (applied != null) && (process != null) && (process.compareTo(applied) < 0);
	}

	/** Stable dedup key for the cost a warning concerns — cost type, plus the övrigt sub-type when present. */
	private static String expenseSourceKey(final FaCost cost) {
		final var sub = cost.getOtherSubType();
		return ((sub == null) || sub.isBlank()) ? nz(cost.getCostType()) : nz(cost.getCostType()) + ":" + sub;
	}

	private static String expenseLabel(final FaCost cost) {
		final var sub = cost.getOtherSubType();
		return ((sub == null) || sub.isBlank()) ? nz(cost.getCostType()) : nz(cost.getCostType()) + " (" + sub + ")";
	}

	/** Plain (no scientific notation, no trailing zeros) rendering of a non-null amount for warning messages. */
	private static String plain(final BigDecimal amount) {
		return amount.stripTrailingZeros().toPlainString();
	}

	private static String nz(final String value) {
		return (value == null) ? "" : value;
	}

	/**
	 * The fresh person process rows — applicant + co-applicant (full month) and each child (days in the home; a part-time
	 * child becomes an umgängesbarn). All start {@code included = true}; the handläggare may later exclude one (omfattas).
	 */
	public List<FaNormPersonEntity> personRows(final String errandId, final FinancialAssistanceEntity errand) {
		final var rows = new ArrayList<FaNormPersonEntity>();

		ofNullable(errand.getPersons()).orElseGet(List::of).forEach(person -> rows.add(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(person.getPartyId()).withRole(person.getRole()).withProcessDays(FULL_MONTH_DAYS).withIncluded(true)));

		ofNullable(errand.getChildren()).orElseGet(List::of).forEach(child -> rows.add(FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_SYSTEM)
			.withPartyId(child.getPartyId()).withRole(childRole(child.getResidenceExtent())).withName(childName(child.getFirstName(), child.getLastName()))
			.withProcessDays(child.getDaysInHome() != null ? child.getDaysInHome() : FULL_MONTH_DAYS).withIncluded(true)));

		return rows;
	}

	/**
	 * Återansökan delta warnings against the previous normberäkning in Lifecare — household-size drift (members
	 * added/removed, count changed) and boende-cost drift. Each candidate delta is classified by the
	 * {@code Decision_ateransokanDelta} DMN, which decides — by what changed and how much — whether it is worth flagging
	 * and the notering to show; a small change passes silently. New members are surfaced separately as NEW_PERSON warnings
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

		final var currentIds = ofNullable(currentPersons).orElseGet(List::of).stream()
			.map(FaNormPersonEntity::getPartyId).filter(id -> (id != null) && !id.isBlank()).collect(Collectors.toSet());
		final var missing = previous.personIds().stream().filter(id -> !currentIds.contains(id)).toList();
		final var sizeDelta = currentIds.size() - previous.memberCount();

		if ((sizeDelta == 0) && missing.isEmpty()) {
			return Optional.empty();
		}

		final var verdict = ateransokanDeltaService.classify(municipalityId, CHANGE_HOUSEHOLD_SIZE, sizeDelta, BigDecimal.ZERO);
		if (!verdict.varning()) {
			return Optional.empty();
		}

		var detail = "Antal hushållsmedlemmar ändrat sedan föregående normberäkning (föregående " + previous.memberCount() + ", nu " + currentIds.size() + ")";
		if (!missing.isEmpty()) {
			detail += " — saknas nu: " + String.join(", ", missing);
		}
		return Optional.of(new WarningService.WarningInput(WarningService.TYPE_HOUSEHOLD_CHANGE, "hushall-storlek", withRegel(detail, verdict.regel())));
	}

	/** The boende-cost delta (previous Hyra vs current applied RENT, as a signed percent), classified by the DMN. */
	private Optional<WarningService.WarningInput> housingCostWarning(final String municipalityId, final FinancialAssistanceEntity errand,
		final PreviousHousehold previous) {

		final var previousCost = previous.housingCost();
		if ((previousCost == null) || (previousCost <= 0)) {
			return Optional.empty();
		}

		final var currentRent = currentRent(errand);
		final var previousBd = BigDecimal.valueOf(previousCost);
		final var percent = currentRent.subtract(previousBd).multiply(HUNDRED).divide(previousBd, 0, RoundingMode.HALF_UP);

		final var verdict = ateransokanDeltaService.classify(municipalityId, CHANGE_HOUSING_COST, 0, percent);
		if (!verdict.varning()) {
			return Optional.empty();
		}

		final var sign = (percent.signum() >= 0) ? "+" : "";
		final var detail = "Boendekostnad ändrad " + sign + percent + "% (föregående " + plain(previousBd) + " kr → nu " + plain(currentRent) + " kr)";
		return Optional.of(new WarningService.WarningInput(WarningService.TYPE_HOUSING_COST_CHANGE, "boende-kostnad", withRegel(detail, verdict.regel())));
	}

	/** Sum of the application's reported RENT costs (0 when none). */
	private static BigDecimal currentRent(final FinancialAssistanceEntity errand) {
		return ofNullable(errand.getCosts()).orElseGet(List::of).stream()
			.filter(cost -> COST_TYPE_RENT.equals(cost.getCostType()))
			.map(FaCost::getAppliedAmount)
			.filter(amount -> amount != null)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static String withRegel(final String detail, final String regel) {
		return ((regel == null) || regel.isBlank()) ? detail : detail + " — " + regel;
	}

	/** A full-time child is a CHILD; a part-time / övrigt child is an umgängesbarn. */
	private static String childRole(final String residenceExtent) {
		return ((residenceExtent == null) || RESIDENCE_FULL_TIME.equals(residenceExtent)) ? ROLE_CHILD : ROLE_UMGANGESBARN;
	}

	private static String childName(final String firstName, final String lastName) {
		return Stream.of(firstName, lastName)
			.filter(part -> (part != null) && !part.isBlank())
			.collect(Collectors.joining(" "));
	}
}
