package se.sundsvall.caremanagement.lifecare.service.model;

import java.util.Set;

/**
 * The household on the person's most recent previous normberäkning in Lifecare — the baseline the current application's
 * household is compared against to warn on drift (members added/removed, count or norm changed, boende cost changed).
 * {@code housingCost} is the previous boende (Hyra) expense, used for the återansökan boende-delta check; it may be
 * {@code null} when the previous calculation carried no identifiable boende cost. Empty ({@code memberCount == 0}) when
 * there is no prior normberäkning or the lookup failed (best-effort).
 */
public record PreviousHousehold(
	Set<String> personIds,
	int memberCount,
	Double normSum,
	Double housingCost) {

	public static PreviousHousehold empty() {
		return new PreviousHousehold(Set.of(), 0, null, null);
	}
}
