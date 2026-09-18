package se.sundsvall.caremanagement.lifecare.service.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

/**
 * Resolves a FamilyCare (Lifecare normberäkning) income-type <em>name</em> back to the financial assistance
 * {@code Income.incomeType} it corresponds to — the reverse direction of
 * {@link ApplicationIncomeToFamilyCareMapper#APPLICATION_TYPE_TO_FC_NAME}, used to read a previous calculation's
 * amounts per application income type for the återansökan income comparison
 * ({@code Decision_inkomstMotForegaende}).
 *
 * <p>
 * The table is <strong>derived by inverting the forward map</strong>, so the two can no longer disagree. It replaced a
 * hand-written substring table whose fragments were guessed from the verksamhet's regelverk ("PLV",
 * "Tjänstepension", "Underhållsbidrag", "Hyresdel"); against the real FamilyCare catalogue (live
 * {@code Calculations/Proposals}, 2026-09-18) <em>none of those four names exists</em>, so those fragments could
 * never match. Matching is now on the whole name, case- and trim-insensitive, because the catalogue's 29 names are
 * known exactly.
 * </p>
 *
 * <p>
 * <strong>A FamilyCare name claimed by more than one income type is dropped rather than resolved arbitrarily.</strong>
 * That is not a corner case: {@code RENT_SHARE_FROM_CHILD} (hyresdel från barn), {@code OTHER_INCOME} and
 * {@code FINANCIAL_AID_OTHER_MUNICIPALITY} all post to the single FamilyCare row "Övriga inkomster", because the
 * catalogue has no hyresdel row at all. Once written, the three are indistinguishable, so reading a hyresdel amount
 * back out of a previous normberäkning is impossible by construction — the hyresdel branch of
 * {@code Decision_inkomstMotForegaende} cannot fire, and cannot be made to fire without either a dedicated
 * FamilyCare income type or a convention on the row's {@code Note}. A name that resolves to nothing simply means the
 * comparison sees no previous amount for that type.
 * </p>
 */
public final class IncomeTypeMapper {

	/**
	 * Normalized FamilyCare income-type name → financial assistance income type. Derived from the forward map, minus
	 * the names more than one income type posts to.
	 */
	private static final Map<String, String> INCOME_TYPE_BY_NAME = invertUnambiguous();

	private IncomeTypeMapper() {}

	private static Map<String, String> invertUnambiguous() {
		final var forward = ApplicationIncomeToFamilyCareMapper.APPLICATION_TYPE_TO_FC_NAME;
		final Set<String> ambiguous = forward.values().stream()
			.map(MapperUtil::normalize)
			.collect(groupingBy(identity(), counting()))
			.entrySet().stream()
			.filter(entry -> entry.getValue() > 1)
			.map(Map.Entry::getKey)
			.collect(toSet());

		final var byName = new LinkedHashMap<String, String>();
		forward.forEach((incomeType, familyCareName) -> {
			final var normalized = MapperUtil.normalize(familyCareName);
			if (!ambiguous.contains(normalized)) {
				byName.put(normalized, incomeType);
			}
		});
		return unmodifiableMap(byName);
	}

	/** The resolved table, exposed so the test can assert what does and does not map. */
	static Map<String, String> incomeTypeByName() {
		return INCOME_TYPE_BY_NAME;
	}

	/**
	 * The financial assistance income type a FamilyCare income-type name maps to (e.g. "Lön efter skatt" →
	 * {@code SALARY}), or empty when the name is not one of the unambiguously mapped types.
	 *
	 * @param  familyCareName the FamilyCare income-type name as the calculation reports it (may be {@code null})
	 * @return                the financial assistance income type, or empty
	 */
	public static Optional<String> incomeTypeForFamilyCareName(final String familyCareName) {
		return ofNullable(familyCareName)
			.map(MapperUtil::normalize)
			.map(INCOME_TYPE_BY_NAME::get);
	}
}
