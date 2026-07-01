package se.sundsvall.caremanagement.types.financialassistance.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;

import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_NEEDS_MANUAL_REVIEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_UNDER_REVIEW;

/**
 * Releases a manually-reviewed financial assistance errand for processing. After a caseworker has reopened the previous
 * intervention in
 * Lifecare
 * and moves the frozen errand {@code NEEDS_MANUAL_REVIEW → UNDER_REVIEW}, this starts the financial assistance process
 * on the same errand
 * so it runs like a normal renewal (SSBTEK → calculation → recommendation → caseworker in the loop).
 *
 * <p>
 * The trigger is scoped precisely to that one edge so the normal renewal flow — which already starts its process at
 * creation and whose worker later moves the errand {@code RECEIVED → UNDER_REVIEW} — never re-triggers a start. A
 * defensive no-double-start guard additionally skips any errand that already carries a process instance.
 */
@Component
class FinancialAssistanceReleaseListener {

	private final FinancialAssistanceRepository repository;
	private final ErrandRepository errandRepository;
	private final FinancialAssistanceProcessStarter processStarter;

	FinancialAssistanceReleaseListener(final FinancialAssistanceRepository repository, final ErrandRepository errandRepository,
		final FinancialAssistanceProcessStarter processStarter) {
		this.repository = repository;
		this.errandRepository = errandRepository;
		this.processStarter = processStarter;
	}

	@ApplicationModuleListener
	void on(final ErrandStatusChanged event) {
		if (!isReleaseTransition(event)) {
			return;
		}
		errandRepository.findByIdAndNamespaceAndMunicipalityId(event.errandId(), event.namespace(), event.municipalityId())
			.filter(errand -> !StringUtils.hasText(errand.getProcessInstanceId())) // defensive: never start a second instance
			.flatMap(errand -> repository.findByErrandId(event.errandId()))
			.ifPresent(entity -> processStarter.start(event.municipalityId(), event.namespace(), event.errandId(), entity));
	}

	/**
	 * The manual-review release edge: a caseworker moving the frozen errand out of NEEDS_MANUAL_REVIEW into UNDER_REVIEW.
	 */
	private static boolean isReleaseTransition(final ErrandStatusChanged event) {
		return STATUS_NEEDS_MANUAL_REVIEW.equals(event.fromStatus()) && STATUS_UNDER_REVIEW.equals(event.toStatus());
	}
}
