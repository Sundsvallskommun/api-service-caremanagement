package se.sundsvall.caremanagement.eventlog.service.event;

import java.util.Optional;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.conversation.service.event.MessageCreated;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;

/**
 * Records a published {@link MessageCreated} as a descriptive change-log row ({@code source = EVENT}) on the errand,
 * mirroring {@link ErrandEventDomainListener}. The message body never leaves the in-app thread — only the fact that a
 * message was posted is logged, worded by direction (OUTBOUND = caseworker → applicant, INBOUND = applicant →
 * caseworker), with the actor taken from the message's {@code author}.
 */
@Component
class MessageCreatedEventListener {

	static final String SOURCE = "EVENT";
	static final String SYSTEM_ACTOR = "system";
	static final String OUTBOUND = "OUTBOUND";

	private final ErrandEventService service;

	MessageCreatedEventListener(final ErrandEventService service) {
		this.service = service;
	}

	@ApplicationModuleListener
	void recordMessageCreation(final MessageCreated event) {
		service.recordDomainEvent(ErrandEventEntity.create()
			.withErrandId(event.errandId())
			.withMunicipalityId(event.municipalityId())
			.withNamespace(event.namespace())
			.withSource(SOURCE)
			.withAction("CREATE")
			.withTarget("message")
			.withDescription(description(event))
			.withActor(Optional.ofNullable(event.author()).filter(value -> !value.isBlank()).orElse(SYSTEM_ACTOR))
			.withCreated(event.timestamp()));
	}

	private static String description(final MessageCreated event) {
		if (OUTBOUND.equals(event.direction())) {
			return "Meddelande skickat";
		}
		return "Meddelande mottaget";
	}
}
