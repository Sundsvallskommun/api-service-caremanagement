package se.sundsvall.caremanagement.eventlog.service.event;

import java.util.Optional;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.caremanagement.journal.service.event.JournalEntryCreated;

/**
 * Records a published {@link JournalEntryCreated} as a descriptive change-log row ({@code source = EVENT}) on the
 * errand,
 * mirroring {@link ErrandEventDomainListener}. The raw HTTP access row is still captured by the request interceptor;
 * this adds the human-readable change entry, with the actor taken from the entry's {@code createdBy} (the
 * request-scoped
 * {@code Identifier} is gone by the time this async listener runs after commit).
 */
@Component
class JournalEntryCreatedEventListener {

	static final String SOURCE = "EVENT";
	static final String SYSTEM_ACTOR = "system";

	private final ErrandEventService service;

	JournalEntryCreatedEventListener(final ErrandEventService service) {
		this.service = service;
	}

	@ApplicationModuleListener
	void recordJournalEntryCreation(final JournalEntryCreated event) {
		service.recordDomainEvent(ErrandEventEntity.create()
			.withErrandId(event.errandId())
			.withMunicipalityId(event.municipalityId())
			.withNamespace(event.namespace())
			.withSource(SOURCE)
			.withAction("CREATE")
			.withTarget("journal-entry")
			.withDescription("Journalanteckning tillagd: " + event.type())
			.withActor(Optional.ofNullable(event.createdBy()).filter(value -> !value.isBlank()).orElse(SYSTEM_ACTOR))
			.withCreated(event.timestamp()));
	}
}
