package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One person's SSBTEK basis as the composite service answered it, reduced to the payment fields the caseworker's
 * payment
 * view shows, plus the period the answer covers.
 *
 * <p>
 * {@code agencies} is the untyped per-agency map from api-service-financial-aid, cut down by caremanagement to the
 * allowlisted fields ({@code SsbtekPaymentViewFields}) before it is returned: the payments of fk (Försäkringskassan and
 * Pensionsmyndigheten) and so (the a-kassor). The agencies' answers carry personnummer, names and addresses, and those
 * never leave caremanagement (dataminimering). What is returned keeps the agency's own keys and shape — most agencies
 * are
 * a generic XML-to-JSON conversion of the SSBTEK SOAP response so their keys mirror the contract's XML element names,
 * while <b>fk</b> arrives as LEFI JSON with LEFI property names — and is not modelled here: the shapes are
 * heterogeneous
 * and version with the contract.
 * </p>
 *
 * <p>
 * The normalised, rule-classified reading of the same data is a different thing and lives elsewhere: the operaton
 * {@code evaluate-income-regelverk} worker produces it and it reaches the frontend as the calculation draft's income
 * rows. This model is the unclassified source view next to it.
 * </p>
 */
@Schema(description = "A person's SSBTEK basis for a period, as the composite service answered it, reduced to the payment fields the caseworker's payment view needs.")
public class SsbtekBasis {

	@Schema(description = "Inclusive start of the period the basis covers", examples = "2026-07-01")
	private LocalDate from;

	@Schema(description = "Inclusive end of the period the basis covers", examples = "2026-09-30")
	private LocalDate to;

	@Schema(description = "The answer per responding agency, reduced to the payment fields the caseworker's payment view needs: the payments of "
		+ "fk (Försäkringskassan and Pensionsmyndigheten) and so (the a-kassor), in the agency's own shape. Everything else in the answers "
		+ "— personnummer, names, addresses and the other agencies (af, csn, skv, tns, miv) — is dropped by caremanagement. "
		+ "Shapes differ per agency and are not modelled; an agency that did not answer, or has nothing to show, may be absent or empty.")
	private Map<String, Map<String, Object>> agencies;

	public static SsbtekBasis create() {
		return new SsbtekBasis();
	}

	public LocalDate getFrom() {
		return from;
	}

	public void setFrom(final LocalDate from) {
		this.from = from;
	}

	public SsbtekBasis withFrom(final LocalDate from) {
		this.from = from;
		return this;
	}

	public LocalDate getTo() {
		return to;
	}

	public void setTo(final LocalDate to) {
		this.to = to;
	}

	public SsbtekBasis withTo(final LocalDate to) {
		this.to = to;
		return this;
	}

	public Map<String, Map<String, Object>> getAgencies() {
		return agencies;
	}

	public void setAgencies(final Map<String, Map<String, Object>> agencies) {
		this.agencies = agencies;
	}

	public SsbtekBasis withAgencies(final Map<String, Map<String, Object>> agencies) {
		this.agencies = agencies;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof final SsbtekBasis that)) {
			return false;
		}
		return Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(agencies, that.agencies);
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, to, agencies);
	}

	/**
	 * Reports the period and which agencies answered, never the answers themselves. The agency payloads are income data
	 * for a named person, and a {@code toString()} is exactly how such a payload ends up in a log line by accident — the
	 * sprint rule is that no SSBTEK payload is ever logged.
	 */
	@Override
	public String toString() {
		final var agencyNames = Optional.ofNullable(agencies).map(Map::keySet).orElse(null);
		return "SsbtekBasis{from=" + from + ", to=" + to + ", agencies=" + Objects.toString(agencyNames) + "}";
	}
}
