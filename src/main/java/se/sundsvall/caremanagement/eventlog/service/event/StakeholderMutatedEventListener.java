package se.sundsvall.caremanagement.eventlog.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.caremanagement.stakeholders.service.event.StakeholderMutated;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * Records a published {@link StakeholderMutated} as a change-log row ({@code source = EVENT}) on the errand. The event
 * carries only the errand coordinates — no operation kind, actor or timestamp — so this is a coarse "stakeholder
 * changed" note stamped at handling time; the precise who/when for the triggering request is in the HTTP access row.
 */
@Component
class StakeholderMutatedEventListener {

	static final String SOURCE = "EVENT";
	static final String SYSTEM_ACTOR = "system";

	private final ErrandEventService service;

	StakeholderMutatedEventListener(final ErrandEventService service) {
		this.service = service;
	}

	@ApplicationModuleListener
	void recordStakeholderChange(final StakeholderMutated event) {
		service.recordDomainEvent(ErrandEventEntity.create()
			.withErrandId(event.errandId())
			.withMunicipalityId(event.municipalityId())
			.withNamespace(event.namespace())
			.withSource(SOURCE)
			.withAction("UPDATE")
			.withTarget("stakeholder")
			.withDescription("Intressent ändrad")
			.withActor(SYSTEM_ACTOR)
			.withCreated(now(systemDefault()).truncatedTo(MILLIS)));
	}
}
