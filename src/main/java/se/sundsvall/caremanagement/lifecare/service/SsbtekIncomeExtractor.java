package se.sundsvall.caremanagement.lifecare.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static java.util.Optional.ofNullable;

/**
 * Turns the financial-aid SSBTEK basis — the untyped, per-agency JSON map produced by a generic XML→JSON conversion of
 * the SSBTEK SOAP response — into normalised {@link SsbtekIncome}s for {@code SsbtekToFcIncomeMapper}. Keys mirror the
 * SSBTEK XML element names, and repeated elements arrive as either a single object or a list, so navigation is
 * defensive throughout. One SSBTEK basis is one person's data, so the caller supplies the {@link ApplicantRole}.
 *
 * <p>
 * Grounded agencies: <b>so</b> (arbetslöshetsersättning). The income-bearing <b>fk</b> förmånslista
 * (Dagersättning/Bostadsbidrag/Barnbidrag/PM/PLV/Underhållsstöd) and <b>csn</b> amounts are not present in any current
 * sample payload, so they are explicit extension points (marked in {@link #extract(Map, ApplicantRole)}) — they need a
 * real SSBTEK payload before they can be implemented without guessing field names. Non-income agencies (af/tns/miv, skv
 * capital) are intentionally not read.
 */
public final class SsbtekIncomeExtractor {

	private static final String AGENCY_SO = "so";

	private SsbtekIncomeExtractor() {}

	/**
	 * Extract the transferable incomes from one person's SSBTEK basis.
	 *
	 * @param  agencyBasis the per-agency SSBTEK basis (af/csn/fk/skv/so/tns/miv); may be {@code null}
	 * @param  role        whose basis this is (one SSBTEK call = one person)
	 * @return             the normalised incomes found
	 */
	public static List<SsbtekIncome> extract(final Map<String, ?> agencyBasis, final ApplicantRole role) {
		if (agencyBasis == null) {
			return List.of();
		}

		final var incomes = new ArrayList<SsbtekIncome>();
		incomes.addAll(extractArbetsloshetsersattning(asMap(agencyBasis.get(AGENCY_SO)), role));
		// Extension points (need a real SSBTEK payload before they can be grounded — see class note):
		// - fk: the förmånslista (Dagersättning/Bostadsbidrag/Barnbidrag/PM/PLV/Underhållsstöd), where most EB income lives
		// - csn: studiemedel/studiehjälp amounts
		return List.copyOf(incomes);
	}

	/**
	 * so → ArbetsloshetsersattningLista → Arbetsloshetsersattning(*) → Utbetalningar(*) → NettoEfterSkatt /
	 * Utbetalningsdatum. Förmån "Arbetslöshetsersättning" is on the whitelist (→ FC "A-kassa/Alfa").
	 */
	private static List<SsbtekIncome> extractArbetsloshetsersattning(final Map<String, Object> so, final ApplicantRole role) {
		final var incomes = new ArrayList<SsbtekIncome>();
		for (final var ersattning : asList(asMap(so.get("ArbetsloshetsersattningLista")).get("Arbetsloshetsersattning"))) {
			for (final var utbetalning : asList(asMap(ersattning).get("Utbetalningar"))) {
				final var payment = asMap(utbetalning);
				final var amount = decimal(payment.get("NettoEfterSkatt"));
				if (amount != null) {
					incomes.add(new SsbtekIncome("Arbetslöshetsersättning", null, null, amount, date(payment.get("Utbetalningsdatum")), role));
				}
			}
		}
		return incomes;
	}

	// ---- defensive untyped-map navigation -----------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(final Object value) {
		if (value instanceof final Map<?, ?> map) {
			return (Map<String, Object>) map;
		}
		return Map.of();
	}

	/** A repeated XML element is a single object or a list once converted to JSON; normalise both to a list. */
	private static List<Object> asList(final Object value) {
		if (value == null) {
			return List.of();
		}
		if (value instanceof final List<?> list) {
			return List.copyOf(list);
		}
		return List.of(value);
	}

	private static BigDecimal decimal(final Object value) {
		return ofNullable(value).map(Object::toString).map(String::trim).map(SsbtekIncomeExtractor::parseDecimal).orElse(null);
	}

	private static BigDecimal parseDecimal(final String text) {
		try {
			return new BigDecimal(text);
		} catch (final NumberFormatException e) {
			return null;
		}
	}

	private static LocalDate date(final Object value) {
		return ofNullable(value).map(Object::toString).map(String::trim).map(SsbtekIncomeExtractor::parseDate).orElse(null);
	}

	/** SSBTEK dates are ISO calendar dates; some carry a trailing time/offset, so parse the leading {@code yyyy-MM-dd}. */
	private static LocalDate parseDate(final String text) {
		if (text.length() < 10) {
			return null;
		}
		try {
			return LocalDate.parse(text.substring(0, 10));
		} catch (final RuntimeException e) {
			return null;
		}
	}
}
