package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.util.Set;

/**
 * The household on the person's most recent previous calculation in Lifecare — the baseline the current application's
 * household is compared against to warn on drift (members added/removed, count or norm changed, housing cost changed).
 * {@code housingCost} is the previous housing (Rent) expense, used for the renewal housing-delta check; it may be
 * {@code null} when the previous calculation carried no identifiable housing cost. {@code norm} is FamilyCare's
 * free-text norm on that calculation (e.g. "Riksnorm"), used for the återansökan norm comparison; {@code null} when the
 * calculation carried none. Empty ({@code memberCount == 0}) when there is no prior calculation or the lookup failed
 * (best-effort).
 *
 * <p>
 * {@code partyIds} are party ids whichever FamilyCare route answered — the integrator says so already, the direct
 * one answers with personal identity numbers and {@code LifecareCaseService} resolves them. They are compared against
 * the errand's own members, which careM holds by party id, so the integrator route needs no lookup at all.
 * {@code partyIdsComplete} is {@code false} when at least one member could not be resolved; the set is then short of
 * a member it cannot name, so a caller comparing households member by member has to skip rather than report the
 * difference as a real one. {@code memberCount} is the previous calculation's own member count and stays right either
 * way.
 */
public record PreviousHousehold(
	Set<String> partyIds,
	boolean partyIdsComplete,
	int memberCount,
	BigDecimal normSum,
	BigDecimal housingCost,
	String norm) {

	public static PreviousHousehold empty() {
		return new PreviousHousehold(Set.of(), true, 0, null, null, null);
	}
}
