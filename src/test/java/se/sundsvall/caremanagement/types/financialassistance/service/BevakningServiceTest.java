package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.BevakningRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaBevakningRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaBevakningEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class BevakningServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private FaBevakningRepository repositoryMock;

	@InjectMocks
	private BevakningService service;

	private static FaBevakningEntity entity(final String id, final OffsetDateTime created) {
		return FaBevakningEntity.create().withId(id).withErrandId(ERRAND_ID).withTitle("t-" + id)
			.withStartDate(LocalDate.of(2026, 7, 1)).withCreated(created);
	}

	private static BevakningRequest request() {
		return BevakningRequest.create().withTitle("Följ upp").withDescription("Inväntar underlag")
			.withStartDate(LocalDate.of(2026, 7, 1)).withEndDate(LocalDate.of(2026, 7, 31)).withCreatedBy("joe01doe");
	}

	@Test
	void listReturnsMappedSortedByCreated() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			entity("b2", OffsetDateTime.parse("2026-06-02T00:00:00Z")),
			entity("b1", OffsetDateTime.parse("2026-06-01T00:00:00Z"))));

		final var result = service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).extracting("id").containsExactly("b1", "b2"); // created asc
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void getReturnsBevakning() {
		when(repositoryMock.findByIdAndErrandId("b1", ERRAND_ID)).thenReturn(Optional.of(entity("b1", null)));

		final var result = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "b1");

		assertThat(result.getId()).isEqualTo("b1");
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void getMissingYields404() {
		when(repositoryMock.findByIdAndErrandId("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void createPersistsAndReturns() {
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request());

		final var captor = ArgumentCaptor.forClass(FaBevakningEntity.class);
		verify(repositoryMock).save(captor.capture());
		final var saved = captor.getValue();
		assertThat(saved.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(saved.getTitle()).isEqualTo("Följ upp");
		assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
		assertThat(saved.getCreatedBy()).isEqualTo("joe01doe");
		assertThat(result.getTitle()).isEqualTo("Följ upp");
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void createWithoutEndDateIsAllowed() {
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request().withEndDate(null));

		assertThat(result.getEndDate()).isNull();
		verify(repositoryMock).save(any());
	}

	@Test
	void createWithoutStartDateSkipsDateRangeCheck() {
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		// startDate null (the NotNull guard lives at the API layer) → the range check short-circuits, no 400
		service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, request().withStartDate(null).withEndDate(LocalDate.of(2026, 7, 31)));

		verify(repositoryMock).save(any());
	}

	@Test
	void createWithEndDateBeforeStartYields400() {
		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID,
			request().withStartDate(LocalDate.of(2026, 7, 31)).withEndDate(LocalDate.of(2026, 7, 1))))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(repositoryMock, never()).save(any());
	}

	@Test
	void updateReplacesFields() {
		final var existing = entity("b1", OffsetDateTime.parse("2026-06-01T00:00:00Z")).withTitle("old");
		when(repositoryMock.findByIdAndErrandId("b1", ERRAND_ID)).thenReturn(Optional.of(existing));
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "b1", request());

		assertThat(result.getTitle()).isEqualTo("Följ upp");
		assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void updateMissingYields404() {
		when(repositoryMock.findByIdAndErrandId("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing", request()))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void updateWithEndDateBeforeStartYields400() {
		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "b1",
			request().withStartDate(LocalDate.of(2026, 7, 31)).withEndDate(LocalDate.of(2026, 7, 1))))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(repositoryMock, never()).findByIdAndErrandId(any(), any());
	}

	@Test
	void deleteRemoves() {
		final var existing = entity("b1", null);
		when(repositoryMock.findByIdAndErrandId("b1", ERRAND_ID)).thenReturn(Optional.of(existing));

		service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "b1");

		verify(repositoryMock).delete(existing);
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void deleteMissingYields404() {
		when(repositoryMock.findByIdAndErrandId("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).delete(any());
	}

	@Test
	void scopeCheckPropagatesWhenErrandMissing() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenThrow(se.sundsvall.dept44.problem.Problem.valueOf(NOT_FOUND, "Errand not found"));

		assertThatThrownBy(() -> service.list(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(repositoryMock, never()).findByErrandId(any());
	}
}
