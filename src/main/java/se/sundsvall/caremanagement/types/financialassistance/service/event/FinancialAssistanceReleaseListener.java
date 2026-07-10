package se.sundsvall.caremanagement.types.financialassistance.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;

import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_NEEDS_MANUAL_REVIEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_UNDER_REVIEW;

/**
 * Releases a manually-reviewed financial assistance errand for processing. After a caseworker has reopened the previous
 * intervention in
 * Lifecare
 * and moves the frozen errand {@code NEEDS_MANUAL_REVIEW → UNDER_REVIEW}, this starts the process belonging to the
 * errand's own application type, so it runs exactly as it would have at creation had the recently-closed freeze not
 * held it back. A frozen renewal resumes the full decision-support flow (SSBTEK → calculation → recommendation →
 * caseworker in the loop); a frozen new application still builds its normberäkning from what the citizen declared. The
 * freeze defers a start, it never rewrites the application type — a household that applied as a couple after a
 * single-applicant case is a new application, and the eligibility check has already routed it as one.
 *
 * <p>
 * The trigger is scoped precisely to that one edge so the normal renewal flow — which already starts its process at
 * creation and whose worker later moves the errand {@code RECEIVED → UNDER_REVIEW} — never re-triggers a start. A
 * defensive no-double-start guard additionally skips any errand that already carries a process instance.
 */
@Component
class FinancialAssistanceReleaseListener {

	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final ErrandQueryService errandQueryService;
	private final FinancialAssistanceProcessStarter processStarter;

	FinancialAssistanceReleaseListener(final FinancialAssistanceRepository financialAssistanceRepository, final ErrandQueryService errandQueryService,
		final FinancialAssistanceProcessStarter processStarter) {
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.errandQueryService = errandQueryService;
		this.processStarter = processStarter;
	}

	@ApplicationModuleListener
	void on(final ErrandStatusChanged event) {
		if (!isReleaseTransition(event)) {
			return;
		}
		errandQueryService.findErrand(event.municipalityId(), event.namespace(), event.errandId())
			.filter(errand -> !StringUtils.hasText(errand.getProcessInstanceId())) // defensive: never start a second instance
			.flatMap(errand -> financialAssistanceRepository.findByErrandId(event.errandId()))
			.ifPresent(entity -> processStarter.startFor(event.typeSlug(), event.municipalityId(), event.namespace(), event.errandId(), entity));
	}

	/**
	 * The manual-review release edge: a caseworker moving the frozen errand out of NEEDS_MANUAL_REVIEW into UNDER_REVIEW.
	 */
	private static boolean isReleaseTransition(final ErrandStatusChanged event) {
		return STATUS_NEEDS_MANUAL_REVIEW.equals(event.fromStatus()) && STATUS_UNDER_REVIEW.equals(event.toStatus());
	}
}
