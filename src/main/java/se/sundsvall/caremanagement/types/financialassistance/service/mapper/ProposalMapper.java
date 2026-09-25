package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PreviousDecision;
import se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceTypes;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceLabels.costDisplayName;

/**
 * The pure derivations behind the section proposals — the outcome rule, the estimated amount, the expense
 * partial-rejection check — and the projection of the Lifecare decision view onto the decision
 * proposal.
 * No I/O; everything here is a function of its arguments.
 */
public final class ProposalMapper {

	public static final String OUTCOME_BIFALL = "BIFALL";
	public static final String OUTCOME_DELAVSLAG = "DELAVSLAG";
	public static final String OUTCOME_AVSLAG = "AVSLAG";

	private ProposalMapper() {}

	/**
	 * An expense approved below what was applied for: its warning key, its label, the applied amount and the part not
	 * approved.
	 */
	public record PartialRejection(String sourceKey, String label, BigDecimal appliedAmount, BigDecimal rejectedAmount) {}

	/**
	 * The estimated bistånd: {@code normSum + expenseSum + specialExpenseSum - incomeSum}. The draft sums are the
	 * effective (approved) amounts; a missing sum counts as zero.
	 */
	public static BigDecimal estimatedAmount(final CalculationDraft draft, final BigDecimal normSum) {
		return normSum
			.add(orZero(draft.getExpenseSum()))
			.add(orZero(draft.getSpecialExpenseSum()))
			.subtract(orZero(draft.getIncomeSum()));
	}

	/**
	 * The outcome rule: {@code amount <= 0} → AVSLAG; {@code amount > 0} and every expense fully approved → BIFALL;
	 * {@code amount > 0} and any expense approved below its applied amount → DELAVSLAG.
	 */
	public static String outcome(final BigDecimal estimatedAmount, final List<PartialRejection> partiallyRejected) {
		if (estimatedAmount.signum() <= 0) {
			return OUTCOME_AVSLAG;
		}
		if (partiallyRejected.isEmpty()) {
			return OUTCOME_BIFALL;
		}
		return OUTCOME_DELAVSLAG;
	}

	/**
	 * The live expense rows of careM's draft (both buckets) whose approved (effective) amount is below the applied amount.
	 */
	public static List<PartialRejection> partiallyRejectedExpenses(final CalculationDraft draft) {
		return Stream.concat(orEmpty(draft.getExpenses()).stream(), orEmpty(draft.getSpecialExpenses()).stream())
			.filter(row -> !row.isDeleted())
			.filter(row -> row.getAppliedAmount() != null)
			.filter(row -> orZero(row.getEffectiveAmount()).compareTo(row.getAppliedAmount()) < 0)
			.map(row -> new PartialRejection(expenseSourceKey(row), expenseLabel(row), row.getAppliedAmount(), row.getAppliedAmount().subtract(orZero(row.getEffectiveAmount()))))
			.toList();
	}

	/**
	 * The expenses of the normberäkning saved in Lifecare (both buckets) approved below what was applied for — what the
	 * caseworker decided there, which careM's draft no longer follows once the calculation is saved. Rows of one type
	 * are summed, since the Lifecare listing carries no sub type to tell them apart. The amounts are compared as the
	 * magnitudes they are, whatever sign FamilyCare's listing gives them. The key is the cost type code when the
	 * Lifecare name is one of careM's own, so a warning raised from the draft carries over.
	 */
	public static List<PartialRejection> partiallyRejectedExpenses(final CalculationView calculation) {
		final var applied = new LinkedHashMap<String, BigDecimal>();
		final var approved = new LinkedHashMap<String, BigDecimal>();
		final var names = new LinkedHashMap<String, String>();
		Stream.concat(orEmpty(calculation.expenses()).stream(), orEmpty(calculation.specialExpenses()).stream())
			.filter(Objects::nonNull)
			.filter(row -> hasText(row.type()) && (row.appliedAmount() != null))
			.forEach(row -> {
				final var type = row.type().trim();
				final var key = type.toLowerCase(Locale.ROOT);
				names.putIfAbsent(key, type);
				applied.merge(key, row.appliedAmount().abs(), BigDecimal::add);
				approved.merge(key, orZero(row.approvedAmount()).abs(), BigDecimal::add);
			});
		return names.entrySet().stream()
			.filter(entry -> approved.get(entry.getKey()).compareTo(applied.get(entry.getKey())) < 0)
			.map(entry -> new PartialRejection(lifecareExpenseSourceKey(entry.getValue()), entry.getValue(), applied.get(entry.getKey()),
				applied.get(entry.getKey()).subtract(approved.get(entry.getKey()))))
			.toList();
	}

	/** The cost type code whose Lifecare name this is, or the name itself, prefixed, when careM has no such type. */
	static String lifecareExpenseSourceKey(final String lifecareType) {
		return FinancialAssistanceTypes.COST_TYPES.stream()
			.filter(option -> hasText(option.getCode()) && lifecareType.equalsIgnoreCase(costDisplayName(option.getCode())))
			.map(option -> option.getCode())
			.findFirst()
			.orElseGet(() -> "lifecare:" + lifecareType.toLowerCase(Locale.ROOT));
	}

	/**
	 * The handläggare label for an expense row: the cost type's Lifecare name, with the sub type / specification when set.
	 */
	public static String expenseLabel(final NormExpenseRow row) {
		final var label = ofNullable(costDisplayName(row.getCostType())).orElse("Utgift");
		final var detail = Stream.of(row.getOtherSubType(), row.getSpecification())
			.filter(text -> hasText(text))
			.findFirst();
		return label + detail.map(text -> " (" + text + ")").orElse("");
	}

	/** A stable per-expense dedup key: the cost type code plus the sub type when set. */
	public static String expenseSourceKey(final NormExpenseRow row) {
		final var costType = ofNullable(row.getCostType()).orElse("");
		if (hasText(row.getOtherSubType())) {
			return costType + ":" + row.getOtherSubType();
		}
		return costType;
	}

	/** The amount as a plain integer-ish string for warning texts ("8500", "1250.5"). */
	public static String plain(final BigDecimal amount) {
		return orZero(amount).stripTrailingZeros().toPlainString();
	}

	/**
	 * Whether a Lifecare decision is an återkrav — a decision “mot återbetalning”, whose money Lifecare marks as owed
	 * back (IFO-handboken, Beslut – Bistånd mot återbetalning). FamilyCare's decision read carries the decision type as
	 * text and no category, so the type is matched case-insensitively on återbetalning or återkrav, the way
	 * {@link #isAdvanceOnBenefit} matches förskott. An eftergift decision, which forgives such money, is not one.
	 * The municipality's own type names are not configured for ekonomiskt bistånd yet; the match is to be verified
	 * against them.
	 */
	public static boolean isRecoveryClaim(final DecisionView decision) {
		return ofNullable(decision.type())
			.map(type -> type.toLowerCase(Locale.ROOT))
			.filter(type -> type.contains("återbetalning") || type.contains("återkrav"))
			.filter(type -> !type.contains("eftergift"))
			.isPresent();
	}

	/**
	 * Whether a Lifecare decision was a förskott på förmån — matched case-insensitively on its decision type only
	 * (verksamhetens svar 2026-09-24 §2: e.g. ”EK Förskott på förmån 12 Kap 1 § och 33 kap 2 § SoL, bifall”). The reason
	 * is free text and is deliberately not read.
	 */
	public static boolean isAdvanceOnBenefit(final DecisionView decision) {
		return ofNullable(decision.type())
			.filter(type -> type.toLowerCase().contains("förskott"))
			.isPresent();
	}

	public static PreviousDecision toPreviousDecision(final DecisionView view) {
		return PreviousDecision.create()
			.withType(view.type())
			.withReason(view.reason())
			.withCoApplicant(view.coApplicant())
			.withCoApplicantReason(view.reasonCoApplicant())
			.withPeriodFrom(view.fromDate())
			.withPeriodTo(view.toDate())
			.withAmount(view.amount())
			.withDate(view.date());
	}

	private static BigDecimal orZero(final BigDecimal amount) {
		return ofNullable(amount).orElse(BigDecimal.ZERO);
	}

	private static <T> List<T> orEmpty(final List<T> list) {
		return ofNullable(list).orElseGet(List::of);
	}
}
