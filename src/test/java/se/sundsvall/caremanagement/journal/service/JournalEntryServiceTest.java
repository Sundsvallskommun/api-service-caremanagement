package se.sundsvall.caremanagement.journal.service;

import java.time.LocalDate;
import java.time.LocalTime;
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
import se.sundsvall.caremanagement.journal.api.model.CreateJournalEntry;
import se.sundsvall.caremanagement.journal.api.model.LockJournalEntry;
import se.sundsvall.caremanagement.journal.api.model.UpdateJournalEntry;
import se.sundsvall.caremanagement.journal.integration.db.JournalEntryRepository;
import se.sundsvall.caremanagement.journal.integration.db.model.JournalEntryEntity;
import se.sundsvall.caremanagement.journal.service.event.JournalEntryAdded;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.journal.integration.db.model.JournalEntryStatus.LOCKED;
import static se.sundsvall.caremanagement.journal.integration.db.model.JournalEntryStatus.WORKING;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final LocalDate ENTRY_DATE = LocalDate.parse("2025-05-30");
	private static final LocalTime ENTRY_TIME = LocalTime.of(14, 30);

	@Mock
	private JournalEntryRepository repositoryMock;

	@Mock
	private ApplicationEventPublisher eventsMock;

	@InjectMocks
	private JournalEntryService service;

	@Test
	void addPublishesEventAndReturnsId() {
		final var saved = JournalEntryEntity.create().withId("je-1").withErrandId("errand-1");
		when(repositoryMock.save(any(JournalEntryEntity.class))).thenReturn(saved);

		final var id = service.add("errand-1", new CreateJournalEntry("Journalfört meddelande", "Rubrik", "text", ENTRY_DATE, ENTRY_TIME, "carola"));

		assertThat(id).isEqualTo("je-1");

		final ArgumentCaptor<JournalEntryEntity> entityCaptor = ArgumentCaptor.forClass(JournalEntryEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		final var captured = entityCaptor.getValue();
		assertThat(captured.getErrandId()).isEqualTo("errand-1");
		assertThat(captured.getType()).isEqualTo("Journalfört meddelande");
		assertThat(captured.getHeading()).isEqualTo("Rubrik");
		assertThat(captured.getText()).isEqualTo("text");
		assertThat(captured.getEntryDate()).isEqualTo(ENTRY_DATE);
		assertThat(captured.getEntryTime()).isEqualTo(ENTRY_TIME);
		assertThat(captured.getStatus()).isEqualTo(WORKING);
		assertThat(captured.getCreatedBy()).isEqualTo("carola");
		assertThat(captured.getCreated()).isNotNull();

		final ArgumentCaptor<JournalEntryAdded> eventCaptor = ArgumentCaptor.forClass(JournalEntryAdded.class);
		verify(eventsMock).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().journalEntryId()).isEqualTo("je-1");
		assertThat(eventCaptor.getValue().errandId()).isEqualTo("errand-1");
		assertThat(eventCaptor.getValue().type()).isEqualTo("Journalfört meddelande");
		assertThat(eventCaptor.getValue().createdBy()).isEqualTo("carola");
	}

	@Test
	void listForErrandReturnsMappedEntries() {
		when(repositoryMock.findByErrandIdOrderByEntryDateDescEntryTimeDescCreatedDesc("errand-1")).thenReturn(List.of(
			JournalEntryEntity.create().withId("je1").withErrandId("errand-1").withType("T").withHeading("H")
				.withText("b").withEntryDate(ENTRY_DATE).withEntryTime(ENTRY_TIME).withStatus(WORKING).withCreated(FIXED_TIMESTAMP)));

		final var result = service.listForErrand("errand-1");

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo("je1");
		assertThat(result.getFirst().getHeading()).isEqualTo("H");
		assertThat(result.getFirst().getEntryDate()).isEqualTo(ENTRY_DATE);
		assertThat(result.getFirst().getStatus()).isEqualTo("WORKING");
		assertThat(result.getFirst().getCreated()).isEqualTo(FIXED_TIMESTAMP);
	}

	@Test
	void readReturnsEntry() {
		when(repositoryMock.findById("je1")).thenReturn(Optional.of(
			JournalEntryEntity.create().withId("je1").withErrandId("e1").withHeading("H").withStatus(WORKING)));

		final var result = service.read("je1");

		assertThat(result.getId()).isEqualTo("je1");
		assertThat(result.getHeading()).isEqualTo("H");
		assertThat(result.getStatus()).isEqualTo("WORKING");
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
	void updateUpdatesFieldsAndReturnsEntry() {
		final var existing = JournalEntryEntity.create().withId("je1").withErrandId("e1").withType("old").withHeading("oldH")
			.withEntryDate(ENTRY_DATE).withStatus(WORKING).withCreated(FIXED_TIMESTAMP);
		when(repositoryMock.findByIdForUpdate("je1")).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any(JournalEntryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.update("je1", new UpdateJournalEntry("newT", "newH", "newText", ENTRY_DATE.plusDays(1), ENTRY_TIME, "editor"));

		assertThat(result.getType()).isEqualTo("newT");
		assertThat(result.getHeading()).isEqualTo("newH");
		assertThat(result.getText()).isEqualTo("newText");
		assertThat(result.getEntryDate()).isEqualTo(ENTRY_DATE.plusDays(1));
		assertThat(result.getEntryTime()).isEqualTo(ENTRY_TIME);
		assertThat(result.getModifiedBy()).isEqualTo("editor");
		assertThat(result.getModified()).isNotNull();
		assertThat(result.getStatus()).isEqualTo("WORKING");
		assertThat(result.getCreated()).isEqualTo(FIXED_TIMESTAMP);
	}

	@Test
	void updateNotFound() {
		when(repositoryMock.findByIdForUpdate("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update("missing", new UpdateJournalEntry("t", "h", null, ENTRY_DATE, null, "editor")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void updateLockedConflicts() {
		when(repositoryMock.findByIdForUpdate("je1")).thenReturn(Optional.of(
			JournalEntryEntity.create().withId("je1").withStatus(LOCKED)));

		assertThatThrownBy(() -> service.update("je1", new UpdateJournalEntry("t", "h", null, ENTRY_DATE, null, "editor")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void deleteRemovesWorkingEntry() {
		final var existing = JournalEntryEntity.create().withId("je1").withStatus(WORKING);
		when(repositoryMock.findByIdForUpdate("je1")).thenReturn(Optional.of(existing));

		service.delete("je1");

		verify(repositoryMock).delete(existing);
	}

	@Test
	void deleteNotFound() {
		when(repositoryMock.findByIdForUpdate("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete("missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).delete(any());
	}

	@Test
	void deleteLockedConflicts() {
		when(repositoryMock.findByIdForUpdate("je1")).thenReturn(Optional.of(
			JournalEntryEntity.create().withId("je1").withStatus(LOCKED)));

		assertThatThrownBy(() -> service.delete("je1"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(repositoryMock, never()).delete(any());
	}

	@Test
	void lockSetsLockedStatus() {
		final var existing = JournalEntryEntity.create().withId("je1").withStatus(WORKING).withCreated(FIXED_TIMESTAMP);
		when(repositoryMock.findByIdForUpdate("je1")).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any(JournalEntryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.lock("je1", new LockJournalEntry("carola"));

		assertThat(result.getStatus()).isEqualTo("LOCKED");
		assertThat(result.getLockedBy()).isEqualTo("carola");
		assertThat(result.getLocked()).isNotNull();
	}

	@Test
	void lockWithoutBodyLeavesLockedByNull() {
		final var existing = JournalEntryEntity.create().withId("je1").withStatus(WORKING);
		when(repositoryMock.findByIdForUpdate("je1")).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any(JournalEntryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.lock("je1", null);

		assertThat(result.getStatus()).isEqualTo("LOCKED");
		assertThat(result.getLockedBy()).isNull();
		assertThat(result.getLocked()).isNotNull();
	}

	@Test
	void lockNotFound() {
		when(repositoryMock.findByIdForUpdate("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.lock("missing", new LockJournalEntry("carola")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void lockAlreadyLockedConflicts() {
		when(repositoryMock.findByIdForUpdate("je1")).thenReturn(Optional.of(
			JournalEntryEntity.create().withId("je1").withStatus(LOCKED)));

		assertThatThrownBy(() -> service.lock("je1", new LockJournalEntry("carola")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(repositoryMock, never()).save(any());
	}
}
