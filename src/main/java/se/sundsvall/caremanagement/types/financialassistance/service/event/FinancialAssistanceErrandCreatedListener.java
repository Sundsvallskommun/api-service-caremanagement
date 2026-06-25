package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.util.List;
import java.util.Optional;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.DefaultAssigneeService;
import se.sundsvall.caremanagement.types.financialassistance.service.RecentlyClosedErrandService;

import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_NEEDS_MANUAL_REVIEW;

/**
 * Reacts to a created EB errand. Runs once the create transaction has committed
 * ({@link ApplicationModuleListener} = after-commit, async, new transaction) so the errand and its typed data are
 * persisted and visible both to the re-read below and to any Operaton workers that call back the moment the flow
 * starts.
 *
 * <p>
 * Every EB errand (one carrying typed FA data) that arrived without an assignee is first routed to the default
 * handläggare resolved from {@link DefaultAssigneeService} — the same handläggar-less fallback the Lifecare
 * {@code CaseworkerResolver} can't place.
 *
 * <p>
 * Then the recently-closed guard runs: if the applicant (or co-applicant) had an EB errand closed within the
 * recently-closed window, this re-application is <em>frozen</em> as {@code NEEDS_MANUAL_REVIEW} — no aktualisering (the
 * process is not started), and being non-CLOSED it is never picked up by the archive job — so a caseworker reopens the
 * previous insats in Lifecare and releases it (see {@link FinancialAssistanceReleaseListener}). Otherwise the normal
 * path applies: a renewal starts the full decision-support process and a supplementary application starts the lighter
 * tilläggsansökan process (whose actualisation step attaches the previous återansökan's Lifecare caseworker); a new
 * application has no prior insats and just sits awaiting the caseworker (default assignee).
 */
@Component
class FinancialAssistanceErrandCreatedListener {

	private final FinancialAssistanceRepository repository;
	private final ErrandService errandService;
	private final DefaultAssigneeService defaultAssigneeService;
	private final RecentlyClosedErrandService recentlyClosedErrandService;
	private final FinancialAssistanceProcessStarter processStarter;

	FinancialAssistanceErrandCreatedListener(final FinancialAssistanceRepository repository, final ErrandService errandService,
		final DefaultAssigneeService defaultAssigneeService, final RecentlyClosedErrandService recentlyClosedErrandService,
		final FinancialAssistanceProcessStarter processStarter) {
		this.repository = repository;
		this.errandService = errandService;
		this.defaultAssigneeService = defaultAssigneeService;
		this.recentlyClosedErrandService = recentlyClosedErrandService;
		this.processStarter = processStarter;
	}

	@ApplicationModuleListener
	void on(final ErrandCreated event) {
		// Only EB errands (those carrying typed FA data) are handled here.
		repository.findByErrandId(event.errandId()).ifPresent(entity -> {
			assignDefaultHandlaggare(event);

			// Recently closed → freeze for manual review instead of auto-actualising (skip the process start entirely).
			if (recentlyClosedErrandService.findRecentlyClosed(event.municipalityId(), event.namespace(), parties(entity)).isPresent()) {
				freezeForManualReview(event);
				return;
			}

			if (SLUG_RENEWAL.equals(event.typeSlug())) {
				processStarter.start(event.municipalityId(), event.namespace(), event.errandId(), entity); // renewal starts the full decision-support process
			} else if (SLUG_SUPPLEMENTARY.equals(event.typeSlug())) {
				// A supplementary application supplements an ongoing bistånd: start the tilläggsansökan process so its
				// actualisation step attaches the previous återansökan's Lifecare caseworker, not the default assignee.
				processStarter.startSupplementary(event.municipalityId(), event.namespace(), event.errandId(), entity);
			}
		});
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
