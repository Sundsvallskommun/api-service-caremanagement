package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.lifecare.service.model.PaymentView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PreviousDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PreviousPayment;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceLabels.costDisplayName;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ROLE_CHILD;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ROLE_VISITATION_CHILD;

/**
 * The pure derivations behind the section proposals — the outcome rule, the estimated amount, the expense
 * partial-rejection check, the children check — and the projections of the Lifecare views onto the proposal models.
 * No I/O; everything here is a function of its arguments.
 */
public final class ProposalMapper {

	public static final String OUTCOME_BIFALL = "BIFALL";
	public static final String OUTCOME_DELAVSLAG = "DELAVSLAG";
	public static final String OUTCOME_AVSLAG = "AVSLAG";

	public static final String PHRASE_APPROVED_WITH_CHILDREN = "Bifall månad med barn";
	public static final String PHRASE_APPROVED_WITHOUT_CHILDREN = "Bifall månad utan barn";

	private ProposalMapper() {}

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
	public static String outcome(final BigDecimal estimatedAmount, final List<NormExpenseRow> partiallyRejected) {
		if (estimatedAmount.signum() <= 0) {
			return OUTCOME_AVSLAG;
		}
		if (partiallyRejected.isEmpty()) {
			return OUTCOME_BIFALL;
		}
		return OUTCOME_DELAVSLAG;
	}

	/** The frastext for an approved outcome — null for anything but BIFALL/DELAVSLAG. */
	public static String phraseText(final String outcome, final boolean childrenInCalculation) {
		if (!OUTCOME_BIFALL.equals(outcome) && !OUTCOME_DELAVSLAG.equals(outcome)) {
			return null;
		}
		if (childrenInCalculation) {
			return PHRASE_APPROVED_WITH_CHILDREN;
		}
		return PHRASE_APPROVED_WITHOUT_CHILDREN;
	}

	/** The live expense rows (both buckets) whose approved (effective) amount is below the applied amount. */
	public static List<NormExpenseRow> partiallyRejectedExpenses(final CalculationDraft draft) {
		return Stream.concat(orEmpty(draft.getExpenses()).stream(), orEmpty(draft.getSpecialExpenses()).stream())
			.filter(row -> !row.isDeleted())
			.filter(row -> row.getAppliedAmount() != null)
			.filter(row -> orZero(row.getEffectiveAmount()).compareTo(row.getAppliedAmount()) < 0)
			.toList();
	}

	/** Whether the calculation includes a child (barn or umgängesbarn) — a live, included person row with a child role. */
	public static boolean childrenInCalculation(final CalculationDraft draft) {
		return orEmpty(draft.getPersons()).stream()
			.filter(row -> !row.isDeleted())
			.filter(NormPersonRow::isIncluded)
			.anyMatch(row -> ROLE_CHILD.equals(row.getRole()) || ROLE_VISITATION_CHILD.equals(row.getRole()));
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

	/** Whether a Lifecare decision was a förskott på förmån — matched case-insensitively on its type or reason. */
	public static boolean isAdvanceOnBenefit(final DecisionView decision) {
		return Stream.of(decision.type(), decision.reason())
			.filter(text -> hasText(text))
			.anyMatch(text -> text.toLowerCase().contains("förskott"));
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

	public static PreviousPayment toPreviousPayment(final PaymentView view) {
		return PreviousPayment.create()
			.withPayDate(view.payDate())
			.withAmount(view.amount())
			.withConcernedMonth(view.concernedMonth())
			.withPaymentMethod(view.paymentMethod())
			.withName(view.name())
			.withClearing(view.clearing())
			.withAccountNumber(view.accountNumber())
			.withMessage(view.message());
	}

	public static Payee toPayee(final PaymentView view) {
		return Payee.create()
			.withName(view.name())
			.withPaymentMethod(view.paymentMethod())
			.withClearing(view.clearing())
			.withAccountNumber(view.accountNumber());
	}

	/** The payee the applicant stated in the application, or empty when the application names no account. */
	public static Optional<Payee> toPayee(final FaPerson person, final String name) {
		if (!hasText(person.getAccountNumber()) && !hasText(person.getPaymentMethod())) {
			return Optional.empty();
		}
		return Optional.of(Payee.create()
			.withName(name)
			.withPaymentMethod(person.getPaymentMethod())
			.withClearing(person.getClearingNumber())
			.withAccountNumber(person.getAccountNumber()));
	}

	/** The distinct payees among the payments, in first-seen order. */
	public static List<Payee> distinctPayees(final List<PaymentView> payments) {
		return payments.stream()
			.map(ProposalMapper::toPayee)
			.filter(payee -> hasText(payee.getName()) || hasText(payee.getAccountNumber()))
			.collect(toMap(identity(), identity(), (a, _) -> a, java.util.LinkedHashMap::new))
			.keySet()
			.stream()
			.toList();
	}

	private static BigDecimal orZero(final BigDecimal amount) {
		return ofNullable(amount).orElse(BigDecimal.ZERO);
	}

	private static <T> List<T> orEmpty(final List<T> list) {
		return ofNullable(list).orElseGet(List::of);
	}
}
