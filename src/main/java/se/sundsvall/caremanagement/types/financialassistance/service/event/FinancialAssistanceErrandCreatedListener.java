package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.types.financialassistance.service.DefaultAssigneeService;
import se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedProcessor.Outcome;

/**
 * Reacts to a created financial assistance errand. Runs once the create transaction has committed
 * ({@link ApplicationModuleListener} = after-commit, async, new transaction) so the errand and its typed data are
 * persisted and visible both to the re-read below and to any Operaton workers that call back the moment the flow
 * starts.
 *
 * <p>
 * Every financial assistance errand (one carrying typed FA data) that arrived without an assignee is first routed to
 * the default
 * caseworker resolved from {@link DefaultAssigneeService} — the same caseworker-less fallback the Lifecare
 * {@code CaseworkerResolver} can't place.
 *
 * <p>
 * Then the recently-closed guard runs: if the applicant (or co-applicant) had a financial assistance errand closed
 * within the
 * recently-closed window, this re-application is <em>frozen</em> as {@code NEEDS_MANUAL_REVIEW} — no actualisation (the
 * process is not started), and being non-CLOSED it is never picked up by the archive job — so a caseworker reopens the
 * previous intervention in Lifecare and releases it (see {@link FinancialAssistanceReleaseListener}). Otherwise the
 * normal
 * path applies: a renewal starts the full decision-support process, a supplementary application starts the lighter
 * supplementary-application process (whose actualisation step attaches the previous renewal application's Lifecare
 * caseworker), and a new
 * application starts the new-application process (status + actualisation, then a calculation built straight from the
 * application); having no prior intervention, a new application keeps the default assignee.
 *
 * <p>
 * The classification step writes the {@code errand} row and so races {@link ApplicantNameSyncListener}, which updates
 * the same row from a sibling {@code StakeholderMutated} event. MariaDB/InnoDB can surface the losing writer of
 * that race as {@code 1020 "Record has changed since last read"}; the work runs in a fresh transaction
 * ({@link FinancialAssistanceErrandCreatedProcessor}) so we simply retry it — the next attempt reads the row after the
 * sibling write committed. Process start stays outside the retry so it can never run twice.
 */
@Component
class FinancialAssistanceErrandCreatedListener {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceErrandCreatedListener.class);

	/** MariaDB InnoDB snapshot-isolation conflict: ER_CHECKREAD, "Record has changed since last read". */
	private static final int ER_CHECKREAD = 1020;
	private static final int MAX_ATTEMPTS = 4;

	private final FinancialAssistanceErrandCreatedProcessor processor;

	FinancialAssistanceErrandCreatedListener(final FinancialAssistanceErrandCreatedProcessor processor) {
		this.processor = processor;
	}

	@ApplicationModuleListener
	void classifyAndStartProcess(final ErrandCreated event) {
		// The classification writes the errand row and can lose a snapshot-isolation race with the applicant-name sync,
		// so retry it in a fresh transaction until it lands. Only then, and only for a normal financial assistance errand,
		// start the process.
		if (assignAndClassifyWithRetry(event) == Outcome.PROCEED) {
			processor.startProcess(event);
		}
	}

	private Outcome assignAndClassifyWithRetry(final ErrandCreated event) {
		for (var attempt = 1;; attempt++) {
			try {
				return processor.assignAndClassify(event);
			} catch (final DataAccessException e) {
				if (attempt >= MAX_ATTEMPTS || !isSnapshotConflict(e)) {
					throw e;
				}
				LOG.info("financial assistance errand {} create classification hit a transient row conflict (attempt {}/{}); retrying",
					event.errandId(), attempt, MAX_ATTEMPTS);
			}
		}
	}

	/** True when the failure is the MariaDB snapshot-isolation conflict (errorCode 1020) — the one worth retrying. */
	private static boolean isSnapshotConflict(final Throwable throwable) {
		for (var cause = throwable; cause != null; cause = cause.getCause()) {
			if (cause instanceof final SQLException sqlException && sqlException.getErrorCode() == ER_CHECKREAD) {
				return true;
			}
		}
		return false;
	}
}
