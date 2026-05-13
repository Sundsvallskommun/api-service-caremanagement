package se.sundsvall.caremanagement.notes.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import se.sundsvall.caremanagement.notes.api.model.CreateNote;
import se.sundsvall.caremanagement.notes.integration.db.NoteRepository;
import se.sundsvall.caremanagement.notes.integration.db.model.NoteEntity;
import se.sundsvall.caremanagement.notes.service.event.NoteAdded;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

	@Mock
	private NoteRepository repositoryMock;

	@Mock
	private ApplicationEventPublisher eventsMock;

	@InjectMocks
	private NoteService service;

	@Test
	void addPublishesEventAndReturnsId() {
		final var saved = NoteEntity.create().withId("note-1").withErrandId("errand-1");
		when(repositoryMock.save(any(NoteEntity.class))).thenReturn(saved);

		final var id = service.add("errand-1", new CreateNote("body", "author"));

		assertThat(id).isEqualTo("note-1");

		final ArgumentCaptor<NoteEntity> entityCaptor = ArgumentCaptor.forClass(NoteEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrandId()).isEqualTo("errand-1");
		assertThat(entityCaptor.getValue().getBody()).isEqualTo("body");
		assertThat(entityCaptor.getValue().getAuthor()).isEqualTo("author");
		assertThat(entityCaptor.getValue().getCreated()).isNotNull();

		final ArgumentCaptor<NoteAdded> eventCaptor = ArgumentCaptor.forClass(NoteAdded.class);
		verify(eventsMock).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().noteId()).isEqualTo("note-1");
		assertThat(eventCaptor.getValue().errandId()).isEqualTo("errand-1");
		assertThat(eventCaptor.getValue().author()).isEqualTo("author");
	}

	@Test
	void listForErrandReturnsMappedNotes() {
		final var ts = OffsetDateTime.now();
		when(repositoryMock.findByErrandIdOrderByCreatedDesc("errand-1")).thenReturn(List.of(
			NoteEntity.create().withId("n1").withErrandId("errand-1").withBody("b1").withAuthor("a1").withCreated(ts)));

		final var result = service.listForErrand("errand-1");

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo("n1");
		assertThat(result.getFirst().getBody()).isEqualTo("b1");
		assertThat(result.getFirst().getAuthor()).isEqualTo("a1");
		assertThat(result.getFirst().getCreated()).isEqualTo(ts);
	}

	@Test
	void readReturnsNote() {
		when(repositoryMock.findById("n1")).thenReturn(Optional.of(
			NoteEntity.create().withId("n1").withBody("b").withErrandId("e1")));

		final var result = service.read("n1");

		assertThat(result.getId()).isEqualTo("n1");
		assertThat(result.getBody()).isEqualTo("b");
		verify(eventsMock, never()).publishEvent(any());
	}

	@Test
	void readNotFound() {
		when(repositoryMock.findById("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read("missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void deleteCallsRepository() {
		service.delete("n1");
		verify(repositoryMock).deleteById("n1");
	}
}
