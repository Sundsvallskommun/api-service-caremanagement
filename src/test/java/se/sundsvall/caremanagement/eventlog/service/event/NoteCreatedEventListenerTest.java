package se.sundsvall.caremanagement.eventlog.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;
import se.sundsvall.caremanagement.eventlog.service.ErrandEventService;
import se.sundsvall.caremanagement.notes.service.event.NoteCreated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoteCreatedEventListenerTest {
	private static final OffsetDateTime TS = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private ErrandEventService serviceMock;

	@InjectMocks
	private NoteCreatedEventListener listener;

	private ErrandEventEntity capture() {
		final var captor = ArgumentCaptor.forClass(ErrandEventEntity.class);
		verify(serviceMock).recordDomainEvent(captor.capture());
		return captor.getValue();
	}

	@Test
	void recordsNoteCreatedAsDescriptiveEventRow() {
		listener.recordNoteCreation(new NoteCreated("note-1", "errand-1", "2281", "EB", "carola01winberg", TS));

		final var entity = capture();
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("EB");
		assertThat(entity.getSource()).isEqualTo("EVENT");
		assertThat(entity.getAction()).isEqualTo("CREATE");
		assertThat(entity.getTarget()).isEqualTo("note");
		assertThat(entity.getDescription()).isEqualTo("Anteckning tillagd");
		assertThat(entity.getActor()).isEqualTo("carola01winberg");
		assertThat(entity.getCreated()).isEqualTo(TS);
	}

	@Test
	void defaultsActorToSystemWhenAuthorBlank() {
		listener.recordNoteCreation(new NoteCreated("note-1", "errand-1", "2281", "EB", " ", TS));

		assertThat(capture().getActor()).isEqualTo("system");
	}
}
