package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.BUCKET_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.BUCKET_SPECIAL_EXPENSE;

/**
 * The expense rules — the process-decided (reasonable) amount for a cost, the FC array it belongs to, and the
 * manual-review flag. Each cost type has its <em>own</em> modeler-editable decision in the operaton engine running
 * the business unit's rent rule tree (the 8-row history logic: the amount is governed by the previous month's approved
 * amount, the threshold governs the warning text), so any type can diverge later without touching the others:
 *
 * <ul>
 * <li>{@code RENT} → {@code Decision_hyra} (cap per age/children) · {@code HOME_INSURANCE} →
 * {@code Decision_hemforsakring}
 * (cap per household size)</li>
 * <li>fixed caps:
 * {@code ELECTRICITY}/{@code INTERNET}/{@code UNEMPLOYMENT_FUND}/{@code UNION_FEE}/{@code TRAVEL_APPROVED}/
 * {@code TRAVEL_MEDICAL_TRANSPORT}/{@code MEDICAL_CARE}/{@code MEDICINE}</li>
 * <li>{@code OTHER} → {@code Decision_ovrigtBistand} (always 0, assessed manually)</li>
 * </ul>
 *
 * Best-effort: an unmapped cost type, an unavailable decision, or an empty result falls back to the applied amount with
 * the cost type's static bucket and no flag, so the daily prepare is never blocked.
 */
@Service
public class ExpenseRulesService {

	private static final String OUTPUT_APPROVED_AMOUNT = "approvedAmount";
	private static final String OUTPUT_BUCKET = "bucket";
	private static final String OUTPUT_VARNING = "varning";
	private static final String OUTPUT_REGEL = "regel";

	/** No previous-month approved amount → the sentinel the rule tree reads as "history missing". */
	private static final BigDecimal NO_HISTORY = BigDecimal.valueOf(-1);

	/** financial assistance cost type → its own decision key in the engine. */
	private static final Map<String, String> DECISION_KEY_BY_COST_TYPE = Map.ofEntries(
		Map.entry("RENT", "Decision_hyra"),
		Map.entry("HOME_INSURANCE", "Decision_hemforsakring"),
		Map.entry("ELECTRICITY", "Decision_hushallsel"),
		Map.entry("INTERNET", "Decision_internet"),
		Map.entry("UNEMPLOYMENT_FUND", "Decision_akasseavgift"),
		Map.entry("UNION_FEE", "Decision_fackavgift"),
		Map.entry("TRAVEL_APPROVED", "Decision_resor"),
		Map.entry("TRAVEL_MEDICAL_TRANSPORT", "Decision_sjukresor"),
		Map.entry("MEDICAL_CARE", "Decision_halsosjukvard"),
		Map.entry("MEDICINE", "Decision_medicin"),
		Map.entry("OTHER", "Decision_ovrigtBistand"));

	/**
	 * financial assistance cost type → the FC array it posts to (the static counterpart of each decision's bucket output).
	 */
	private static final Map<String, String> BUCKET_BY_COST_TYPE = Map.ofEntries(
		Map.entry("RENT", BUCKET_EXPENSE),
		Map.entry("ELECTRICITY", BUCKET_EXPENSE),
		Map.entry("HOME_INSURANCE", BUCKET_EXPENSE),
		Map.entry("INTERNET", BUCKET_EXPENSE),
		Map.entry("UNEMPLOYMENT_FUND", BUCKET_EXPENSE),
		Map.entry("UNION_FEE", BUCKET_EXPENSE),
		Map.entry("TRAVEL_APPROVED", BUCKET_EXPENSE),
		Map.entry("TRAVEL_MEDICAL_TRANSPORT", BUCKET_SPECIAL_EXPENSE),
		Map.entry("MEDICAL_CARE", BUCKET_SPECIAL_EXPENSE),
		Map.entry("MEDICINE", BUCKET_SPECIAL_EXPENSE),
		Map.entry("OTHER", BUCKET_SPECIAL_EXPENSE));

	private static final Logger LOG = LoggerFactory.getLogger(ExpenseRulesService.class);

	private final ProcessService processService;

	ExpenseRulesService(final ProcessService processService) {
		this.processService = processService;
	}

	/**
	 * The rules verdict for a cost — the process (reasonable) amount, the FC bucket it posts to, and the manual-review
	 * flag: {@code varning} true when the cost needs a reasonableness assessment, with {@code regel} the human-readable
	 * reason.
	 */
	public record ExpenseVerdict(BigDecimal processAmount, String bucket, boolean varning, String regel) {}

	/** The FC array a cost type posts to (best-effort {@code EXPENSE} for an unmapped type). */
	public static String bucketForCostType(final String costType) {
		return BUCKET_BY_COST_TYPE.getOrDefault(costType, BUCKET_EXPENSE);
	}

	/**
	 * The rules verdict for a cost, evaluated through its per-type decision (the rent rule tree). Falls back to the
	 * applied amount + the cost type's static bucket + unflagged when the cost type is unmapped, the decision is
	 * unavailable, or it returns nothing.
	 *
	 * @param  municipalityId   the municipality the errand belongs to
	 * @param  costType         the financial assistance cost type (e.g. RENT, MEDICINE)
	 * @param  appliedAmount    what the citizen applied for — the fallback and, in the rule tree, the upper bound
	 * @param  previousApproved the approved amount for this cost type on the previous month's calculation (may be
	 *                          {@code null} → treated as "history missing")
	 * @param  sokandeAlder     the applicant's age (rent threshold input; ignored by other types, may be {@code null})
	 * @param  antalBarn        number of children in the household (rent threshold input, may be {@code null})
	 * @param  antalIHushallet  number of persons in the household (home-insurance threshold input, may be {@code null})
	 * @return                  the verdict (process amount + bucket + review flag + reason), best-effort
	 */
	public ExpenseVerdict verdict(final String municipalityId, final String costType, final BigDecimal appliedAmount,
		final BigDecimal previousApproved, final Integer sokandeAlder, final Integer antalBarn, final Integer antalIHushallet) {

		final var decisionKey = DECISION_KEY_BY_COST_TYPE.get(costType);
		if (decisionKey == null) {
			return new ExpenseVerdict(appliedAmount, bucketForCostType(costType), false, null);
		}

		try {
			final var variables = new HashMap<String, Object>();
			variables.put("ansoktBelopp", Optional.ofNullable(appliedAmount).orElse(BigDecimal.ZERO));
			variables.put("godkandForra", Optional.ofNullable(previousApproved).orElse(NO_HISTORY));
			variables.put("sokandeAlder", Optional.ofNullable(sokandeAlder).orElse(0));
			variables.put("antalBarn", Optional.ofNullable(antalBarn).orElse(0));
			variables.put("antalIHushallet", Optional.ofNullable(antalIHushallet).orElse(1));

			final var rows = processService.evaluateDecision(municipalityId, decisionKey, variables);
			if (rows.isEmpty()) {
				return new ExpenseVerdict(appliedAmount, bucketForCostType(costType), false, null);
			}
			final var row = rows.getFirst();
			final var approved = row.get(OUTPUT_APPROVED_AMOUNT);
			final var amount = Optional.ofNullable(approved).map(value -> new BigDecimal(value.toString())).orElse(appliedAmount);
			final var bucket = Optional.ofNullable(row.get(OUTPUT_BUCKET)).map(Object::toString).orElseGet(() -> bucketForCostType(costType));
			return new ExpenseVerdict(amount, bucket, Boolean.TRUE.equals(row.get(OUTPUT_VARNING)), str(row.get(OUTPUT_REGEL)));
		} catch (final RuntimeException e) {
			LOG.warn("Expense rules ({}) unavailable — using the applied amount + {} bucket", decisionKey, bucketForCostType(costType), e);
			return new ExpenseVerdict(appliedAmount, bucketForCostType(costType), false, null);
		}
	}

	private static String str(final Object value) {
		return Optional.ofNullable(value).map(Object::toString).orElse(null);
	}
}
