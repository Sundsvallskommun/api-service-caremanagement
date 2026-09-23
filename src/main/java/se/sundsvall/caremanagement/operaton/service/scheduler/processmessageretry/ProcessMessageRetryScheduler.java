package se.sundsvall.caremanagement.operaton.service.scheduler.processmessageretry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

/**
 * Re-sends the process messages that did not reach the engine — the outbox behind finalize's
 * {@code PaymentDecisionReceived}. The work is {@link ProcessMessageRetryWorker}'s.
 */
@Component
class ProcessMessageRetryScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessMessageRetryScheduler.class);

	private final ProcessMessageRetryWorker worker;

	ProcessMessageRetryScheduler(final ProcessMessageRetryWorker worker) {
		this.worker = worker;
	}

	@Dept44Scheduled(cron = "${scheduler.process-message-retry.cron}",
		name = "${scheduler.process-message-retry.name}",
		lockAtMostFor = "${scheduler.process-message-retry.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.process-message-retry.maximum-execution-time}")
	void retryProcessMessages() {
		final var result = worker.retryDue();
		if (result.attempted() > 0) {
			LOG.info("Process message retry: {} attempted, {} delivered, {} given up", result.attempted(), result.delivered(), result.gaveUp());
		}
	}
}
