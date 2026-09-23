package se.sundsvall.caremanagement.operaton.service.scheduler.processmessageretry;

import java.time.Duration;
import java.time.OffsetDateTime;
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
		final var now = OffsetDateTime.now();
		var delivered = 0;
		var gaveUp = 0;
		final var due = repository.findByStatusAndNextAttemptBeforeOrderByNextAttempt(STATUS_PENDING, now);
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
		try {
			processService.correlateMessage(retry.getMunicipalityId(), retry.getNamespace(), retry.getMessageName(), retry.getErrandId(), variablesOf(retry));
			repository.delete(retry);
			LOG.info("Delivered {} to the process of errand {} on attempt {}", sanitizeForLogging(retry.getMessageName()), sanitizeForLogging(retry.getErrandId()),
				retry.getAttempts() + 1);
			return true;
		} catch (final RuntimeException e) {
			recordFailure(retry, now, e);
			return false;
		}
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
