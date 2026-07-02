package se.sundsvall.caremanagement.eventlog.service.event;

import java.util.Optional;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.caremanagement.notes.service.event.NoteCreated;

/**
 * Records a published {@link NoteCreated} as a descriptive change-log row ({@code source = EVENT}) on the errand,
 * mirroring {@link ErrandEventDomainListener}. The raw HTTP access row is still captured by the request interceptor;
 * this adds the human-readable change entry, with the actor taken from the note's {@code author} (the request-scoped
 * {@code Identifier} is gone by the time this async listener runs after commit).
 */
@Component
class NoteCreatedEventListener {

	static final String SOURCE = "EVENT";
	static final String SYSTEM_ACTOR = "system";

	private final ErrandEventService service;

	NoteCreatedEventListener(final ErrandEventService service) {
		this.service = service;
	}

	@ApplicationModuleListener
	void on(final NoteCreated event) {
		service.recordDomainEvent(ErrandEventEntity.create()
			.withErrandId(event.errandId())
			.withMunicipalityId(event.municipalityId())
			.withNamespace(event.namespace())
			.withSource(SOURCE)
			.withAction("CREATE")
			.withTarget("note")
			.withDescription("Anteckning tillagd")
			.withActor(Optional.ofNullable(event.author()).filter(value -> !value.isBlank()).orElse(SYSTEM_ACTOR))
			.withCreated(event.timestamp()));
	}
}
