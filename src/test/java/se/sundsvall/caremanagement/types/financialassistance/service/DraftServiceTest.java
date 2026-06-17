package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.api.model.DraftIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormberakningDraftRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormberakningDraftEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class DraftServiceTest {

	private static final String ERRAND_ID = "errand-1";

	@Mock
	private FaNormberakningDraftRepository repositoryMock;

	private final JsonMapper mapper = JsonMapper.builder().build();
	private DraftService service;

	@BeforeEach
	void setup() {
		service = new DraftService(repositoryMock, mapper);
	}

	private String rowsJson(final String... typeNames) {
		final var rows = java.util.Arrays.stream(typeNames).map(name -> DraftIncomeRow.create().withTypeName(name).withApplicantAmount(100.0)).toList();
		return mapper.writeValueAsString(rows);
	}

	private static List<DraftIncomeRow> rows(final String... typeNames) {
		return java.util.Arrays.stream(typeNames).map(name -> DraftIncomeRow.create().withTypeName(name).withApplicantAmount(100.0)).toList();
	}

	@Test
	void refreshCreatesDraftWhenNone() {
		when(repositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());

		final var newIncome = service.refresh(ERRAND_ID, "2026-06", rows("Bostadsbidrag"));

		assertThat(newIncome).isEmpty();
		final var captor = ArgumentCaptor.forClass(FaNormberakningDraftEntity.class);
		verify(repositoryMock).save(captor.capture());
		assertThat(captor.getValue().getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(captor.getValue().isEdited()).isFalse();
		assertThat(captor.getValue().getRowsJson()).contains("Bostadsbidrag");
	}

	@Test
	void refreshOverwritesWhenUntouched() {
		final var existing = FaNormberakningDraftEntity.create().withErrandId(ERRAND_ID).withEdited(false).withRowsJson(rowsJson("Gammal"));
		when(repositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(existing));

		final var newIncome = service.refresh(ERRAND_ID, "2026-06", rows("Bostadsbidrag"));

		assertThat(newIncome).isEmpty();
		final var captor = ArgumentCaptor.forClass(FaNormberakningDraftEntity.class);
		verify(repositoryMock).save(captor.capture());
		assertThat(captor.getValue().getRowsJson()).contains("Bostadsbidrag").doesNotContain("Gammal");
	}

	@Test
	void refreshPreservesEditedAndReportsNewIncome() {
		final var edited = FaNormberakningDraftEntity.create().withErrandId(ERRAND_ID).withEdited(true).withRowsJson(rowsJson("Bostadsbidrag"));
		when(repositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(edited));

		final var newIncome = service.refresh(ERRAND_ID, "2026-06", rows("Bostadsbidrag", "Lön"));

		assertThat(newIncome).containsExactly("Lön"); // Bostadsbidrag already in the edited draft → only Lön is new
		verify(repositoryMock, never()).save(any()); // edited draft is preserved
	}

	@Test
	void getYields404WhenMissing() {
		when(repositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void getReturnsDraftWithDeserializedRows() {
		when(repositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(
			FaNormberakningDraftEntity.create().withErrandId(ERRAND_ID).withApplicationMonth("2026-06").withEdited(true).withRowsJson(rowsJson("Bostadsbidrag"))));

		final var draft = service.get(ERRAND_ID);

		assertThat(draft.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(draft.isEdited()).isTrue();
		assertThat(draft.getRows()).extracting(DraftIncomeRow::getTypeName).containsExactly("Bostadsbidrag");
	}

	@Test
	void replaceMarksEditedAndStoresRows() {
		when(repositoryMock.findById(ERRAND_ID)).thenReturn(Optional.empty());
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var draft = service.replace(ERRAND_ID, "2026-06", rows("Bostadsbidrag", "Lön"));

		assertThat(draft.isEdited()).isTrue();
		final var captor = ArgumentCaptor.forClass(FaNormberakningDraftEntity.class);
		verify(repositoryMock).save(captor.capture());
		assertThat(captor.getValue().isEdited()).isTrue();
		assertThat(captor.getValue().getRowsJson()).contains("Bostadsbidrag").contains("Lön");
	}

	@Test
	void editedRowsPresentOnlyWhenEdited() {
		when(repositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(
			FaNormberakningDraftEntity.create().withErrandId(ERRAND_ID).withEdited(false).withRowsJson(rowsJson("Bostadsbidrag"))));

		assertThat(service.editedRows(ERRAND_ID)).isEmpty();
	}

	@Test
	void editedRowsReturnedWhenEdited() {
		when(repositoryMock.findById(ERRAND_ID)).thenReturn(Optional.of(
			FaNormberakningDraftEntity.create().withErrandId(ERRAND_ID).withEdited(true).withRowsJson(rowsJson("Bostadsbidrag"))));

		assertThat(service.editedRows(ERRAND_ID)).get().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(DraftIncomeRow.class))
			.extracting(DraftIncomeRow::getTypeName).containsExactly("Bostadsbidrag");
	}
}
