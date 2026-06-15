package se.sundsvall.caremanagement.lifecare.service.mapper;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Optional.ofNullable;

/**
 * The Drakel whitelist of SSBTEK förmåner: which ones transfer to the FC normberäkning and under which FC income-type
 * name. Encoded from {@code vof-ekonomiskt-bistand/docs/ssbtek-regelverk.txt} ("Tabell med inkomster från SSBTEK", the
 * Förmån → Normberäkning column). A data-driven table on purpose — the regelverk's stated goal is to avoid hard-coding
 * the inverse, so anything not on the list is flagged rather than guessed.
 *
 * <p>
 * The FC income-type <em>names</em> here are matched against the names in the FC calculation proposal at runtime to
 * resolve the actual numeric type id, so this table never hard-codes ids. Names should be re-verified against a real FC
 * proposal once one is available.
 */
public final class SsbtekIncomeRegistry {

	/** SSBTEK förmån (normalised) → FC normberäkning income-type name. */
	private static final Map<String, String> INCLUDED = Map.ofEntries(
		Map.entry(normalize("Dagersättning"), "Dagersättning FK"),
		Map.entry(normalize("PM-Prel"), "PM-PREL"),
		Map.entry(normalize("PM"), "PM"),
		Map.entry(normalize("Bostadsbidrag"), "Bostadsbidrag"),
		Map.entry(normalize("Pension/SA/Livräntor/Vårdbidrag"), "PLV"),
		Map.entry(normalize("Allmänt barnbidrag"), "Barnbidrag"),
		Map.entry(normalize("Underhållsstöd"), "Underhållsstöd"),
		Map.entry(normalize("Studiemedel"), "Studiemedel"),
		Map.entry(normalize("Studiehjalp"), "Studiebidrag (gymn)"),
		Map.entry(normalize("Arbetslöshetsersättning"), "A-kassa/Alfa"),
		Map.entry(normalize("Elstöd"), "Elstöd"),
		Map.entry(normalize("Skattekontouppgift"), "Skatteåterbäring"));

	/** SSBTEK förmåner that are known but deliberately not transferred ("Ej ta med" / "Inget" in the regelverk). */
	private static final Set<String> EXCLUDED = Set.of(
		normalize("Handikappersättning"),
		normalize("Bostadskostnad"));

	private SsbtekIncomeRegistry() {}

	/**
	 * The FC normberäkning income-type name a förmån transfers to, if it is on the whitelist and is to be transferred.
	 *
	 * @param  forman the SSBTEK förmån
	 * @return        the FC income-type name, or empty if the förmån is excluded or unknown
	 */
	public static Optional<String> fcNormberakningType(final String forman) {
		return ofNullable(forman).map(SsbtekIncomeRegistry::normalize).map(INCLUDED::get);
	}

	/**
	 * Whether a förmån is a known, deliberate exclusion (so it should be skipped silently rather than warned about).
	 *
	 * @param  forman the SSBTEK förmån
	 * @return        {@code true} if the förmån is on the explicit exclusion list
	 */
	public static boolean isExcluded(final String forman) {
		return ofNullable(forman).map(SsbtekIncomeRegistry::normalize).map(EXCLUDED::contains).orElse(false);
	}

	/** Trim + lower-case for case-/whitespace-insensitive matching of SSBTEK and FC names. */
	public static String normalize(final String value) {
		return ofNullable(value).map(String::trim).map(trimmed -> trimmed.toLowerCase(Locale.ROOT)).orElse("");
	}
}
