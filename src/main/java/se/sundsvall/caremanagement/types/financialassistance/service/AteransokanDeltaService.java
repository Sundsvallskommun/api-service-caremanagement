package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

/**
 * The återansökan delta-regelverk — classifies a single household/boende change against the föregående normberäkning.
 * It
 * evaluates the modeler-editable {@code Decision_ateransokanDelta} DMN in the operaton engine (the same place the
 * income
 * rålista and the utgiftsregelverk live) for a change kind and its magnitude, and returns whether the change is worth
 * flagging ({@code varning}) plus the human-readable notering ({@code regel}). The delta itself (what changed, by how
 * much) is computed care-side; this only decides severity/whether to surface — so a small change can pass silently
 * while
 * a large one is flagged. Best-effort: when the decision is unavailable or returns nothing, nothing is flagged so the
 * daily prepare is never blocked.
 */
@Service
public class AteransokanDeltaService {

	static final String DECISION_KEY = "Decision_ateransokanDelta";
	private static final String OUTPUT_VARNING = "varning";
	private static final String OUTPUT_REGEL = "regel";

	private static final Logger LOG = LoggerFactory.getLogger(AteransokanDeltaService.class);

	private final ProcessService processService;

	AteransokanDeltaService(final ProcessService processService) {
		this.processService = processService;
	}

	/** The delta verdict — whether to flag the change and the notering describing it. */
	public record DeltaVerdict(boolean varning, String regel) {}

	/**
	 * Classify a household/boende change against the previous normberäkning.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  changeKind     the kind of change (e.g. {@code HOUSEHOLD_SIZE}, {@code HOUSING_COST})
	 * @param  changeCount    the signed count delta (members added/removed); 0 when not applicable
	 * @param  changePercent  the signed percent delta (boende cost); 0 when not applicable
	 * @return                the verdict (flag + notering), best-effort
	 */
	public DeltaVerdict classify(final String municipalityId, final String changeKind, final int changeCount, final BigDecimal changePercent) {
		try {
			final var variables = new HashMap<String, Object>();
			variables.put("changeKind", nz(changeKind));
			variables.put("changeCount", changeCount);
			variables.put("changePercent", changePercent == null ? BigDecimal.ZERO : changePercent);

			final var rows = processService.evaluateDecision(municipalityId, DECISION_KEY, variables);
			if (rows.isEmpty()) {
				return new DeltaVerdict(false, null);
			}
			final var row = rows.getFirst();
			return new DeltaVerdict(Boolean.TRUE.equals(row.get(OUTPUT_VARNING)), str(row.get(OUTPUT_REGEL)));
		} catch (final RuntimeException e) {
			LOG.warn("Återansökan delta regelverk ({}) unavailable — skipping the delta flag", DECISION_KEY, e);
			return new DeltaVerdict(false, null);
		}
	}

	private static String str(final Object value) {
		return value == null ? null : value.toString();
	}

	private static String nz(final String value) {
		return value == null ? "" : value;
	}
}
