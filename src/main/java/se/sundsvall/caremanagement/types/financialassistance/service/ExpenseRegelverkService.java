package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

/**
 * The utgift (expense) regelverk — the process-decided amount for a cost. It evaluates the modeler-editable
 * {@code Decision_utgiftRegelverk} DMN in the operaton engine (the same place the income rålista lives) for the cost
 * type in its household context and returns the allowed (capped) amount. Best-effort: when the decision is unavailable
 * or returns nothing, the applied amount is used as the process amount so the daily prepare is never blocked — the cap
 * simply has no effect until the decision is deployed.
 */
@Service
public class ExpenseRegelverkService {

	static final String DECISION_KEY = "Decision_utgiftRegelverk";
	private static final String OUTPUT_APPROVED_AMOUNT = "approvedAmount";

	private static final Logger LOG = LoggerFactory.getLogger(ExpenseRegelverkService.class);

	private final ProcessService processService;

	ExpenseRegelverkService(final ProcessService processService) {
		this.processService = processService;
	}

	/**
	 * The process-decided (capped) amount for a cost, or the applied amount when the regelverk allows it in full or is
	 * unavailable.
	 *
	 * @param  municipalityId     the municipality the errand belongs to
	 * @param  costType           the EB cost type (e.g. RENT, ELECTRICITY)
	 * @param  otherSubType       the övrigt-bistånd sub-type, when the cost type is OTHER (may be {@code null})
	 * @param  housingForm        the household's housing form (cap context, may be {@code null})
	 * @param  housingPersonCount the household adult count (cap context, may be {@code null})
	 * @param  normType           the norm type (RIKSNORM/OTHER_NORM, may be {@code null})
	 * @param  appliedAmount      what the citizen applied for — the fallback and upper bound
	 * @return                    the capped amount, or the applied amount when not capped / unavailable
	 */
	public BigDecimal cap(final String municipalityId, final String costType, final String otherSubType, final String housingForm,
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
				return appliedAmount;
			}
			final var approved = rows.getFirst().get(OUTPUT_APPROVED_AMOUNT);
			return approved == null ? appliedAmount : new BigDecimal(approved.toString());
		} catch (final RuntimeException e) {
			LOG.warn("Expense regelverk ({}) unavailable — using the applied amount as the process amount", DECISION_KEY, e);
			return appliedAmount;
		}
	}

	private static String nz(final String value) {
		return value == null ? "" : value;
	}
}
