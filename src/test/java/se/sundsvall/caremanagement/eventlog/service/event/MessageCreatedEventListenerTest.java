package se.sundsvall.caremanagement.eventlog.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.conversation.service.event.MessageCreated;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageCreatedEventListenerTest {
	private static final OffsetDateTime TS = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private ErrandEventService serviceMock;

	@InjectMocks
	private MessageCreatedEventListener listener;

	private ErrandEventEntity capture() {
		final var captor = ArgumentCaptor.forClass(ErrandEventEntity.class);
		verify(serviceMock).recordDomainEvent(captor.capture());
		return captor.getValue();
	}

	@Test
	void recordsOutboundMessageAsSent() {
		listener.recordMessageCreation(new MessageCreated("m-1", "2281", "EB", "errand-1", "OUTBOUND", "carola01winberg", true, TS));

		final var entity = capture();
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("EB");
		assertThat(entity.getSource()).isEqualTo("EVENT");
		assertThat(entity.getAction()).isEqualTo("CREATE");
		assertThat(entity.getTarget()).isEqualTo("message");
		assertThat(entity.getDescription()).isEqualTo("Meddelande skickat");
		assertThat(entity.getActor()).isEqualTo("carola01winberg");
		assertThat(entity.getCreated()).isEqualTo(TS);
	}

	@Test
	void recordsInboundMessageAsReceived() {
		listener.recordMessageCreation(new MessageCreated("m-1", "2281", "EB", "errand-1", "INBOUND", "199001011234", false, TS));

		assertThat(capture().getDescription()).isEqualTo("Meddelande mottaget");
	}

	@Test
	void defaultsActorToSystemWhenAuthorBlank() {
		listener.recordMessageCreation(new MessageCreated("m-1", "2281", "EB", "errand-1", "OUTBOUND", " ", false, TS));

		assertThat(capture().getActor()).isEqualTo("system");
	}
}
