package se.sundsvall.caremanagement.eventlog.service.event;

import java.util.Optional;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.decisions.service.event.DecisionRecorded;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;

import static org.springframework.util.StringUtils.hasText;

/**
 * Records a published {@link DecisionRecorded} as a descriptive change-log row ({@code source = EVENT}) on the errand,
 * mirroring {@link ErrandEventDomainListener}. A decision (a RECOMMENDATION from the process, a PAYMENT etc. from a
 * handläggare) is the most significant errand activity, so it gets a "Beslut registrerat: `<type>` = `<outcome>`"
 * entry;
 * the actor is the decision's {@code decidedBy} (the process actor for system decisions, the caseworker otherwise).
 */
@Component
class DecisionRecordedEventListener {

	static final String SOURCE = "EVENT";
	static final String SYSTEM_ACTOR = "system";

	private final ErrandEventService service;

	DecisionRecordedEventListener(final ErrandEventService service) {
		this.service = service;
	}

	@ApplicationModuleListener
	void on(final DecisionRecorded event) {
		service.recordDomainEvent(ErrandEventEntity.create()
			.withErrandId(event.errandId())
			.withMunicipalityId(event.municipalityId())
			.withNamespace(event.namespace())
			.withSource(SOURCE)
			.withAction("CREATE")
			.withTarget("decision")
			.withDescription(description(event))
			.withActor(Optional.ofNullable(event.decidedBy()).filter(value -> !value.isBlank()).orElse(SYSTEM_ACTOR))
			.withCreated(event.timestamp()));
	}

	private static String description(final DecisionRecorded event) {
		final var base = "Beslut registrerat: " + event.typeSlug();
		return hasText(event.outcome()) ? base + " = " + event.outcome() : base;
	}
}
