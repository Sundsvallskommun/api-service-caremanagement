package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaWarningRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaWarningEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class WarningServiceTest {

	private static final String ERRAND_ID = "errand-1";

	@Mock
	private FaWarningRepository repositoryMock;

	@InjectMocks
	private WarningService service;

	private static FaWarningEntity warning(final String type, final String sourceKey, final String status) {
		return FaWarningEntity.create().withId("w-" + sourceKey).withErrandId(ERRAND_ID)
			.withType(type).withSourceKey(sourceKey).withMessage("msg").withStatus(status);
	}

	@Test
	void reconcileIncomeWarningsCreatesTypedWarningsWithSourceKeys() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		service.reconcileIncomeWarnings(ERRAND_ID,
			List.of("Bostadstillägg (NOT_ON_WHITELIST)"),
			List.of("Bostadsbidrag: -23%"),
			List.of("Dagersättning"),
			List.of("Lön"));

		final var captor = ArgumentCaptor.forClass(FaWarningEntity.class);
		verify(repositoryMock, org.mockito.Mockito.times(4)).save(captor.capture());
		final var saved = captor.getAllValues();
		assertThat(saved).allMatch(w -> "OPEN".equals(w.getStatus()) && !w.isAutoResolved());
		assertThat(saved).extracting(FaWarningEntity::getType, FaWarningEntity::getSourceKey)
			.containsExactlyInAnyOrder(
				tuple("UNHANDLED_INCOME", "Bostadstillägg"),
				tuple("INCOME_CHANGE", "Bostadsbidrag"),
				tuple("MISSING_SSBTEK", "Dagersättning"),
				tuple("NEW_INCOME", "Lön"));
	}

	@Test
	void reconcileUpdatesOpenWarningButNeverReopensClosed() {
		final var open = warning("UNHANDLED_INCOME", "Bostadstillägg", "OPEN");
		final var closed = warning("INCOME_CHANGE", "Bostadsbidrag", "CLOSED");
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(open, closed));

		service.reconcileIncomeWarnings(ERRAND_ID,
			List.of("Bostadstillägg (NOT_ON_WHITELIST)"), // matches the OPEN one → update
			List.of("Bostadsbidrag: -23%"), // matches the CLOSED one → must NOT re-open
			List.of(),
			List.of());

		final var captor = ArgumentCaptor.forClass(FaWarningEntity.class);
		verify(repositoryMock).save(captor.capture()); // exactly one save — the open one
		assertThat(captor.getValue().getType()).isEqualTo("UNHANDLED_INCOME");
		assertThat(captor.getValue().getStatus()).isEqualTo("OPEN");
	}

	@Test
	void reconcileAutoClosesResolvedWarnings() {
		final var open = warning("MISSING_SSBTEK", "Dagersättning", "OPEN");
		final var acknowledged = warning("UNHANDLED_INCOME", "Bostadstillägg", "ACKNOWLEDGED");
		final var alreadyClosed = warning("INCOME_CHANGE", "Bostadsbidrag", "CLOSED");
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(open, acknowledged, alreadyClosed));

		// nothing computed this round → all causes resolved
		service.reconcileIncomeWarnings(ERRAND_ID, List.of(), List.of(), List.of(), List.of());

		final var captor = ArgumentCaptor.forClass(FaWarningEntity.class);
		verify(repositoryMock, org.mockito.Mockito.times(2)).save(captor.capture()); // open + acknowledged auto-close; closed untouched
		assertThat(captor.getAllValues()).allMatch(w -> "CLOSED".equals(w.getStatus()) && w.isAutoResolved());
		assertThat(captor.getAllValues()).extracting(FaWarningEntity::getType)
			.containsExactlyInAnyOrder("MISSING_SSBTEK", "UNHANDLED_INCOME");
	}

	@Test
	void updateStatusAcknowledges() {
		final var entity = warning("MISSING_SSBTEK", "Dagersättning", "OPEN").withCreated(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
		when(repositoryMock.findByIdAndErrandId("w-Dagersättning", ERRAND_ID)).thenReturn(Optional.of(entity));
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.updateStatus(ERRAND_ID, "w-Dagersättning", "ACKNOWLEDGED");

		assertThat(result.getStatus()).isEqualTo("ACKNOWLEDGED");
		assertThat(result.isAutoResolved()).isFalse();
	}

	@Test
	void updateStatusUnknownWarningYields404() {
		when(repositoryMock.findByIdAndErrandId("missing", ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateStatus(ERRAND_ID, "missing", "CLOSED"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void updateStatusReopensToOpen() {
		final var entity = warning("MISSING_SSBTEK", "Dagersättning", "ACKNOWLEDGED").withCreated(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
		when(repositoryMock.findByIdAndErrandId("w-Dagersättning", ERRAND_ID)).thenReturn(Optional.of(entity));
		when(repositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.updateStatus(ERRAND_ID, "w-Dagersättning", "OPEN");

		assertThat(result.getStatus()).isEqualTo("OPEN");
		assertThat(result.isAutoResolved()).isFalse();
	}

	@Test
	void countActiveCountsNonClosedWarnings() {
		when(repositoryMock.countByErrandIdAndStatusNot(ERRAND_ID, WarningService.STATUS_CLOSED)).thenReturn(3L);

		assertThat(service.countActive(ERRAND_ID)).isEqualTo(3L);
		verify(repositoryMock).countByErrandIdAndStatusNot(ERRAND_ID, WarningService.STATUS_CLOSED);
	}

	@Test
	void updateStatusInvalidTargetYields400() {
		assertThatThrownBy(() -> service.updateStatus(ERRAND_ID, "w-1", "BOGUS"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(repositoryMock, never()).findByIdAndErrandId(any(), any());
	}

	@Test
	void reconcileCalculationWarningsFoldsAllSections() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		final var changes = new se.sundsvall.caremanagement.types.financialassistance.service.model.DraftChanges(
			List.of("Lön (APPLICANT)"), List.of("Pension (APPLICANT)"),
			List.of("RENT"), List.of(),
			List.of("Barn (CHILD)"), List.of());

		service.reconcileCalculationWarnings(ERRAND_ID,
			List.of("Bostadstillägg (NOT_ON_WHITELIST)"),
			List.of("Bostadsbidrag: -23%"),
			List.of("Dagersättning"),
			changes,
			List.of(
				new WarningService.WarningInput(WarningService.TYPE_HOUSEHOLD_CHANGE, "hushall-storlek", "Antal household members ändrat"),
				new WarningService.WarningInput(WarningService.TYPE_HOUSING_COST_CHANGE, "housing-kostnad", "Housing cost changed +32%"),
				new WarningService.WarningInput(WarningService.TYPE_EXPENSE_REVIEW, "OTHER", "OTHER: reasonableness bedöms manuellt"),
				new WarningService.WarningInput(WarningService.TYPE_EXPENSE_CAPPED, "RENT", "Capped cost: RENT")));

		final var captor = ArgumentCaptor.forClass(FaWarningEntity.class);
		// 3 income/change/missing + NEW_INCOME + NEW_EXPENSE + NEW_PERSON + INCOME_DROPPED (draft) + 4 section warnings
		// (HOUSEHOLD_CHANGE + HOUSING_COST_CHANGE + EXPENSE_REVIEW + EXPENSE_CAPPED) = 11
		verify(repositoryMock, org.mockito.Mockito.times(11)).save(captor.capture());
		assertThat(captor.getAllValues()).extracting(FaWarningEntity::getType)
			.containsExactlyInAnyOrder("UNHANDLED_INCOME", "INCOME_CHANGE", "MISSING_SSBTEK",
				"NEW_INCOME", "NEW_EXPENSE", "NEW_PERSON", "INCOME_DROPPED",
				"HOUSEHOLD_CHANGE", "HOUSING_COST_CHANGE", "EXPENSE_REVIEW", "EXPENSE_CAPPED");
	}

	@Test
	void reconcileCalculationWarningsToleratesNullDraftChanges() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		service.reconcileCalculationWarnings(ERRAND_ID, List.of("X (Y)"), List.of(), List.of(), null, null);

		verify(repositoryMock).save(any()); // only the single unhandled-income warning
	}

	@Test
	void listReturnsMappedWarnings() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			warning("MISSING_SSBTEK", "Dagersättning", "OPEN").withCreated(OffsetDateTime.parse("2026-06-02T00:00:00Z")),
			warning("UNHANDLED_INCOME", "Bostadstillägg", "ACKNOWLEDGED").withCreated(OffsetDateTime.parse("2026-06-01T00:00:00Z"))));

		final var result = service.list(ERRAND_ID);

		assertThat(result).extracting("type").containsExactly("UNHANDLED_INCOME", "MISSING_SSBTEK"); // sorted by created asc
	}
}
