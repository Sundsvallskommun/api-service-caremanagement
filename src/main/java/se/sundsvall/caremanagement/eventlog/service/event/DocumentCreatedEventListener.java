package se.sundsvall.caremanagement.eventlog.service.event;

import java.util.Optional;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.document.service.event.DocumentCreated;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;

/**
 * Records a published {@link DocumentCreated} as a descriptive change-log row ({@code source = EVENT}) on the errand,
 * mirroring the envelope entries in {@link ErrandEventDomainListener}. The raw HTTP access row for the document POST is
 * still captured independently by the request interceptor ({@code source = HTTP}); this adds the human-readable
 * "document added" change entry the access row can't express, and carries the actor from the document's
 * {@code createdBy}
 * (the request-scoped {@code Identifier} is gone by the time this async listener runs after commit).
 */
@Component
class DocumentCreatedEventListener {

	static final String SOURCE = "EVENT";
	static final String SYSTEM_ACTOR = "system";

	private final ErrandEventService service;

	DocumentCreatedEventListener(final ErrandEventService service) {
		this.service = service;
	}

	@ApplicationModuleListener
	void on(final DocumentCreated event) {
		service.recordDomainEvent(ErrandEventEntity.create()
			.withErrandId(event.errandId())
			.withMunicipalityId(event.municipalityId())
			.withNamespace(event.namespace())
			.withSource(SOURCE)
			.withAction("CREATE")
			.withTarget("document")
			.withDescription("Dokument tillagt: " + event.type())
			.withActor(Optional.ofNullable(event.createdBy()).filter(value -> !value.isBlank()).orElse(SYSTEM_ACTOR))
			.withCreated(event.timestamp()));
	}
}
