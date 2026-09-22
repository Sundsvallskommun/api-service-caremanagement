package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/**
 * One person's SSBTEK basis as the composite service answered it, plus the period the answer covers.
 *
 * <p>
 * {@code agencies} is the untyped per-agency map caremanagement forwards verbatim from api-service-financial-aid
 * (af/csn/fk/skv/so/tns/miv). It is deliberately not modelled: most agencies are a generic XML-to-JSON conversion of
 * the SSBTEK SOAP response so their keys mirror the contract's XML element names, while <b>fk</b> arrives as LEFI JSON
 * with LEFI property names. The shapes are heterogeneous and version with the contract, so typing them here would
 * silently drop whatever we had not modelled — the point of this endpoint is that the caseworker sees what SSBTEK
 * actually said.
 * </p>
 *
 * <p>
 * The normalised, rule-classified reading of the same data is a different thing and lives elsewhere: the operaton
 * {@code evaluate-income-regelverk} worker produces it and it reaches the frontend as the calculation draft's income
 * rows. This model is the unclassified source view next to it.
 * </p>
 */
@Schema(description = "A person's SSBTEK basis for a period, as the composite service answered it.")
public class SsbtekBasis {

	@Schema(description = "Inclusive start of the period the basis covers", examples = "2026-07-01")
	private LocalDate from;

	@Schema(description = "Inclusive end of the period the basis covers", examples = "2026-09-30")
	private LocalDate to;

	@Schema(description = "The answer per responding agency (af, csn, fk, skv, so, tns, miv), forwarded verbatim from SSBTEK. "
		+ "Shapes differ per agency and are not modelled; an agency that did not answer may be absent or empty.")
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
		return "SsbtekBasis{from=" + from + ", to=" + to + ", agencies=" + Objects.toString(agencies == null ? null : agencies.keySet()) + "}";
	}
}
