package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.util.Comparator;
import java.util.Optional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.elements;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.hasNumber;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.isTrue;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.number;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.textOrEmpty;

/**
 * Picks the beräkning preceding the errand's own period from the insats's list in Lifecare.
 */
final class PreviousCalculationPicker {

	private static final String START_DATE = "startDate";

	/** Newer first: a later period, then, within the same period, a slutlig one, then the one made last. */
	private static final Comparator<ObjectNode> NEWER_FIRST = Comparator
		.comparing((ObjectNode calculation) -> textOrEmpty(calculation, START_DATE)).reversed()
		.thenComparing((ObjectNode calculation) -> isTrue(calculation, "isFinalized"), Comparator.reverseOrder())
		.thenComparing((ObjectNode calculation) -> number(calculation, "calculationId"), Comparator.reverseOrder());

	private PreviousCalculationPicker() {}

	/**
	 * Of the beräkningar starting before the period start, the latest period; within it a slutlig one before one still
	 * being worked on, then the newest. The errand's own beräkning is never its own predecessor. Without a period start
	 * the most recent other beräkning is taken.
	 *
	 * @param  calculations     ListCalculations' answer
	 * @param  periodStart      the errand's period start, yyyy-MM-dd, or null
	 * @param  ownCalculationId the errand's own beräkning, or null
	 * @return                  the previous beräkning, if any
	 */
	static Optional<ObjectNode> pick(final JsonNode calculations, final String periodStart, final Integer ownCalculationId) {
		return elements(calculations).stream()
			.filter(calculation -> ownCalculationId == null || !hasNumber(calculation, "calculationId", ownCalculationId))
			.filter(calculation -> !textOrEmpty(calculation, START_DATE).isEmpty())
			.filter(calculation -> periodStart == null || periodStart.isEmpty() || textOrEmpty(calculation, START_DATE).compareTo(periodStart) < 0)
			.min(NEWER_FIRST);
	}
}
