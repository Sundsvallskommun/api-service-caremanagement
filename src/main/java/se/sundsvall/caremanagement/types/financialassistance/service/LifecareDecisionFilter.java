package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;

import static org.springframework.util.StringUtils.hasText;

/**
 * Picks the ekonomiskt bistånd decisions out of a person's Lifecare decision list. FamilyCare answers with every
 * decision registered on the person across IFO — Vux, BoU, LVM, the 14 kap utredningsbeslut — and an EB handläggare
 * is to see only the EB ones.
 *
 * <p>
 * A decision is an EB decision when it was made under one of the configured EB services (Lifecare's {@code ServiceId},
 * {@code 2} in Lifecare test). The service id is a Lifecare catalogue id, not our own, so it is configured per
 * environment rather than hardcoded. Matching on the decision's type text was rejected: the texts are free catalogue
 * names, and the förskott-på-förmån decisions the previous-decision warning depends on must not fall outside a text
 * pattern nobody has verified.
 * </p>
 */
@Component
public class LifecareDecisionFilter {

	private final Set<Integer> serviceIds;

	LifecareDecisionFilter(@Value("${financial-assistance.lifecare.decision-service-ids:2}") final Set<Integer> serviceIds) {
		this.serviceIds = Set.copyOf(serviceIds);
	}

	/** Whether the decision was made under an ekonomiskt bistånd service — the filter for the case-history listing. */
	public boolean isFinancialAssistance(final DecisionView decision) {
		return decision.serviceId() != null && serviceIds.contains(decision.serviceId());
	}

	/**
	 * Whether the decision can be the previous decision the decision proposal builds on: an EB decision that covers a
	 * period. That leaves out the EB utredningsbeslut (”Beslut om ekonomiskt bistånd under nuvarande förhållande”),
	 * which is made under the EB service but carries no period, orsak or amount.
	 */
	public boolean isPreviousDecisionCandidate(final DecisionView decision) {
		return isFinancialAssistance(decision) && hasText(decision.fromDate());
	}
}
