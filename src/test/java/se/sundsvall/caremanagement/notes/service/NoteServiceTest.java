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
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.notes.api.model.CreateNote;
import se.sundsvall.caremanagement.notes.api.model.UpdateNote;
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
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String NOTE_ID = "n1";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private NoteRepository repositoryMock;

	@Mock
	private ApplicationEventPublisher eventsMock;

	@InjectMocks
	private NoteService service;

	private void errandExists() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID)));
	}

	@Test
	void addPublishesEventAndReturnsId() {
		errandExists();
		final var saved = NoteEntity.create().withId(NOTE_ID).withErrandId(ERRAND_ID);
		when(repositoryMock.save(any(NoteEntity.class))).thenReturn(saved);

		final var id = service.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new CreateNote("body", "author"));

		assertThat(id).isEqualTo(NOTE_ID);

		final ArgumentCaptor<NoteEntity> entityCaptor = ArgumentCaptor.forClass(NoteEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entityCaptor.getValue().getBody()).isEqualTo("body");
		assertThat(entityCaptor.getValue().getAuthor()).isEqualTo("author");
		assertThat(entityCaptor.getValue().getCreated()).isNotNull();

		final ArgumentCaptor<NoteAdded> eventCaptor = ArgumentCaptor.forClass(NoteAdded.class);
		verify(eventsMock).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().noteId()).isEqualTo(NOTE_ID);
		assertThat(eventCaptor.getValue().errandId()).isEqualTo(ERRAND_ID);
		assertThat(eventCaptor.getValue().author()).isEqualTo("author");
	}

	@Test
	void addUnknownErrandNotFound() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new CreateNote("body", "author")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
		verify(eventsMock, never()).publishEvent(any());
	}

	@Test
	void listForErrandReturnsMappedNotes() {
		errandExists();
		when(repositoryMock.findByErrandIdOrderByCreatedDesc(ERRAND_ID)).thenReturn(List.of(
			NoteEntity.create().withId(NOTE_ID).withErrandId(ERRAND_ID).withBody("b1").withAuthor("a1").withCreated(FIXED_TIMESTAMP)));

		final var result = service.listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo(NOTE_ID);
		assertThat(result.getFirst().getBody()).isEqualTo("b1");
		assertThat(result.getFirst().getAuthor()).isEqualTo("a1");
		assertThat(result.getFirst().getCreated()).isEqualTo(FIXED_TIMESTAMP);
	}

	@Test
	void readReturnsNote() {
		errandExists();
		when(repositoryMock.findByErrandIdAndId(ERRAND_ID, NOTE_ID)).thenReturn(Optional.of(
			NoteEntity.create().withId(NOTE_ID).withBody("b").withErrandId(ERRAND_ID)));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTE_ID);

		assertThat(result.getId()).isEqualTo(NOTE_ID);
		assertThat(result.getBody()).isEqualTo("b");
		verify(eventsMock, never()).publishEvent(any());
	}

	@Test
	void readNotFoundOnOtherErrand() {
		errandExists();
		when(repositoryMock.findByErrandIdAndId(ERRAND_ID, NOTE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTE_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readNotFoundOnUnknownErrand() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTE_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).findByErrandIdAndId(any(), any());
	}

	@Test
	void updateUpdatesBodyAndReturnsNote() {
		errandExists();
		final var existing = NoteEntity.create().withId(NOTE_ID).withErrandId(ERRAND_ID).withBody("old").withCreated(FIXED_TIMESTAMP);
		when(repositoryMock.findByErrandIdAndId(ERRAND_ID, NOTE_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any(NoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTE_ID, new UpdateNote("new body", "editor"));

		assertThat(result.getBody()).isEqualTo("new body");
		assertThat(result.getModifiedBy()).isEqualTo("editor");
		assertThat(result.getModified()).isNotNull();
		assertThat(result.getCreated()).isEqualTo(FIXED_TIMESTAMP);

		final ArgumentCaptor<NoteEntity> entityCaptor = ArgumentCaptor.forClass(NoteEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		assertThat(entityCaptor.getValue().getBody()).isEqualTo("new body");
		assertThat(entityCaptor.getValue().getModifiedBy()).isEqualTo("editor");
		assertThat(entityCaptor.getValue().getModified()).isNotNull();
		verify(eventsMock, never()).publishEvent(any());
	}

	@Test
	void updateNotFound() {
		errandExists();
		when(repositoryMock.findByErrandIdAndId(ERRAND_ID, NOTE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTE_ID, new UpdateNote("b", "editor")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void deleteRemovesScopedNote() {
		errandExists();
		final var existing = NoteEntity.create().withId(NOTE_ID).withErrandId(ERRAND_ID);
		when(repositoryMock.findByErrandIdAndId(ERRAND_ID, NOTE_ID)).thenReturn(Optional.of(existing));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTE_ID);

		verify(repositoryMock).delete(existing);
	}

	@Test
	void deleteNotFound() {
		errandExists();
		when(repositoryMock.findByErrandIdAndId(ERRAND_ID, NOTE_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, NOTE_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).delete(any());
	}

	@Test
	void countForErrandDelegatesToRepository() {
		errandExists();
		when(repositoryMock.countByErrandId(ERRAND_ID)).thenReturn(4L);

		assertThat(service.countForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEqualTo(4L);
		verify(repositoryMock).countByErrandId(ERRAND_ID);
	}
}
