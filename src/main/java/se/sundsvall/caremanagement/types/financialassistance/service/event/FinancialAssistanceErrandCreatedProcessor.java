package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.DefaultAssigneeService;
import se.sundsvall.caremanagement.types.financialassistance.service.ProtectedIdentityGate;
import se.sundsvall.caremanagement.types.financialassistance.service.RecentlyClosedErrandService;

import static java.util.Optional.ofNullable;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_NEEDS_MANUAL_REVIEW;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * The transactional work behind {@link FinancialAssistanceErrandCreatedListener}, split into two steps that each run in
 * their <b>own</b> transaction ({@link org.springframework.transaction.annotation.Propagation#REQUIRES_NEW
 * REQUIRES_NEW}).
 *
 * <p>
 * The errand-envelope writes here ({@link #assignAndClassify} — default-caseworker assignment and the two freezes,
 * protected identity and recently closed) race the {@link ApplicantNameSyncListener}, which updates the same
 * {@code errand} row from a sibling {@code StakeholderMutated} event the moment the errand is created. MariaDB/InnoDB
 * can surface that concurrency as a
 * hard {@code 1020 "Record has changed since last read"} on the losing writer rather than a
 * silent last-writer-wins. Running in a fresh transaction lets the listener simply retry: the next attempt reads the
 * row
 * after the sibling write committed and proceeds cleanly. The process start lives in its own method so a retry of the
 * racy writes can never start the Operaton process twice.
 */
@Component
class FinancialAssistanceErrandCreatedProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceErrandCreatedProcessor.class);

	/** What the create classification decided, driving whether the listener goes on to start a process. */
	enum Outcome {
		/** Not a financial assistance errand (no typed FA data) — nothing to do. */
		NOT_FINANCIAL_ASSISTANCE,
		/**
		 * Held back from automatic preparation — protected identity (or unable to tell), or a recently-closed
		 * re-application — frozen for manual review, no process.
		 */
		FROZEN,
		/** A normal financial assistance errand — the listener should start the type's process. */
		PROCEED
	}

	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final ErrandService errandService;
	private final DefaultAssigneeService defaultAssigneeService;
	private final ProtectedIdentityGate protectedIdentityGate;
	private final RecentlyClosedErrandService recentlyClosedErrandService;
	private final FinancialAssistanceProcessStarter processStarter;

	FinancialAssistanceErrandCreatedProcessor(final FinancialAssistanceRepository financialAssistanceRepository, final ErrandService errandService,
		final DefaultAssigneeService defaultAssigneeService, final ProtectedIdentityGate protectedIdentityGate,
		final RecentlyClosedErrandService recentlyClosedErrandService, final FinancialAssistanceProcessStarter processStarter) {
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.errandService = errandService;
		this.defaultAssigneeService = defaultAssigneeService;
		this.protectedIdentityGate = protectedIdentityGate;
		this.recentlyClosedErrandService = recentlyClosedErrandService;
		this.processStarter = processStarter;
	}

	/**
	 * Assign the default handläggare (best-effort) and decide whether the errand must be frozen for manual review instead
	 * of going into automatic preparation. An errand is frozen for either of two reasons, checked in this order:
	 *
	 * <ol>
	 * <li><b>Protected identity, fail closed</b> — the applicant, the co-applicant or a child on the application has
	 * protected identity in the population register or in Lifecare, <em>or</em> that could not be established because a
	 * lookup failed ({@link ProtectedIdentityGate}). Automatic preparation reads SSBTEK and writes to Lifecare, so an
	 * errand that may concern a protected person must not start it. The reason is deliberately not recorded on the
	 * errand: the status is the same {@code NEEDS_MANUAL_REVIEW} as for the second reason, so the protected status is
	 * not readable from any errand field.</li>
	 * <li><b>Recently closed</b> — a re-application of a party whose previous errand was closed within the
	 * recently-closed window ({@link RecentlyClosedErrandService}).</li>
	 * </ol>
	 *
	 * A frozen errand still gets the default handläggare, and never has its process started here (a caseworker's release,
	 * see {@link FinancialAssistanceReleaseListener}, starts it later).
	 *
	 * <p>
	 * Runs in its own transaction so the listener can retry it on the transient row conflict with
	 * {@link ApplicantNameSyncListener}. The identity lookups are reads, so a retry only repeats them.
	 */
	@Transactional(propagation = REQUIRES_NEW)
	Outcome assignAndClassify(final ErrandCreated event) {
		return financialAssistanceRepository.findByErrandId(event.errandId())
			.map(entity -> {
				assignDefaultHandlaggare(event);

				// Protected identity (or unable to tell) → freeze for manual review before any automatic preparation. Checked first:
				// it is the hard stop, and a frozen errand needs no further classification.
				if (protectedIdentityGate.protectedOrUnknown(event.municipalityId(), event.errandId(), identityParties(entity))) {
					// Deliberately neutral: the line must not tell whether a party is protected or the check merely failed (the gate
					// logs the failed lookup itself).
					LOG.info("Financial assistance errand {} held for manual review before automatic preparation", sanitizeForLogging(event.errandId()));
					freezeForManualReview(event);
					return Outcome.FROZEN;
				}

				// Recently closed → freeze for manual review instead of auto-actualising (skip the process start entirely).
				if (recentlyClosedErrandService.findRecentlyClosed(event.municipalityId(), event.namespace(), parties(entity)).isPresent()) {
					freezeForManualReview(event);
					return Outcome.FROZEN;
				}
				return Outcome.PROCEED;
			})
			.orElse(Outcome.NOT_FINANCIAL_ASSISTANCE);
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
	 * Route a financial assistance errand that arrived without an assignee to the modeler-configured default handläggare
	 * (best-effort).
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

	/** Freeze the errand: set NEEDS_MANUAL_REVIEW and let a caseworker take it by hand (and release it when it may run). */
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

	/**
	 * Everyone on the application the automatic preparation would read about — the applicant and co-applicant, and the
	 * children (whose income is read from SSBTEK and who are named in warnings) — as distinct partyIds, blanks removed.
	 * Wider than {@link #parties}: a recently closed errand is looked up by the adults who applied, whereas a protected
	 * identity matters for every person whose data the preparation touches.
	 */
	private static List<String> identityParties(final FinancialAssistanceEntity entity) {
		return Stream.concat(
			ofNullable(entity.getPersons()).orElseGet(List::of).stream().map(FaPerson::getPartyId),
			ofNullable(entity.getChildren()).orElseGet(List::of).stream().map(FaChild::getPartyId))
			.filter(StringUtils::hasText)
			.distinct()
			.toList();
	}
}
