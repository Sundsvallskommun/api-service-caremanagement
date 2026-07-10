package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.DefaultAssigneeService;
import se.sundsvall.caremanagement.types.financialassistance.service.RecentlyClosedErrandService;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_NEEDS_MANUAL_REVIEW;

/**
 * The transactional work behind {@link FinancialAssistanceErrandCreatedListener}, split into two steps that each run in
 * their <b>own</b> transaction ({@link org.springframework.transaction.annotation.Propagation#REQUIRES_NEW
 * REQUIRES_NEW}).
 *
 * <p>
 * The errand-envelope writes here ({@link #assignAndClassify} — default-caseworker assignment and the recently-closed
 * freeze) race the {@link ApplicantNameSyncListener}, which updates the same {@code errand} row from a sibling
 * {@code StakeholderMutated} event the moment the errand is created. MariaDB/InnoDB can surface that concurrency as a
 * hard {@code 1020 "Record has changed since last read"} on the losing writer rather than a
 * silent last-writer-wins. Running in a fresh transaction lets the listener simply retry: the next attempt reads the
 * row
 * after the sibling write committed and proceeds cleanly. The process start lives in its own method so a retry of the
 * racy writes can never start the Operaton process twice.
 */
@Component
class FinancialAssistanceErrandCreatedProcessor {

	/** What the create classification decided, driving whether the listener goes on to start a process. */
	enum Outcome {
		/** Not an EB errand (no typed FA data) — nothing to do. */
		NOT_EB,
		/** A recently-closed re-application — frozen for manual review, no process. */
		FROZEN,
		/** A normal EB errand — the listener should start the type's process. */
		PROCEED
	}

	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final ErrandService errandService;
	private final DefaultAssigneeService defaultAssigneeService;
	private final RecentlyClosedErrandService recentlyClosedErrandService;
	private final FinancialAssistanceProcessStarter processStarter;

	FinancialAssistanceErrandCreatedProcessor(final FinancialAssistanceRepository financialAssistanceRepository, final ErrandService errandService,
		final DefaultAssigneeService defaultAssigneeService, final RecentlyClosedErrandService recentlyClosedErrandService,
		final FinancialAssistanceProcessStarter processStarter) {
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.errandService = errandService;
		this.defaultAssigneeService = defaultAssigneeService;
		this.recentlyClosedErrandService = recentlyClosedErrandService;
		this.processStarter = processStarter;
	}

	/**
	 * Assign the default handläggare (best-effort) and decide whether this re-application must be frozen. Runs in its own
	 * transaction so the listener can retry it on the transient row conflict with {@link ApplicantNameSyncListener}.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	Outcome assignAndClassify(final ErrandCreated event) {
		return financialAssistanceRepository.findByErrandId(event.errandId())
			.map(entity -> {
				assignDefaultHandlaggare(event);

				// Recently closed → freeze for manual review instead of auto-actualising (skip the process start entirely).
				if (recentlyClosedErrandService.findRecentlyClosed(event.municipalityId(), event.namespace(), parties(entity)).isPresent()) {
					freezeForManualReview(event);
					return Outcome.FROZEN;
				}
				return Outcome.PROCEED;
			})
			.orElse(Outcome.NOT_EB);
	}

	/**
	 * Start the type's process exactly once. Kept out of {@link #assignAndClassify} so retrying the racy errand writes can
	 * never double-start the Operaton process (businessKey = errandId).
	 */
	@Transactional(propagation = REQUIRES_NEW)
	void startProcess(final ErrandCreated event) {
		financialAssistanceRepository.findByErrandId(event.errandId())
			.ifPresent(entity -> processStarter.startFor(event.typeSlug(), event.municipalityId(), event.namespace(), event.errandId(), entity));
	}

	/**
	 * Route an EB errand that arrived without an assignee to the modeler-configured default handläggare (best-effort).
	 * Respects an assignee the application already carried; a renewal/supplement that later resolves a real Lifecare
	 * caseworker overwrites this in the actualisation flow.
	 */
	private void assignDefaultHandlaggare(final ErrandCreated event) {
		if (StringUtils.hasText(event.assignedUserId())) {
			return; // the application carried an explicit assignee — respect it
		}
		defaultAssigneeService.resolve(event.municipalityId())
			.ifPresent(assignedUserId -> errandService.updateErrand(event.municipalityId(), event.namespace(), event.errandId(),
				PatchErrand.create().withAssignedUserId(assignedUserId)));
	}

	/** Freeze a recently-closed re-application: set NEEDS_MANUAL_REVIEW and let a caseworker reopen + release it. */
	private void freezeForManualReview(final ErrandCreated event) {
		errandService.updateErrand(event.municipalityId(), event.namespace(), event.errandId(),
			PatchErrand.create().withStatus(STATUS_NEEDS_MANUAL_REVIEW));
	}

	/** The applicant and co-applicant partyIds carried on the application, blanks removed. */
	private static List<String> parties(final FinancialAssistanceEntity entity) {
		return Optional.ofNullable(entity.getPersons()).orElseGet(List::of).stream()
			.map(FaPerson::getPartyId)
			.filter(StringUtils::hasText)
			.toList();
	}
}
