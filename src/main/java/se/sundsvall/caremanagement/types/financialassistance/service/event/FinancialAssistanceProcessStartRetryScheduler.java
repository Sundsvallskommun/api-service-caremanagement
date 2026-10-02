package se.sundsvall.caremanagement.types.financialassistance.service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

/**
 * Gives a financial assistance errand its process again when the start that followed its creation did not take — the
 * engine was down, the token request failed, the call timed out. Thin trigger; the work is
 * {@link FinancialAssistanceProcessStartRetryWorker}'s. ShedLock keeps a single node running it at a time.
 *
 * <p>
 * Lives beside {@link FinancialAssistanceProcessStarter} and {@link FinancialAssistanceErrandCreatedProcessor} rather
 * than in a {@code scheduler} package, because the recovery goes through both of them and they are package-private.
 */
@Component
class FinancialAssistanceProcessStartRetryScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceProcessStartRetryScheduler.class);

	private final FinancialAssistanceProcessStartRetryWorker worker;

	FinancialAssistanceProcessStartRetryScheduler(final FinancialAssistanceProcessStartRetryWorker worker) {
		this.worker = worker;
	}

	@Dept44Scheduled(cron = "${scheduler.process-start-retry.cron}",
		name = "${scheduler.process-start-retry.name}",
		lockAtMostFor = "${scheduler.process-start-retry.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.process-start-retry.maximum-execution-time}")
	void retryProcessStarts() {
		final var result = worker.retryUnstarted();
		if (result.candidates() > 0 || result.agedOut() > 0) {
			LOG.info("Process start retry: {} errand(s) without a process, {} started, {} already running, {} held for manual review, {} failed; {} aged out",
				result.candidates(), result.started(), result.alreadyRunning(), result.held(), result.failed(), result.agedOut());
		}
	}
}
