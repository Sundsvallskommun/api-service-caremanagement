package se.sundsvall.caremanagement.eventlog.service.event;

import java.util.Optional;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandAssigned;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.core.service.event.ErrandEvent;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;

/**
 * Records the published envelope {@link ErrandEvent}s into {@code errand_event} as the change log
 * ({@code source = EVENT}). Origin-agnostic: it fires whether the change came from a human request, an Operaton worker
 * or in-process logic, so process- and system-driven changes are captured even when no HTTP request carried them — most
 * importantly errand creation, which the request interceptor cannot see (the create call has no errand id in its path).
 *
 * The actor comes from the event (it ran on a different thread after commit, so the request-scoped {@code Identifier}
 * is
 * gone); when the event carries none, the actor is {@code system}. For reliable human attribution, the HTTP access log
 * ({@code source = HTTP}, populated from {@code X-Sent-By}) remains authoritative.
 */
@Component
class ErrandEventDomainListener {

	static final String SOURCE = "EVENT";
	static final String SYSTEM_ACTOR = "system";

	private final ErrandEventService service;

	ErrandEventDomainListener(final ErrandEventService service) {
		this.service = service;
	}

	@ApplicationModuleListener
	void on(final ErrandEvent event) {
		final var entity = base(event);

		switch (event) {
			case final ErrandCreated created -> service.recordDomainEvent(entity
				.withAction("CREATE")
				.withTarget("errand")
				.withDescription("Ärende skapat")
				.withActor(actorOr(created.reporterUserId())));
			case final ErrandStatusChanged changed -> service.recordDomainEvent(entity
				.withAction("UPDATE")
				.withTarget("status")
				.withDescription("Status " + changed.fromStatus() + " → " + changed.toStatus())
				.withActor(actorOr(changed.changedBy())));
			case final ErrandAssigned assigned -> service.recordDomainEvent(entity
				.withAction("UPDATE")
				.withTarget("assignment")
				.withDescription("Tilldelad " + assigned.previousAssignee() + " → " + assigned.newAssignee())
				.withActor(actorOr(assigned.changedBy())));
			case final ErrandDeleted deleted -> service.recordDomainEvent(entity
				.withAction("DELETE")
				.withTarget("errand")
				.withDescription("Ärende borttaget")
				.withActor(actorOr(deleted.deletedBy())));
		}
	}

	private static ErrandEventEntity base(final ErrandEvent event) {
		return ErrandEventEntity.create()
			.withErrandId(event.errandId())
			.withMunicipalityId(event.municipalityId())
			.withNamespace(event.namespace())
			.withSource(SOURCE)
			.withCreated(event.timestamp());
	}

	private static String actorOr(final String actor) {
		return Optional.ofNullable(actor)
			.filter(value -> !value.isBlank())
			.orElse(SYSTEM_ACTOR);
	}
}
