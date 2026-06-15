package se.sundsvall.caremanagement.lifecare.integration;

import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefc.PostCalculationBodyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Thin wrapper over {@link LifecareFcClient}. Translates any transport/FC failure into a {@code BAD_GATEWAY} problem,
 * carrying the upstream status into the log and problem detail. Logs no {@code personId} or payloads — FC carries
 * personnummer and income data (sprint privacy rule, vof-ekonomiskt-bistand/CLAUDE.md).
 */
@Component
public class LifecareFcIntegration {

	public static final String CLIENT_ID = LifecareFcClient.class.getSimpleName();

	private static final Logger LOG = LoggerFactory.getLogger(LifecareFcIntegration.class);

	private final LifecareFcClient lifecareFcClient;

	public LifecareFcIntegration(final LifecareFcClient lifecareFcClient) {
		this.lifecareFcClient = lifecareFcClient;
	}

	public PersonBasedAktualiseringProposalDTO getActualisationProposal(final String personId) {
		try {
			LOG.debug("Fetching aktualisering proposal from Lifecare FC");
			return lifecareFcClient.getActualisationProposal(personId);
		} catch (final Exception e) {
			LOG.error("Error fetching aktualisering proposal from Lifecare FC: {}", describe(e), e);
			throw Problem.valueOf(BAD_GATEWAY, "Error fetching aktualisering proposal from Lifecare FC: %s".formatted(describe(e)));
		}
	}

	public Integer createActualisation(final PostAktualiseringsBodyRequest body) {
		try {
			final var id = lifecareFcClient.createActualisation(body);
			LOG.debug("Created aktualisering {} in Lifecare FC", id);
			return id;
		} catch (final Exception e) {
			LOG.error("Error creating aktualisering in Lifecare FC: {}", describe(e), e);
			throw Problem.valueOf(BAD_GATEWAY, "Error creating aktualisering in Lifecare FC: %s".formatted(describe(e)));
		}
	}

	public PersonBasedCalculationProposalDTO getCalculationProposal(final String personId) {
		try {
			LOG.debug("Fetching calculation proposal from Lifecare FC");
			return lifecareFcClient.getCalculationProposal(personId);
		} catch (final Exception e) {
			LOG.error("Error fetching calculation proposal from Lifecare FC: {}", describe(e), e);
			throw Problem.valueOf(BAD_GATEWAY, "Error fetching calculation proposal from Lifecare FC: %s".formatted(describe(e)));
		}
	}

	public Integer createCalculation(final PostCalculationBodyRequest body) {
		try {
			final var id = lifecareFcClient.createCalculation(body);
			LOG.debug("Created calculation {} in Lifecare FC", id);
			return id;
		} catch (final Exception e) {
			LOG.error("Error creating calculation in Lifecare FC: {}", describe(e), e);
			throw Problem.valueOf(BAD_GATEWAY, "Error creating calculation in Lifecare FC: %s".formatted(describe(e)));
		}
	}

	/** Short upstream descriptor (HTTP status when available) to make failures self-diagnosing without leaking payloads. */
	private static String describe(final Throwable e) {
		if (e instanceof final ThrowableProblem problem) {
			return ofNullable(problem.getStatus()).map(status -> status.value() + " " + problem.getMessage()).orElseGet(problem::getMessage);
		}
		return e.getClass().getSimpleName() + ": " + e.getMessage();
	}
}
