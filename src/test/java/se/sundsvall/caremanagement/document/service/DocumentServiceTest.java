package se.sundsvall.caremanagement.document.service;

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
import se.sundsvall.caremanagement.document.api.model.CreateDocument;
import se.sundsvall.caremanagement.document.api.model.LockDocument;
import se.sundsvall.caremanagement.document.api.model.UpdateDocument;
import se.sundsvall.caremanagement.document.integration.db.DocumentRepository;
import se.sundsvall.caremanagement.document.integration.db.model.DocumentEntity;
import se.sundsvall.caremanagement.document.service.event.DocumentAdded;
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
import static se.sundsvall.caremanagement.document.integration.db.model.DocumentStatus.LOCKED;
import static se.sundsvall.caremanagement.document.integration.db.model.DocumentStatus.WORKING;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final LocalDate DOCUMENT_DATE = LocalDate.parse("2025-05-30");
	private static final LocalTime DOCUMENT_TIME = LocalTime.of(14, 30);
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private DocumentRepository repositoryMock;

	@Mock
	private ApplicationEventPublisher eventsMock;

	@Mock
	private ErrandAccessGuard errandGuardMock;

	@InjectMocks
	private DocumentService service;

	@Test
	void addPublishesEventAndReturnsId() {
		final var saved = DocumentEntity.create().withId("doc-1").withErrandId(ERRAND_ID);
		when(repositoryMock.save(any(DocumentEntity.class))).thenReturn(saved);

		final var id = service.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new CreateDocument("Brev", "Rubrik", "text", DOCUMENT_DATE, DOCUMENT_TIME, "carola"));

		assertThat(id).isEqualTo("doc-1");

		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		final ArgumentCaptor<DocumentEntity> entityCaptor = ArgumentCaptor.forClass(DocumentEntity.class);
		verify(repositoryMock).save(entityCaptor.capture());
		final var captured = entityCaptor.getValue();
		assertThat(captured.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(captured.getType()).isEqualTo("Brev");
		assertThat(captured.getHeading()).isEqualTo("Rubrik");
		assertThat(captured.getText()).isEqualTo("text");
		assertThat(captured.getDocumentDate()).isEqualTo(DOCUMENT_DATE);
		assertThat(captured.getDocumentTime()).isEqualTo(DOCUMENT_TIME);
		assertThat(captured.getStatus()).isEqualTo(WORKING);
		assertThat(captured.getCreatedBy()).isEqualTo("carola");
		assertThat(captured.getCreated()).isNotNull();

		final ArgumentCaptor<DocumentAdded> eventCaptor = ArgumentCaptor.forClass(DocumentAdded.class);
		verify(eventsMock).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().documentId()).isEqualTo("doc-1");
		assertThat(eventCaptor.getValue().errandId()).isEqualTo(ERRAND_ID);
		assertThat(eventCaptor.getValue().type()).isEqualTo("Brev");
		assertThat(eventCaptor.getValue().createdBy()).isEqualTo("carola");
	}

	@Test
	void addMissingErrandNotFound() {
		doThrow(Problem.valueOf(NOT_FOUND, "no errand")).when(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThatThrownBy(() -> service.add(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, new CreateDocument("T", "H", null, DOCUMENT_DATE, null, "carola")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(repositoryMock);
		verifyNoInteractions(eventsMock);
	}

	@Test
	void listForErrandReturnsMappedDocuments() {
		when(repositoryMock.findByErrandIdOrderByDocumentDateDescDocumentTimeDescCreatedDesc(ERRAND_ID)).thenReturn(List.of(
			DocumentEntity.create().withId("d1").withErrandId(ERRAND_ID).withType("T").withHeading("H")
				.withText("b").withDocumentDate(DOCUMENT_DATE).withDocumentTime(DOCUMENT_TIME).withStatus(WORKING).withCreated(FIXED_TIMESTAMP)));

		final var result = service.listForErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().getId()).isEqualTo("d1");
		assertThat(result.getFirst().getHeading()).isEqualTo("H");
		assertThat(result.getFirst().getDocumentDate()).isEqualTo(DOCUMENT_DATE);
		assertThat(result.getFirst().getStatus()).isEqualTo("WORKING");
		assertThat(result.getFirst().getCreated()).isEqualTo(FIXED_TIMESTAMP);
		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void readReturnsDocument() {
		when(repositoryMock.findByIdAndErrandId("d1", ERRAND_ID)).thenReturn(Optional.of(
			DocumentEntity.create().withId("d1").withErrandId(ERRAND_ID).withHeading("H").withStatus(WORKING)));

		final var result = service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "d1");

		assertThat(result.getId()).isEqualTo("d1");
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

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "d1"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(repositoryMock);
	}

	/**
	 * Cross-tenant guard: the errand exists in the tenant but the document id belongs to a different errand. The scoped
	 * query returns empty, so it must surface as a 404 rather than leaking the foreign document.
	 */
	@Test
	void readForeignErrandNotFound() {
		when(repositoryMock.findByIdAndErrandId("doc-from-other-errand", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.read(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "doc-from-other-errand"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(repositoryMock).findByIdAndErrandId("doc-from-other-errand", ERRAND_ID);
	}

	@Test
	void updateUpdatesFieldsAndReturnsDocument() {
		final var existing = DocumentEntity.create().withId("d1").withErrandId(ERRAND_ID).withType("old").withHeading("oldH")
			.withDocumentDate(DOCUMENT_DATE).withStatus(WORKING).withCreated(FIXED_TIMESTAMP);
		when(repositoryMock.findByIdAndErrandIdForUpdate("d1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "d1", new UpdateDocument("newT", "newH", "newText", DOCUMENT_DATE.plusDays(1), DOCUMENT_TIME, "editor"));

		assertThat(result.getType()).isEqualTo("newT");
		assertThat(result.getHeading()).isEqualTo("newH");
		assertThat(result.getText()).isEqualTo("newText");
		assertThat(result.getDocumentDate()).isEqualTo(DOCUMENT_DATE.plusDays(1));
		assertThat(result.getDocumentTime()).isEqualTo(DOCUMENT_TIME);
		assertThat(result.getModifiedBy()).isEqualTo("editor");
		assertThat(result.getModified()).isNotNull();
		assertThat(result.getStatus()).isEqualTo("WORKING");
		assertThat(result.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void updateNotFound() {
		when(repositoryMock.findByIdAndErrandIdForUpdate("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing", new UpdateDocument("t", "h", null, DOCUMENT_DATE, null, "editor")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void updateLockedConflicts() {
		when(repositoryMock.findByIdAndErrandIdForUpdate("d1", ERRAND_ID)).thenReturn(Optional.of(
			DocumentEntity.create().withId("d1").withStatus(LOCKED)));

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "d1", new UpdateDocument("t", "h", null, DOCUMENT_DATE, null, "editor")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void deleteRemovesWorkingDocument() {
		final var existing = DocumentEntity.create().withId("d1").withStatus(WORKING);
		when(repositoryMock.findByIdAndErrandIdForUpdate("d1", ERRAND_ID)).thenReturn(Optional.of(existing));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "d1");

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
		when(repositoryMock.findByIdAndErrandIdForUpdate("d1", ERRAND_ID)).thenReturn(Optional.of(
			DocumentEntity.create().withId("d1").withStatus(LOCKED)));

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "d1"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(repositoryMock, never()).delete(any());
	}

	@Test
	void lockSetsLockedStatus() {
		final var existing = DocumentEntity.create().withId("d1").withStatus(WORKING).withCreated(FIXED_TIMESTAMP);
		when(repositoryMock.findByIdAndErrandIdForUpdate("d1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "d1", new LockDocument("carola"));

		assertThat(result.getStatus()).isEqualTo("LOCKED");
		assertThat(result.getLockedBy()).isEqualTo("carola");
		assertThat(result.getLocked()).isNotNull();
		verify(errandGuardMock).verifyExistingErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void lockWithoutBodyLeavesLockedByNull() {
		final var existing = DocumentEntity.create().withId("d1").withStatus(WORKING);
		when(repositoryMock.findByIdAndErrandIdForUpdate("d1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any(DocumentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "d1", null);

		assertThat(result.getStatus()).isEqualTo("LOCKED");
		assertThat(result.getLockedBy()).isNull();
		assertThat(result.getLocked()).isNotNull();
	}

	@Test
	void lockNotFound() {
		when(repositoryMock.findByIdAndErrandIdForUpdate("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing", new LockDocument("carola")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void lockAlreadyLockedConflicts() {
		when(repositoryMock.findByIdAndErrandIdForUpdate("d1", ERRAND_ID)).thenReturn(Optional.of(
			DocumentEntity.create().withId("d1").withStatus(LOCKED)));

		assertThatThrownBy(() -> service.lock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "d1", new LockDocument("carola")))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", CONFLICT);

		verify(repositoryMock, never()).save(any());
	}
}
