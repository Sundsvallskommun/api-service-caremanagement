package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static se.sundsvall.caremanagement.operaton.service.ProcessService.MESSAGE_ERRAND_WITHDRAWN;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_WITHDRAWN;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Ends the process of a financial assistance errand that is withdrawn. Without it the instance keeps running: it goes
 * on waiting for the decision and its daily loop goes on preparing an errand nobody is going to decide.
 *
 * <p>
 * On a transition to {@code WITHDRAWN} the {@code ErrandWithdrawn} message is correlated to the process by business key
 * (the errandId). Every one of the three financial assistance models catches it in an interrupting event subprocess, so
 * the instance ends wherever it waits. The process writes no status on the way out: the errand's status is careM's, and
 * it is already {@code WITHDRAWN}. Runs once the status change has committed
 * ({@link ApplicationModuleListener} = after-commit, async, new transaction), so the engine never learns of a
 * withdrawal that was rolled back.
 *
 * <p>
 * Like finalize's {@code PaymentDecisionReceived}, a message the engine could not take is queued for the scheduled
 * process message retry, which sends it again until the engine accepts it. What the engine answers when no instance
 * waits ({@code 404}) is not such a failure: an errand frozen for manual review has no process, and one whose process
 * already ended has nothing left to stop, so the errand is simply where the message was meant to leave it. That is
 * logged as information and nothing is queued; see {@link ProcessService#isNothingToEnd}. The retry worker settles a
 * queued message the same way.
 *
 * <p>
 * Only errands of the financial assistance types are looked at: another type that reaches {@code WITHDRAWN} has no such
 * process and no such message to catch.
 */
@Component
class FinancialAssistanceWithdrawalListener {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceWithdrawalListener.class);

	private final ProcessService processService;

	FinancialAssistanceWithdrawalListener(final ProcessService processService) {
		this.processService = processService;
	}

	@ApplicationModuleListener
	void endProcessOnWithdrawal(final ErrandStatusChanged event) {
		if (!STATUS_WITHDRAWN.equals(event.toStatus()) || FinancialAssistanceProcessStarter.processDefinitionName(event.typeSlug()).isEmpty()) {
			return;
		}
		final var errandId = event.errandId();
		try {
			processService.correlateMessage(event.municipalityId(), event.namespace(), MESSAGE_ERRAND_WITHDRAWN, errandId, Map.of());
			LOG.info("Ended the process of withdrawn errand {}", sanitizeForLogging(errandId));
		} catch (final RuntimeException e) {
			if (ProcessService.isNothingToEnd(MESSAGE_ERRAND_WITHDRAWN, e)) {
				LOG.info("Errand {} was withdrawn and has no running process to end", sanitizeForLogging(errandId));
				return;
			}
			LOG.warn("Could not correlate {} for errand {} - message queued for retry", sanitizeForLogging(MESSAGE_ERRAND_WITHDRAWN), sanitizeForLogging(errandId), e);
			processService.queueMessageRetry(event.municipalityId(), event.namespace(), MESSAGE_ERRAND_WITHDRAWN, errandId, Map.of(), e.getMessage());
		}
	}
}
