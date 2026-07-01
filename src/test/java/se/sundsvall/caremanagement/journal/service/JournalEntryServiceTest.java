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
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private JournalEntryRepository repositoryMock;

	@Mock
	private ApplicationEventPublisher eventsMock;

	@Mock
	private ErrandAccessGuard errandGuardMock;

	@InjectMocks
	private JournalEntryService service;

	@Test
	void addPublishesEventAndReturnsId() {
		final var saved = JournalEntryEntity.create().withId("je-1").withErrandId(ERRAND_ID);
		when(repositoryMock.save(any(JournalEntryEntity.class))).thenReturn(saved);

		final var id = service.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new CreateJournalEntry("Journalfört meddelande", "Rubrik", "text", ENTRY_DATE, ENTRY_TIME, "carola"));

		assertThat(id).isEqualTo("je-1");

		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		final ArgumentCaptor<JournalEntryEntity> entityCaptor = ArgumentCaptor.forClass(JournalEntryEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		final var captured = entityCaptor.getValue();
		assertThat(captured.getErrandId()).isEqualTo(ERRAND_ID);
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
		assertThat(eventCaptor.getValue().errandId()).isEqualTo(ERRAND_ID);
		assertThat(eventCaptor.getValue().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(eventCaptor.getValue().namespace()).isEqualTo(NAMESPACE);
		assertThat(eventCaptor.getValue().type()).isEqualTo("Journalfört meddelande");
		assertThat(eventCaptor.getValue().createdBy()).isEqualTo("carola");
	}

	@Test
	void addMissingErrandNotFound() {
		doThrow(Problem.valueOf(NOT_FOUND, "no errand")).when(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThatThrownBy(() -> service.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new CreateJournalEntry("T", "H", null, ENTRY_DATE, null, "carola")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(repositoryMock);
		verifyNoInteractions(eventsMock);
	}

	@Test
	void listForErrandReturnsMappedEntries() {
		when(repositoryMock.findByErrandIdOrderByEntryDateDescEntryTimeDescCreatedDesc(ERRAND_ID)).thenReturn(List.of(
			JournalEntryEntity.create().withId("je1").withErrandId(ERRAND_ID).withType("T").withHeading("H")
				.withText("b").withEntryDate(ENTRY_DATE).withEntryTime(ENTRY_TIME).withStatus(WORKING).withCreated(FIXED_TIMESTAMP)));

		final var result = service.listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo("je1");
		assertThat(result.getFirst().getHeading()).isEqualTo("H");
		assertThat(result.getFirst().getEntryDate()).isEqualTo(ENTRY_DATE);
		assertThat(result.getFirst().getStatus()).isEqualTo("WORKING");
		assertThat(result.getFirst().getCreated()).isEqualTo(FIXED_TIMESTAMP);
		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readReturnsEntry() {
		when(repositoryMock.findByIdAndErrandId("je1", ERRAND_ID)).thenReturn(Optional.of(
			JournalEntryEntity.create().withId("je1").withErrandId(ERRAND_ID).withHeading("H").withStatus(WORKING)));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "je1");

		assertThat(result.getId()).isEqualTo("je1");
		assertThat(result.getHeading()).isEqualTo("H");
		assertThat(result.getStatus()).isEqualTo("WORKING");
		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(eventsMock, never()).publishEvent(any());
	}

	@Test
	void readNotFound() {
		when(repositoryMock.findByIdAndErrandId("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void readMissingErrandNotFound() {
		doThrow(Problem.valueOf(NOT_FOUND, "no errand")).when(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "je1"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(repositoryMock);
	}

	/**
	 * Cross-tenant guard: the errand exists in the tenant but the entry id belongs to a different errand. The scoped
	 * query returns empty, so it must surface as a 404 rather than leaking the foreign entry.
	 */
	@Test
	void readForeignErrandNotFound() {
		when(repositoryMock.findByIdAndErrandId("je-from-other-errand", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "je-from-other-errand"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(repositoryMock).findByIdAndErrandId("je-from-other-errand", ERRAND_ID);
	}

	@Test
	void updateUpdatesFieldsAndReturnsEntry() {
		final var existing = JournalEntryEntity.create().withId("je1").withErrandId(ERRAND_ID).withType("old").withHeading("oldH")
			.withEntryDate(ENTRY_DATE).withStatus(WORKING).withCreated(FIXED_TIMESTAMP);
		when(repositoryMock.findByIdAndErrandIdForUpdate("je1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any(JournalEntryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "je1", new UpdateJournalEntry("newT", "newH", "newText", ENTRY_DATE.plusDays(1), ENTRY_TIME, "editor"));

		assertThat(result.getType()).isEqualTo("newT");
		assertThat(result.getHeading()).isEqualTo("newH");
		assertThat(result.getText()).isEqualTo("newText");
		assertThat(result.getEntryDate()).isEqualTo(ENTRY_DATE.plusDays(1));
		assertThat(result.getEntryTime()).isEqualTo(ENTRY_TIME);
		assertThat(result.getModifiedBy()).isEqualTo("editor");
		assertThat(result.getModified()).isNotNull();
		assertThat(result.getStatus()).isEqualTo("WORKING");
		assertThat(result.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void updateNotFound() {
		when(repositoryMock.findByIdAndErrandIdForUpdate("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing", new UpdateJournalEntry("t", "h", null, ENTRY_DATE, null, "editor")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void updateLockedConflicts() {
		when(repositoryMock.findByIdAndErrandIdForUpdate("je1", ERRAND_ID)).thenReturn(Optional.of(
			JournalEntryEntity.create().withId("je1").withStatus(LOCKED)));

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "je1", new UpdateJournalEntry("t", "h", null, ENTRY_DATE, null, "editor")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void deleteRemovesWorkingEntry() {
		final var existing = JournalEntryEntity.create().withId("je1").withStatus(WORKING);
		when(repositoryMock.findByIdAndErrandIdForUpdate("je1", ERRAND_ID)).thenReturn(Optional.of(existing));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "je1");

		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(repositoryMock).delete(existing);
	}

	@Test
	void deleteNotFound() {
		when(repositoryMock.findByIdAndErrandIdForUpdate("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).delete(any());
	}

	@Test
	void deleteLockedConflicts() {
		when(repositoryMock.findByIdAndErrandIdForUpdate("je1", ERRAND_ID)).thenReturn(Optional.of(
			JournalEntryEntity.create().withId("je1").withStatus(LOCKED)));

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "je1"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(repositoryMock, never()).delete(any());
	}

	@Test
	void lockSetsLockedStatus() {
		final var existing = JournalEntryEntity.create().withId("je1").withStatus(WORKING).withCreated(FIXED_TIMESTAMP);
		when(repositoryMock.findByIdAndErrandIdForUpdate("je1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any(JournalEntryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "je1", new LockJournalEntry("carola"));

		assertThat(result.getStatus()).isEqualTo("LOCKED");
		assertThat(result.getLockedBy()).isEqualTo("carola");
		assertThat(result.getLocked()).isNotNull();
		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void lockWithoutBodyLeavesLockedByNull() {
		final var existing = JournalEntryEntity.create().withId("je1").withStatus(WORKING);
		when(repositoryMock.findByIdAndErrandIdForUpdate("je1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any(JournalEntryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "je1", null);

		assertThat(result.getStatus()).isEqualTo("LOCKED");
		assertThat(result.getLockedBy()).isNull();
		assertThat(result.getLocked()).isNotNull();
	}

	@Test
	void lockNotFound() {
		when(repositoryMock.findByIdAndErrandIdForUpdate("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing", new LockJournalEntry("carola")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void lockAlreadyLockedConflicts() {
		when(repositoryMock.findByIdAndErrandIdForUpdate("je1", ERRAND_ID)).thenReturn(Optional.of(
			JournalEntryEntity.create().withId("je1").withStatus(LOCKED)));

		assertThatThrownBy(() -> service.lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "je1", new LockJournalEntry("carola")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(repositoryMock, never()).save(any());
	}
}
