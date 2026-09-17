package se.sundsvall.caremanagement.lifecare.service.mapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Resolves a FamilyCare (Lifecare normberäkning) income-type <em>name</em> back to the financial assistance
 * {@code Income.incomeType} it corresponds to — the reverse direction of
 * {@link ApplicationIncomeToFamilyCareMapper#APPLICATION_TYPE_TO_FC_NAME}, used to read a previous calculation's
 * amounts per application income type for the återansökan income comparison
 * ({@code Decision_inkomstMotForegaende}).
 *
 * <p>
 * Only the four income types that rule compares are mapped: {@code SALARY},
 * {@code OCCUPATIONAL_PENSION_INSURANCE}, {@code CHILD_SUPPORT} and {@code RENT_SHARE_FROM_CHILD}. Matching is a
 * case-insensitive substring test against an <em>ordered</em> fragment table (first match wins), the same
 * best-effort style {@code LifecareCaseService#isHousing} uses — FamilyCare's exact catalogue names are not
 * confirmed against a real payload, so the fragments come from the verksamhet's regelverk table ("Tabell med övriga
 * inkomster": Lön → "Lön", Tjänstepension → "PLV", Underhållsbidrag → "Underhållsstöd") plus the names the forward
 * map already posts to.
 * </p>
 *
 * <p>
 * The table deliberately <strong>under</strong>-matches rather than over-matches: a bare {@code "pension"} fragment
 * would swallow unrelated Lifecare rows (Barnpension, Ålderspension, Garantipension) and raise a false
 * "income missing versus the previous calculation" warning on every errand carrying one, so only the specific
 * pension names are matched and {@code SKIP} entries short-circuit the ones that must never map. A name that
 * resolves to nothing simply means the comparison sees no previous amount for that type.
 * </p>
 */
public final class IncomeTypeMapper {

	/** Sentinel for a name that must resolve to nothing even though a later fragment would match it. */
	private static final String SKIP = "";

	/**
	 * Lowercased FamilyCare income-type name fragment → financial assistance income type, in match order (first hit
	 * wins). Edit here when the FamilyCare catalogue is confirmed.
	 */
	private static final Map<String, String> INCOME_TYPE_BY_NAME_FRAGMENT = orderedFragments();

	private IncomeTypeMapper() {}

	private static Map<String, String> orderedFragments() {
		final var fragments = new LinkedHashMap<String, String>();
		fragments.put("barnpension", SKIP);
		fragments.put("lön", "SALARY");
		fragments.put("plv", "OCCUPATIONAL_PENSION_INSURANCE");
		fragments.put("tjänstepension", "OCCUPATIONAL_PENSION_INSURANCE");
		fragments.put("pension/sa", "OCCUPATIONAL_PENSION_INSURANCE");
		fragments.put("underhållsstöd", "CHILD_SUPPORT");
		fragments.put("underhållsbidrag", "CHILD_SUPPORT");
		fragments.put("hyresdel", "RENT_SHARE_FROM_CHILD");
		return Collections.unmodifiableMap(fragments);
	}

	/** The fragment table, exposed so the test can assert the declared match order. */
	static Map<String, String> incomeTypeByNameFragment() {
		return INCOME_TYPE_BY_NAME_FRAGMENT;
	}

	/**
	 * The financial assistance income type a FamilyCare income-type name maps to (e.g. "Lön efter skatt" →
	 * {@code SALARY}), or empty when the name is not one of the four compared types.
	 *
	 * @param  familyCareName the FamilyCare income-type name as the calculation reports it (may be {@code null})
	 * @return                the financial assistance income type, or empty
	 */
	public static Optional<String> incomeTypeForFamilyCareName(final String familyCareName) {
		return ofNullable(familyCareName)
			.map(MapperUtil::normalize)
			.flatMap(IncomeTypeMapper::matchFragment);
	}

	private static Optional<String> matchFragment(final String normalized) {
		for (final var entry : INCOME_TYPE_BY_NAME_FRAGMENT.entrySet()) {
			if (normalized.contains(entry.getKey())) {
				return Optional.of(entry.getValue()).filter(type -> !SKIP.equals(type));
			}
		}
		return Optional.empty();
	}
}
