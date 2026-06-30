package se.sundsvall.caremanagement.financialaid.integration;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Thin wrapper over {@link FinancialAidClient}. Translates any transport/financial-aid failure into a
 * {@code BAD_GATEWAY} problem carrying the upstream status into the log and problem detail. Deliberately logs no
 * {@code personalNumber} and no part of the SSBTEK basis — both are sensitive (personnummer + income data); see the
 * sprint privacy rule in vof-ekonomiskt-bistand/CLAUDE.md.
 */
@Component
public class FinancialAidIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAidIntegration.class);

	private final FinancialAidClient financialAidClient;

	public FinancialAidIntegration(final FinancialAidClient financialAidClient) {
		this.financialAidClient = financialAidClient;
	}

	/**
	 * Fetch a person's aggregated SSBTEK basis for a period.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  personalNumber the person's full personnummer
	 * @param  fromDate       the period start (yyyy-MM-dd)
	 * @param  toDate         the period end (yyyy-MM-dd)
	 * @return                the per-agency basis as a nested, untyped JSON map
	 */
	public Map<String, Map<String, Object>> getFinancialAidBasis(final String municipalityId, final String personalNumber, final String fromDate, final String toDate) {
		try {
			LOG.debug("Fetching financial-aid basis");
			final var basis = financialAidClient.getFinancialAidBasis(municipalityId, personalNumber, fromDate, toDate);
			LOG.debug("Financial-aid basis fetched");
			return basis;
		} catch (final Exception e) {
			LOG.error("Error fetching financial-aid basis: {}", describe(e), e);
			throw Problem.valueOf(BAD_GATEWAY, "Error fetching financial-aid basis: %s".formatted(describe(e)));
		}
	}

	/**
	 * Short upstream descriptor (HTTP status when available) to make failures self-diagnosing without leaking payloads.
	 * For {@link ThrowableProblem} causes the (already-clean) status + detail is used; for any other cause only the
	 * exception class name is emitted — transport failures embed the full request URL in their message, which carries the
	 * personalNumber, so the message is deliberately dropped.
	 */
	private static String describe(final Throwable e) {
		if (e instanceof final ThrowableProblem problem) {
			return ofNullable(problem.getStatus()).map(status -> status.value() + " " + problem.getMessage()).orElseGet(problem::getMessage);
		}
		return e.getClass().getSimpleName();
	}
}
