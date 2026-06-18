package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.BUCKET_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.BUCKET_SPECIAL_EXPENSE;

/**
 * The utgift (expense) regelverk — the process-decided amount for a cost <em>and</em> which FC array it belongs to. It
 * evaluates the modeler-editable {@code Decision_utgiftRegelverk} DMN in the operaton engine (the same place the income
 * rålista lives) for the cost type in its household context and returns the allowed (capped) amount plus the bucket:
 * {@code EXPENSE} (UTGIFTER → FC CalculationExpenses) or {@code SPECIAL_EXPENSE} (LEVNADSKOSTNADER I ÖVRIGT → FC
 * CalculationSpecialExpenses). Best-effort: when the decision is unavailable or returns nothing, the applied amount is
 * used as the process amount and the bucket defaults to {@code EXPENSE} so the daily prepare is never blocked.
 */
@Service
public class ExpenseRegelverkService {

	static final String DECISION_KEY = "Decision_utgiftRegelverk";
	private static final String OUTPUT_APPROVED_AMOUNT = "approvedAmount";
	private static final String OUTPUT_BUCKET = "bucket";

	private static final Logger LOG = LoggerFactory.getLogger(ExpenseRegelverkService.class);

	private final ProcessService processService;

	ExpenseRegelverkService(final ProcessService processService) {
		this.processService = processService;
	}

	/** The regelverk verdict for a cost — the process (capped) amount and the FC bucket it posts to. */
	public record ExpenseVerdict(BigDecimal processAmount, String bucket) {}

	/**
	 * The regelverk verdict for a cost: the process-decided (capped) amount and its bucket. Falls back to the applied
	 * amount + {@code EXPENSE} when the regelverk allows it in full or is unavailable.
	 *
	 * @param  municipalityId     the municipality the errand belongs to
	 * @param  costType           the EB cost type (e.g. RENT, ELECTRICITY)
	 * @param  otherSubType       the övrigt-bistånd sub-type, when the cost type is OTHER (may be {@code null})
	 * @param  housingForm        the household's housing form (cap context, may be {@code null})
	 * @param  housingPersonCount the household adult count (cap context, may be {@code null})
	 * @param  normType           the norm type (RIKSNORM/OTHER_NORM, may be {@code null})
	 * @param  appliedAmount      what the citizen applied for — the fallback and upper bound
	 * @return                    the verdict (capped amount + bucket), best-effort
	 */
	public ExpenseVerdict verdict(final String municipalityId, final String costType, final String otherSubType, final String housingForm,
		final Integer housingPersonCount, final String normType, final BigDecimal appliedAmount) {

		try {
			final var variables = new HashMap<String, Object>();
			variables.put("costType", nz(costType));
			variables.put("otherSubType", nz(otherSubType));
			variables.put("housingForm", nz(housingForm));
			variables.put("housingPersonCount", housingPersonCount == null ? 0 : housingPersonCount);
			variables.put("normType", nz(normType));
			variables.put("appliedAmount", appliedAmount == null ? 0 : appliedAmount);

			final var rows = processService.evaluateDecision(municipalityId, DECISION_KEY, variables);
			if (rows.isEmpty()) {
				return new ExpenseVerdict(appliedAmount, BUCKET_EXPENSE);
			}
			final var row = rows.getFirst();
			final var approved = row.get(OUTPUT_APPROVED_AMOUNT);
			final var amount = approved == null ? appliedAmount : new BigDecimal(approved.toString());
			return new ExpenseVerdict(amount, bucketOf(row.get(OUTPUT_BUCKET)));
		} catch (final RuntimeException e) {
			LOG.warn("Expense regelverk ({}) unavailable — using the applied amount + EXPENSE bucket", DECISION_KEY, e);
			return new ExpenseVerdict(appliedAmount, BUCKET_EXPENSE);
		}
	}

	private static String bucketOf(final Object value) {
		return BUCKET_SPECIAL_EXPENSE.equals(value == null ? null : value.toString()) ? BUCKET_SPECIAL_EXPENSE : BUCKET_EXPENSE;
	}

	private static String nz(final String value) {
		return value == null ? "" : value;
	}
}
