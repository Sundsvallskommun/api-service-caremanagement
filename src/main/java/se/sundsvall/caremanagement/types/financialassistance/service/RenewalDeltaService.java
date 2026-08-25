package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

/**
 * The renewal delta-rules — classifies a single household/housing change against the previous calculation.
 * It
 * evaluates the modeler-editable {@code Decision_ateransokanDelta} DMN in the operaton engine (the same place the
 * income
 * raw list and the expense rules live) for a change kind and its magnitude, and returns whether the change is worth
 * flagging ({@code warning}) plus the human-readable note ({@code rule}). The delta itself (what changed, by how
 * much) is computed care-side; this only decides severity/whether to surface — so a small change can pass silently
 * while
 * a large one is flagged. Best-effort: when the decision is unavailable or returns nothing, nothing is flagged so the
 * daily prepare is never blocked.
 */
@Service
public class RenewalDeltaService {

	static final String DECISION_KEY = "Decision_ateransokanDelta";
	private static final String OUTPUT_VARNING = "varning";
	private static final String OUTPUT_REGEL = "regel";

	private static final Logger LOG = LoggerFactory.getLogger(RenewalDeltaService.class);

	private final ProcessService processService;

	RenewalDeltaService(final ProcessService processService) {
		this.processService = processService;
	}

	/** The delta verdict — whether to flag the change and the note describing it. */
	public record DeltaVerdict(boolean warning, String rule) {}

	/**
	 * Classify a household/housing change against the previous calculation.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  changeKind     the kind of change (e.g. {@code HOUSEHOLD_SIZE}, {@code HOUSING_COST})
	 * @param  changeCount    the signed count delta (members added/removed); 0 when not applicable
	 * @param  changePercent  the signed percent delta (housing cost); 0 when not applicable
	 * @return                the verdict (flag + note), best-effort
	 */
	public DeltaVerdict classify(final String municipalityId, final String changeKind, final int changeCount, final BigDecimal changePercent) {
		try {
			final var variables = new HashMap<String, Object>();
			variables.put("changeKind", orEmpty(changeKind));
			variables.put("changeCount", changeCount);
			variables.put("changePercent", Optional.ofNullable(changePercent).orElse(BigDecimal.ZERO));

			final var rows = processService.evaluateDecision(municipalityId, DECISION_KEY, variables);
			if (rows.isEmpty()) {
				return new DeltaVerdict(false, null);
			}
			final var row = rows.getFirst();
			return new DeltaVerdict(Boolean.TRUE.equals(row.get(OUTPUT_VARNING)), str(row.get(OUTPUT_REGEL)));
		} catch (final RuntimeException e) {
			LOG.warn("Renewal delta rules ({}) unavailable — skipping the delta flag", DECISION_KEY, e);
			return new DeltaVerdict(false, null);
		}
	}

	private static String str(final Object value) {
		return Optional.ofNullable(value).map(Object::toString).orElse(null);
	}

	/** The value, or an empty string when it is {@code null}. */
	private static String orEmpty(final String value) {
		return Optional.ofNullable(value).orElse("");
	}
}
