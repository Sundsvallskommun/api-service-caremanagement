package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.TERMINAL_STATUSES;

/**
 * Tells whether an errand has ended — withdrawn, rejected or closed — so that what the process does daily for an
 * errand in progress is not done for it. A withdrawn errand's process is ended by message
 * ({@link se.sundsvall.caremanagement.operaton.service.ProcessService#MESSAGE_ERRAND_WITHDRAWN}), but that takes a
 * moment, and a run that is already on its way, or a process the message could not reach yet, must not read Lifecare
 * for the errand, write to it or move its status.
 *
 * <p>
 * Read outside the caller's transaction, and before it has read anything: the daily preparation resolves the
 * errand's Lifecare insats in a transaction of its own first, and a read taken inside its transaction ahead of that
 * would fix the transaction's snapshot older than the errand row that write changes — which MariaDB then refuses the
 * preparation's own later write to ({@code 1020}). See {@link LifecareServiceIdService}, which is called for the same
 * reason.
 * </p>
 */
@Component
public class EndedErrandGate {

	private final ErrandQueryService errandQueryService;

	EndedErrandGate(final ErrandQueryService errandQueryService) {
		this.errandQueryService = errandQueryService;
	}

	/**
	 * The errand's status when it is one of the terminal ones ({@code CLOSED}, {@code WITHDRAWN}, {@code REJECTED}),
	 * otherwise empty — also for an errand that does not exist, which the caller's own scope check answers with a 404.
	 */
	@Transactional(propagation = NOT_SUPPORTED)
	public Optional<String> endedStatus(final String municipalityId, final String namespace, final String errandId) {
		return errandQueryService.findErrand(municipalityId, namespace, errandId)
			.map(Errand::getStatus)
			.filter(TERMINAL_STATUSES::contains);
	}
}
