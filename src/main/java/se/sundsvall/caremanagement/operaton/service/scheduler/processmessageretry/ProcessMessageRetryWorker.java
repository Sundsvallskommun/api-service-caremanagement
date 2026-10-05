package se.sundsvall.caremanagement.operaton.service.scheduler.processmessageretry;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.operaton.integration.db.ProcessMessageRetryRepository;
import se.sundsvall.caremanagement.operaton.integration.db.model.ProcessMessageRetryEntity;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static se.sundsvall.caremanagement.operaton.service.ProcessService.truncate;
import static se.sundsvall.caremanagement.operaton.service.ProcessService.variablesOf;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Sends each due, queued process message again. Delivered rows are removed; a failure is counted and the next attempt
 * backs off — 1, 2, 4 … minutes, at most an hour apart — because a message that failed usually failed on an engine
 * that is down or a process that has not reached its catch event yet (a decision made while the beredning still runs),
 * and both resolve with time.
 *
 * <p>
 * Every failure is retried, a {@code 404} ("nothing waiting") included: that is also what the engine answers while the
 * process has not reached its catch event. A row still undelivered {@value #GIVE_UP_AFTER_DAYS} days after it was
 * queued
 * is marked {@code GAVE_UP} and logged as an error, and stays in the table as the record of it. A message that did
 * arrive but whose answer was lost ends there too: the engine then keeps answering {@code 404}. The row is the evidence
 * either way; correlating by hand through the process-messages endpoint is the remedy.
 * </p>
 *
 * <p>
 * The one exception is a message that ends a process ({@code ErrandWithdrawn}): a {@code 404} for it means there is
 * no process to end — it ended already, or the errand never had one — which is where the message was meant to leave
 * the errand. Such a row is settled and removed with an info line (and counted with the delivered ones), instead of
 * retrying for days and ending as an error for an errand nothing was wrong with; see
 * {@link ProcessService#isNothingToEnd}.
 * </p>
 *
 * <p>
 * Likewise a {@code 404} for an errand the engine runs no process for at all: no catch event will ever wait for the
 * message, so it has nowhere to go. That is an errand whose process ended, or one that never had one (EB-26090038, a
 * pre-process errand, got a payment decision on 2026-09-30 and was retried hourly). Only a running process can still be
 * on its way to the catch event; an engine that cannot say leaves the row to be retried as before.
 * </p>
 */
@Component
class ProcessMessageRetryWorker {

	static final long GIVE_UP_AFTER_DAYS = 3;
	static final long MAX_BACKOFF_MINUTES = 60;

	private static final Logger LOG = LoggerFactory.getLogger(ProcessMessageRetryWorker.class);
	private static final String STATUS_PENDING = "PENDING";
	private static final String STATUS_GAVE_UP = "GAVE_UP";

	record Result(int attempted, int delivered, int gaveUp) {}

	private final ProcessMessageRetryRepository repository;
	private final ProcessService processService;

	ProcessMessageRetryWorker(final ProcessMessageRetryRepository repository, final ProcessService processService) {
		this.repository = repository;
		this.processService = processService;
	}

	Result retryDue() {
		final var now = OffsetDateTime.now(ZoneId.systemDefault());
		var delivered = 0;
		var gaveUp = 0;
		final var due = repository.findTop5ByStatusAndNextAttemptBeforeOrderByNextAttempt(STATUS_PENDING, now);
		for (final var retry : due) {
			if (retryOne(retry, now)) {
				delivered++;
			} else if (STATUS_GAVE_UP.equals(retry.getStatus())) {
				gaveUp++;
			}
		}
		return new Result(due.size(), delivered, gaveUp);
	}

	/** One row, failures kept to the row so the rest of the batch still runs. */
	private boolean retryOne(final ProcessMessageRetryEntity retry, final OffsetDateTime now) {
		// Claimed before the engine call: the next attempt moves forward first, so a run that overlaps this one (a lock
		// that expired under a hanging engine, or a second instance) does not pick the row up and send it again.
		retry.setNextAttempt(now.plus(backoff(retry.getAttempts() + 1)));
		repository.save(retry);
		try {
			processService.correlateMessage(retry.getMunicipalityId(), retry.getNamespace(), retry.getMessageName(), retry.getErrandId(), variablesOf(retry));
			repository.delete(retry);
			LOG.info("Delivered {} to the process of errand {} on attempt {}", sanitizeForLogging(retry.getMessageName()), sanitizeForLogging(retry.getErrandId()),
				retry.getAttempts() + 1);
			return true;
		} catch (final RuntimeException e) {
			if (ProcessService.isNothingToEnd(retry.getMessageName(), e) || hasNowhereToGo(retry, e)) {
				return settleWithoutDelivery(retry);
			}
			try {
				recordFailure(retry, now, e);
			} catch (final RuntimeException saveFailure) {
				// The claim above already moved the next attempt forward, so the row is simply tried again later.
				LOG.warn("Could not record the failed delivery for errand {} ({})", sanitizeForLogging(retry.getErrandId()), saveFailure.getClass().getSimpleName());
			}
			return false;
		}
	}

	/** A {@code 404} while no process runs for the errand. Unknown (the engine did not answer) counts as no. */
	private boolean hasNowhereToGo(final ProcessMessageRetryEntity retry, final RuntimeException failure) {
		if (!ProcessService.isNotFound(failure)) {
			return false;
		}
		try {
			return !processService.hasRunningProcess(retry.getMunicipalityId(), retry.getErrandId());
		} catch (final RuntimeException unknown) {
			return false;
		}
	}

	/**
	 * There is no process for the message to reach: the row is done. A failure to remove it leaves it to the
	 * next run, which is then answered the same way.
	 */
	private boolean settleWithoutDelivery(final ProcessMessageRetryEntity retry) {
		try {
			repository.delete(retry);
		} catch (final RuntimeException deleteFailure) {
			LOG.warn("Could not remove the settled {} for errand {} ({})", sanitizeForLogging(retry.getMessageName()), sanitizeForLogging(retry.getErrandId()),
				deleteFailure.getClass().getSimpleName());
			return false;
		}
		LOG.info("No process is running for errand {}; {} has nowhere to go and is dropped after {} attempt(s)", sanitizeForLogging(retry.getErrandId()),
			sanitizeForLogging(retry.getMessageName()), retry.getAttempts() + 1);
		return true;
	}

	private void recordFailure(final ProcessMessageRetryEntity retry, final OffsetDateTime now, final RuntimeException e) {
		final var attempts = retry.getAttempts() + 1;
		retry.withAttempts(attempts).withLastError(truncate(e.getMessage()));
		if (retry.getCreated().plusDays(GIVE_UP_AFTER_DAYS).isBefore(now)) {
			retry.setStatus(STATUS_GAVE_UP);
			LOG.error("Gave up delivering {} to the process of errand {} after {} attempts; correlate it through the process-messages endpoint", sanitizeForLogging(retry
				.getMessageName()), sanitizeForLogging(retry.getErrandId()), attempts);
		} else {
			retry.setNextAttempt(now.plus(backoff(attempts)));
		}
		repository.save(retry);
	}

	/** 1, 2, 4 … minutes after the given number of attempts, never more than an hour. */
	static Duration backoff(final int attempts) {
		final var exponent = Math.min(Math.max(attempts - 1, 0), 6);
		return Duration.ofMinutes(Math.min(1L << exponent, MAX_BACKOFF_MINUTES));
	}
}
