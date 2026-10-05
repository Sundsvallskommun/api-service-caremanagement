package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Removes a normberäkning careM created in Lifecare and then lost the errand's link to. The prepare step and the
 * caseworker's first save can both create the errand's beräkning, and only the first to link it keeps it
 * ({@code FinancialAssistanceRepository.linkLifecareCalculationIfAbsent}); the other one would otherwise stay in
 * Lifecare unlinked. FamilyCare has no delete, so the removal goes through ProfessionalWeb, the way Ta bort does in
 * Lifecare's own list.
 *
 * <p>
 * Best-effort: a removal Lifecare refuses or never answers is logged and left as it was, the same outcome as before
 * there was a removal at all.
 * </p>
 */
@Component
public class SurplusCalculationRemover {

	private static final Logger LOG = LoggerFactory.getLogger(SurplusCalculationRemover.class);

	private final LifecareCalculationClient client;

	SurplusCalculationRemover(final LifecareCalculationClient client) {
		this.client = client;
	}

	/**
	 * Removes the beräkning from Lifecare.
	 *
	 * @param  calculationId a beräkning careM created and lost the link race for
	 * @return               true when Lifecare removed it, false when it is left in Lifecare unlinked
	 */
	public boolean remove(final int calculationId) {
		try {
			client.delete(calculationId);
			LOG.info("Removed the surplus normberäkning {} from Lifecare", calculationId);
			return true;
		} catch (final RuntimeException e) {
			// The exception type only: Lifecare's error detail may echo the calculation.
			LOG.warn("Could not remove the surplus normberäkning {} from Lifecare ({}); it is left there unlinked", calculationId, e.getClass().getSimpleName());
			return false;
		}
	}
}
