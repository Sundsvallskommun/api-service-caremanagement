package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The family on the person's most recent calculation in Lifecare before the application month — what the återansökan
 * regelverk copies into the new normberäkning (“Kopiera följande från föregående normberäkning: Norm, Familj,
 * Gemensamma kostnader”).
 *
 * <p>
 * FamilyCare's read model carries less than its write model, so not everything can be copied: per member it gives the
 * person, the name, the norm amount and a deviation period, but not the role or the number of days the write takes
 * ({@code NumberOfDays}); for the household it gives the common-cost amount ({@code CommonHouseholdCost}) but not
 * whether a custom household size was set, nor which. Callers flag those for the caseworker rather than guess them.
 * </p>
 *
 * <p>
 * {@code complete} is {@code false} when a member's identity could not be resolved to a {@code partyId}: the list is
 * then short of someone it cannot name, and a caller building the household from it has to fall back rather than
 * silently drop a person. Empty ({@link #empty()}) when there is no prior calculation.
 * </p>
 *
 * @param members             the calculation's members, in Lifecare's order
 * @param complete            whether every member's identity resolved to a {@code partyId}
 * @param commonHouseholdCost the calculation's gemensamma hushållskostnader, {@code null} when it carried none
 */
public record PreviousFamily(List<Member> members, boolean complete, BigDecimal commonHouseholdCost) {

	/**
	 * One member of the previous calculation.
	 *
	 * @param partyId       the member's party id, {@code null} when it could not be resolved
	 * @param name          the name as Lifecare stores it
	 * @param deviationFrom the start of the member's deviating period in that calculation, {@code null} when none
	 * @param deviationTo   the end of the member's deviating period, {@code null} when none or open-ended
	 */
	public record Member(String partyId, String name, LocalDate deviationFrom, LocalDate deviationTo) {

		/** Whether the member was only part of the previous calculation for a deviating period. */
		public boolean hasDeviation() {
			return (deviationFrom != null) || (deviationTo != null);
		}
	}

	public static PreviousFamily empty() {
		return new PreviousFamily(List.of(), true, null);
	}

	public boolean isEmpty() {
		return members.isEmpty();
	}
}
