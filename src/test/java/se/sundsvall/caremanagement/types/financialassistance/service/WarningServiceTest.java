package se.sundsvall.caremanagement.types.financialassistance.service;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationIncomeView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaWarningRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaWarningEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.model.DraftChanges;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

	/** The warnings the reconcile raised, rebuilt from its {@code insertIgnore} calls in call order. */
	private List<FaWarningEntity> inserted() {
		return mockingDetails(repositoryMock).getInvocations().stream()
			.filter(invocation -> "insertIgnore".equals(invocation.getMethod().getName()))
			.map(invocation -> FaWarningEntity.create().withId(invocation.getArgument(0)).withErrandId(invocation.getArgument(1))
				.withType(invocation.getArgument(2)).withSourceKey(invocation.getArgument(3)).withMessage(invocation.getArgument(4))
				.withStatus(invocation.getArgument(5)))
			.toList();
	}

	private static FaWarningEntity warning(final String type, final String sourceKey, final String status) {
		return FaWarningEntity.create().withId("w-" + sourceKey).withErrandId(ERRAND_ID)
			.withType(type).withSourceKey(sourceKey).withMessage("msg").withStatus(status);
	}

	@Test
	void reconcileUpdatesOpenWarningButNeverReopensClosed() {
		final var open = warning("UNHANDLED_INCOME", "Bostadstillägg", "OPEN");
		final var closed = warning("INCOME_CHANGE", "Bostadsbidrag", "CLOSED");
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(open, closed));

		service.reconcileCalculationWarnings(ERRAND_ID,
			List.of("Bostadstillägg (NOT_ON_WHITELIST)"), // matches the OPEN one → update
			List.of("Bostadsbidrag: -23%"), // matches the CLOSED one → must NOT re-open
			List.of(),
			null, null);

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
		service.reconcileCalculationWarnings(ERRAND_ID, List.of(), List.of(), List.of(), null, null);

		final var captor = ArgumentCaptor.forClass(FaWarningEntity.class);
		verify(repositoryMock, times(2)).save(captor.capture()); // open + acknowledged auto-close; closed untouched
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
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("Not Found: Warning not found on errand");
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
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST)
			.hasMessage("Bad Request: status must be OPEN, ACKNOWLEDGED or CLOSED");

		verify(repositoryMock, never()).findByIdAndErrandId(any(), any());
	}

	/**
	 * Every {@code TYPE_*} constant must carry a Swedish display name and be exposed on {@code Warning.type}, or the
	 * frontend gets a warning it cannot label or a value the contract does not admit. The återansökan rule types come
	 * straight from the DMN's {@code varningskod} column, which makes this the guard that keeps the three in step.
	 */
	@Test
	void everyWarningTypeConstantHasADisplayNameAndIsOnTheApiContract() throws Exception {
		final var allowableValues = List.of(Warning.class.getDeclaredField("type").getAnnotation(Schema.class).allowableValues());
		final var displayNames = displayNames();

		final var types = Arrays.stream(WarningService.class.getDeclaredFields())
			.filter(field -> Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
			.filter(field -> (field.getType() == String.class) && field.getName().startsWith("TYPE_"))
			.map(WarningServiceTest::readConstant)
			.toList();

		assertThat(types).hasSize(43);
		assertThat(types).allSatisfy(type -> {
			assertThat(displayNames).as("display name for %s", type).containsKey(type);
			assertThat(displayNames.get(type)).as("display name for %s", type).isNotBlank();
			assertThat(allowableValues).as("Warning.type allowableValues must contain %s", type).contains(type);
		});
	}

	@Test
	void reconcileCalculationWarningsFoldsAllSections() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		final var changes = new DraftChanges(
			List.of("Lön (APPLICANT)"), List.of("Pension (APPLICANT)"),
			List.of("RENT"), List.of(),
			List.of("Barn (CHILD)"), List.of());

		service.reconcileCalculationWarnings(ERRAND_ID,
			List.of("Bostadstillägg (NOT_ON_WHITELIST)"),
			List.of("Bostadsbidrag: -23%"),
			List.of("Dagersättning"),
			changes,
			List.of(
				new WarningService.WarningInput(WarningService.TYPE_HOUSEHOLD_CHANGE, "household-size", "Antal hushållsmedlemmar ändrat"),
				new WarningService.WarningInput(WarningService.TYPE_HOUSING_COST_CHANGE, "housing-cost", "Housing cost changed +32%"),
				new WarningService.WarningInput(WarningService.TYPE_EXPENSE_REVIEW, "OTHER", "OTHER: skälighet bedöms manuellt")));

		// 3 income/change/missing + NEW_INCOME + NEW_EXPENSE + NEW_PERSON + INCOME_DROPPED (draft) + 3 section warnings
		// (HOUSEHOLD_CHANGE + HOUSING_COST_CHANGE + EXPENSE_REVIEW) = 10, all new
		assertThat(inserted()).extracting(FaWarningEntity::getType)
			.containsExactlyInAnyOrder("UNHANDLED_INCOME", "INCOME_CHANGE", "MISSING_SSBTEK",
				"NEW_INCOME", "NEW_EXPENSE", "NEW_PERSON", "INCOME_DROPPED",
				"HOUSEHOLD_CHANGE", "HOUSING_COST_CHANGE", "EXPENSE_REVIEW");
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void reconcileCalculationWarningsToleratesNullDraftChanges() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		service.reconcileCalculationWarnings(ERRAND_ID, List.of("X (Y)"), List.of(), List.of(), null, null);

		assertThat(inserted()).extracting(FaWarningEntity::getType).containsExactly("UNHANDLED_INCOME"); // the single unhandled-income warning
	}

	@Test
	void listReturnsMappedWarnings() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(
			warning("MISSING_SSBTEK", "Dagersättning", "OPEN").withCreated(OffsetDateTime.parse("2026-06-02T00:00:00Z")),
			warning("UNHANDLED_INCOME", "Bostadstillägg", "ACKNOWLEDGED").withCreated(OffsetDateTime.parse("2026-06-01T00:00:00Z"))));

		final var result = service.list(ERRAND_ID);

		assertThat(result).extracting("type").containsExactly("UNHANDLED_INCOME", "MISSING_SSBTEK"); // sorted by created asc
	}

	private static String readConstant(final Field field) {
		try {
			return (String) field.get(null);
		} catch (final IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> displayNames() throws Exception {
		final var field = WarningService.class.getDeclaredField("TYPE_DISPLAY_NAME");
		field.setAccessible(true);
		return (Map<String, String>) field.get(null);
	}

	@Test
	void calculationReconcileLeavesProposalWarningsAlone() {
		final var proposalWarning = warning("EXPENSE_PARTIALLY_REJECTED", "RENT", "OPEN");
		final var calculationWarning = warning("MISSING_SSBTEK", "Dagersättning", "OPEN");
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(proposalWarning, calculationWarning));

		service.reconcileCalculationWarnings(ERRAND_ID, List.of(), List.of(), List.of(), null, null);

		final var captor = ArgumentCaptor.forClass(FaWarningEntity.class);
		verify(repositoryMock).save(captor.capture()); // only the calculation one auto-closes
		assertThat(captor.getValue().getType()).isEqualTo("MISSING_SSBTEK");
		assertThat(proposalWarning.getStatus()).isEqualTo("OPEN");
	}

	@Test
	void reconcileRuleWarningsLeavesTheDraftWarningsAsTheyWere() {
		// The normberäkning is saved in Lifecare: the draft warnings are neither refreshed nor auto-closed.
		final var draftWarnings = WarningService.DRAFT_REFRESH_TYPES.stream()
			.map(type -> warning(type, "draft-" + type, "OPEN"))
			.toList();
		final var staleRule = warning("PENDING_BENEFIT", "pending-benefit", "OPEN"); // no longer computed → auto-close
		final var staleSsbtek = warning("MISSING_SSBTEK", "Dagersättning", "ACKNOWLEDGED"); // no longer computed → auto-close
		final var proposalWarning = warning("EXPENSE_PARTIALLY_REJECTED", "RENT", "OPEN"); // another section → untouched
		final var existing = new ArrayList<>(draftWarnings);
		existing.addAll(List.of(staleRule, staleSsbtek, proposalWarning));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(existing);

		service.reconcileRuleWarnings(ERRAND_ID, List.of("Bostadstillägg (NOT_ON_WHITELIST)"), List.of(), List.of(),
			List.of(new WarningService.WarningInput("STAY_OUTSIDE_MUNICIPALITY", "stay", "Vistelse")));

		assertThat(inserted())
			.extracting(FaWarningEntity::getType, FaWarningEntity::getStatus)
			.containsExactlyInAnyOrder(
				tuple("UNHANDLED_INCOME", "OPEN"),
				tuple("STAY_OUTSIDE_MUNICIPALITY", "OPEN"));
		final var captor = ArgumentCaptor.forClass(FaWarningEntity.class);
		verify(repositoryMock, times(2)).save(captor.capture()); // the two auto-closes
		assertThat(captor.getAllValues())
			.extracting(FaWarningEntity::getType, FaWarningEntity::getStatus)
			.containsExactlyInAnyOrder(
				tuple("PENDING_BENEFIT", "CLOSED"),
				tuple("MISSING_SSBTEK", "CLOSED"));
		assertThat(draftWarnings).allSatisfy(draftWarning -> assertThat(draftWarning.getStatus()).isEqualTo("OPEN"));
		assertThat(proposalWarning.getStatus()).isEqualTo("OPEN");
	}

	@Test
	void reconcileRuleWarningsRefusesADraftWarning() {
		final var draftWarning = List.of(new WarningService.WarningInput(WarningService.TYPE_EXPENSE_REVIEW, "RENT", "Granska"));

		assertThatThrownBy(() -> service.reconcileRuleWarnings(ERRAND_ID, List.of(), List.of(), List.of(), draftWarning))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("warning type EXPENSE_REVIEW belongs to the draft refresh");
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void reconcileRuleWarningsToleratesNullRuleWarnings() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		service.reconcileRuleWarnings(ERRAND_ID, null, null, List.of("Dagersättning"), null);

		assertThat(inserted()).extracting(FaWarningEntity::getType).containsExactly("MISSING_SSBTEK");
	}

	@Test
	void reconcileByTypesTouchesOnlyTheOwnedTypes() {
		final var stale = warning("EXPENSE_PARTIALLY_REJECTED", "INTERNET", "ACKNOWLEDGED"); // no longer computed → auto-close
		final var open = warning("EXPENSE_PARTIALLY_REJECTED", "RENT", "OPEN"); // still computed → refresh
		final var closed = warning("PREVIOUS_DECISION_ADVANCE_ON_BENEFIT", "previous-decision", "CLOSED"); // computed again → never re-opened
		final var paymentWarning = warning("CO_APPLICANT_SPLIT_PAYMENT", "co-applicant", "OPEN"); // another section → untouched
		final var calculationWarning = warning("MISSING_SSBTEK", "Dagersättning", "OPEN"); // untouched
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(stale, open, closed, paymentWarning, calculationWarning));
		when(repositoryMock.save(any(FaWarningEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reconcileByTypes(ERRAND_ID, WarningService.DECISION_PROPOSAL_TYPES, List.of(
			new WarningService.WarningInput("EXPENSE_PARTIALLY_REJECTED", "RENT", "new text"),
			new WarningService.WarningInput("PREVIOUS_DECISION_ADVANCE_ON_BENEFIT", "previous-decision", "förskott")));

		verify(repositoryMock, times(2)).save(any(FaWarningEntity.class)); // refresh + auto-close
		assertThat(open.getMessage()).isEqualTo("new text");
		assertThat(stale.getStatus()).isEqualTo("CLOSED");
		assertThat(stale.isAutoResolved()).isTrue();
		assertThat(closed.getStatus()).isEqualTo("CLOSED");
		assertThat(paymentWarning.getStatus()).isEqualTo("OPEN");
		assertThat(calculationWarning.getStatus()).isEqualTo("OPEN");
		assertThat(result).extracting(Warning::getType, Warning::getSection).containsExactlyInAnyOrder(
			tuple("EXPENSE_PARTIALLY_REJECTED", "DECISION"), tuple("EXPENSE_PARTIALLY_REJECTED", "DECISION"), tuple("PREVIOUS_DECISION_ADVANCE_ON_BENEFIT", "DECISION"));
	}

	@Test
	void createSavesANewWarningOpen() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(warning("NEW_INCOME", "other", "OPEN")));
		when(repositoryMock.save(any(FaWarningEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var created = service.create(ERRAND_ID, "NEW_INCOME", "Bostadsbidrag", "Ny inkomst");

		assertThat(created).returns("NEW_INCOME", Warning::getType).returns("Bostadsbidrag", Warning::getSourceKey).returns("OPEN", Warning::getStatus);
		verify(repositoryMock).save(any(FaWarningEntity.class));
	}

	@Test
	void createReturnsTheExistingWarningForTheSameKey() {
		final var existing = warning("NEW_INCOME", "Bostadsbidrag", "ACKNOWLEDGED");
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(existing));

		final var created = service.create(ERRAND_ID, "NEW_INCOME", "Bostadsbidrag", "Ny inkomst");

		assertThat(created).returns(existing.getId(), Warning::getId).returns("ACKNOWLEDGED", Warning::getStatus);
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void reconcileByTypesCreatesNewWarningsOpen() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		service.reconcileByTypes(ERRAND_ID, Set.of("CO_APPLICANT_SPLIT_PAYMENT"), List.of(new WarningService.WarningInput("CO_APPLICANT_SPLIT_PAYMENT", "co-applicant", "text")));

		verify(repositoryMock).insertIgnore(any(), eq(ERRAND_ID), eq("CO_APPLICANT_SPLIT_PAYMENT"), eq("co-applicant"), eq("text"), eq("OPEN"), any());
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void reconcileByTypesRejectsAnInputOfAnotherType() {
		final var input = List.of(new WarningService.WarningInput("MISSING_SSBTEK", "x", "text"));
		final var owned = WarningService.PAYMENT_PROPOSAL_TYPES;

		assertThrows(IllegalArgumentException.class, () -> service.reconcileByTypes(ERRAND_ID, owned, input));
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void reconcileSsbtekReadFailureRaisesOneWarningPerErrand() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of());

		service.reconcileSsbtekReadFailure(ERRAND_ID, true);

		assertThat(inserted()).hasSize(1);
		final var raised = inserted().getFirst();
		assertThat(raised.getType()).isEqualTo("SSBTEK_READ_FAILED");
		assertThat(raised.getSourceKey()).isEqualTo("SSBTEK");
		assertThat(raised.getStatus()).isEqualTo("OPEN");
		// The text carries the time of the attempt, so it is matched on its two fixed halves rather than verbatim.
		assertThat(raised.getMessage())
			.startsWith(WarningService.MESSAGE_SSBTEK_READ_FAILED_PREFIX)
			.endsWith(WarningService.MESSAGE_SSBTEK_READ_FAILED_SUFFIX)
			.matches("^\\QFel att läsa SSBTEK \\E\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2},.*");
	}

	@Test
	void theReadFailureMessageCarriesTheAttemptTimeInSwedishWallClockTime() {
		// The services run on UTC containers; a handläggare reading the warning must see 14:53, not 12:53.
		final var failedAt = OffsetDateTime.parse("2026-09-21T12:53:38Z");

		assertThat(WarningService.ssbtekReadFailureMessage(failedAt))
			.isEqualTo("Fel att läsa SSBTEK 2026-09-21 14:53, nytt försök görs snart igen och ärendet kommer uppdateras med ny information");
	}

	@Test
	void theReadFailureMessageKeepsSwedishTimeAcrossTheWinterOffset() {
		// Same instant in January is +01:00, not +02:00 — the zone is honoured, not a fixed offset.
		final var failedAt = OffsetDateTime.parse("2026-01-15T12:53:38Z");

		assertThat(WarningService.ssbtekReadFailureMessage(failedAt)).contains("2026-01-15 13:53");
	}

	@Test
	void reconcileSsbtekReadFailureClosesItselfOnARunThatSucceeded() {
		// The whole point of the type: the handläggare never has to dismiss it, the next readable day removes it.
		final var readFailure = warning("SSBTEK_READ_FAILED", "SSBTEK", "OPEN");
		final var unrelated = warning("MISSING_SSBTEK", "Dagersättning", "OPEN");
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(readFailure, unrelated));
		when(repositoryMock.save(any(FaWarningEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.reconcileSsbtekReadFailure(ERRAND_ID, false);

		verify(repositoryMock).save(readFailure);
		assertThat(readFailure.getStatus()).isEqualTo("CLOSED");
		assertThat(readFailure.isAutoResolved()).isTrue();
		assertThat(unrelated.getStatus()).isEqualTo("OPEN");
	}

	@Test
	void calculationReconcileLeavesTheReadFailureWarningAlone() {
		// A failed day runs no calculation reconcile at all; a later successful one must not close a warning that is
		// still true, nor re-raise one that the read-failure reconcile has already closed.
		final var readFailure = warning("SSBTEK_READ_FAILED", "SSBTEK", "OPEN");
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(readFailure));

		service.reconcileCalculationWarnings(ERRAND_ID, List.of(), List.of(), List.of(), null, List.of());

		assertThat(readFailure.getStatus()).isEqualTo("OPEN");
		verify(repositoryMock, never()).save(readFailure);
	}

	@Test
	void sectionOfDerivesTheTabFromTheType() {
		assertThat(WarningService.sectionOf("EXPENSE_PARTIALLY_REJECTED")).isEqualTo("DECISION");
		assertThat(WarningService.sectionOf("CO_APPLICANT_SPLIT_PAYMENT")).isEqualTo("PAYMENT");
		assertThat(WarningService.sectionOf("MISSING_SSBTEK")).isEqualTo("CALCULATION");
	}

	@Test
	void sectionOfPutsALifecareReadFailureOnTheTabItsReadFeeds() {
		assertThat(WarningService.sectionOf("LIFECARE_READ_FAILED", "lifecare-read:recovery-claims")).isEqualTo("DECISION");
		assertThat(WarningService.sectionOf("LIFECARE_READ_FAILED", "lifecare-read:previous-decision")).isEqualTo("DECISION");
		assertThat(WarningService.sectionOf("LIFECARE_READ_FAILED", "lifecare-read:previous-family")).isEqualTo("CALCULATION");
		assertThat(WarningService.sectionOf("LIFECARE_READ_FAILED", null)).isEqualTo("CALCULATION");
		assertThat(WarningService.sectionOf("RECOVERY_CLAIM", "recovery-claim:41")).isEqualTo("DECISION");
	}

	@Test
	void reconcileLifecareReadFailureRaisesOneWarningPerRead() {
		final var otherRead = warning("LIFECARE_READ_FAILED", "lifecare-read:previous-decision", "OPEN");
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(otherRead));

		service.reconcileLifecareReadFailure(ERRAND_ID, "lifecare-read:recovery-claims", true);

		verify(repositoryMock, never()).save(any());
		assertThat(inserted()).singleElement()
			.returns("LIFECARE_READ_FAILED", FaWarningEntity::getType)
			.returns("lifecare-read:recovery-claims", FaWarningEntity::getSourceKey)
			.returns("OPEN", FaWarningEntity::getStatus)
			.returns("Återkrav i Lifecare kunde inte läsas – eventuella återkrav visas inte, kontrollera dem i Lifecare. Nytt försök görs vid nästa uppdatering",
				FaWarningEntity::getMessage);
		// Another read's failure is not this reconcile's to close.
		assertThat(otherRead.getStatus()).isEqualTo("OPEN");
	}

	@Test
	void reconcileLifecareReadFailureClosesItselfWhenTheReadSucceeds() {
		final var readFailure = warning("LIFECARE_READ_FAILED", "lifecare-read:previous-family", "OPEN").withCreated(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(readFailure));
		when(repositoryMock.save(any(FaWarningEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reconcileLifecareReadFailure(ERRAND_ID, "lifecare-read:previous-family", false);

		assertThat(readFailure.getStatus()).isEqualTo("CLOSED");
		assertThat(readFailure.isAutoResolved()).isTrue();
		assertThat(result).singleElement().satisfies(warning -> {
			assertThat(warning.getSection()).isEqualTo("CALCULATION");
			assertThat(warning.getTypeDisplayName()).isEqualTo("Lifecare kunde inte läsas");
		});
	}

	@Test
	void reconcileLifecareReadFailureRejectsAnUnknownRead() {
		assertThrows(IllegalArgumentException.class, () -> service.reconcileLifecareReadFailure(ERRAND_ID, "lifecare-read:unknown", true));
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void reconcileByTypesLeavesTheUnverifiedTypesAsTheyWereButReturnsThem() {
		final var claim = warning("RECOVERY_CLAIM", "recovery-claim:41", "OPEN"); // its read failed → must stay open
		final var stale = warning("EXPENSE_PARTIALLY_REJECTED", "INTERNET", "OPEN"); // verified and gone → auto-close
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(claim, stale));
		when(repositoryMock.save(any(FaWarningEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final var result = service.reconcileByTypes(ERRAND_ID, WarningService.DECISION_PROPOSAL_TYPES, Set.of("RECOVERY_CLAIM"), List.of());

		assertThat(claim.getStatus()).isEqualTo("OPEN");
		assertThat(stale.getStatus()).isEqualTo("CLOSED");
		verify(repositoryMock, never()).save(claim);
		assertThat(result).extracting(Warning::getType).containsExactlyInAnyOrder("RECOVERY_CLAIM", "EXPENSE_PARTIALLY_REJECTED");
	}

	@Test
	void reconcileByTypesRejectsAnInputOfAnUnverifiedType() {
		final var input = List.of(new WarningService.WarningInput("RECOVERY_CLAIM", "recovery-claim:1", "text"));
		final var owned = WarningService.DECISION_PROPOSAL_TYPES;
		final var unverified = Set.of("RECOVERY_CLAIM");

		assertThrows(IllegalArgumentException.class, () -> service.reconcileByTypes(ERRAND_ID, owned, unverified, input));
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void calculationReconcileLeavesTheUnverifiedTypesAndTheLifecareReadFailureAlone() {
		final var family = warning("FAMILY_DIFFERS_FROM_APPLICATION", "not-in-previous:p-2", "OPEN"); // its read failed → stays
		final var readFailure = warning("LIFECARE_READ_FAILED", "lifecare-read:previous-family", "OPEN"); // reconciled separately
		final var missing = warning("MISSING_SSBTEK", "Dagersättning", "OPEN"); // verified and gone → auto-close
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(family, readFailure, missing));

		service.reconcileCalculationWarnings(ERRAND_ID, List.of(), List.of(), List.of(), null, List.of(), WarningService.PREVIOUS_FAMILY_TYPES);

		assertThat(family.getStatus()).isEqualTo("OPEN");
		assertThat(readFailure.getStatus()).isEqualTo("OPEN");
		assertThat(missing.getStatus()).isEqualTo("CLOSED");
		verify(repositoryMock).save(missing);
		verify(repositoryMock, never()).save(family);
		verify(repositoryMock, never()).save(readFailure);
	}

	@Test
	void calculationReconcileRejectsAnInputOfAnUnverifiedType() {
		final var inputs = List.of(new WarningService.WarningInput("COMMON_HOUSEHOLD_COST_CHECK", "common-household-cost", "text"));
		final var unverified = WarningService.PREVIOUS_FAMILY_TYPES;

		assertThrows(IllegalArgumentException.class,
			() -> service.reconcileCalculationWarnings(ERRAND_ID, List.of(), List.of(), List.of(), null, inputs, unverified));
		verify(repositoryMock, never()).save(any());
	}

	@Test
	void closeResolvedDraftWarningsClosesOnlyWhatTheSavedCalculationSettles() {
		final var added = warning(WarningService.TYPE_NEW_INCOME, "Barnbidrag", WarningService.STATUS_OPEN);
		final var acknowledged = warning(WarningService.TYPE_NEW_INCOME, "Bostadsbidrag", WarningService.STATUS_ACKNOWLEDGED);
		final var stillMissing = warning(WarningService.TYPE_NEW_INCOME, "Underhållsstöd", WarningService.STATUS_OPEN);
		// Not a draft warning: the SSBTEK rules own it, whatever the calculation holds.
		final var ssbtek = warning(WarningService.TYPE_INCOME_CHANGE, "Barnbidrag", WarningService.STATUS_OPEN);
		final var closed = warning(WarningService.TYPE_INCOME_DROPPED, "Lön", WarningService.STATUS_CLOSED);
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(List.of(added, acknowledged, stillMissing, ssbtek, closed));
		final var calculation = new CalculationView(31, null, null, null, null, null, null, null, null, null, null, null, false, List.of(),
			List.of(new CalculationIncomeView("Barnbidrag", new BigDecimal("1250"), null, null, null), new CalculationIncomeView("Bostadsbidrag", null, null, BigDecimal.TEN, null)),
			List.of(), List.of());

		service.closeResolvedDraftWarnings(ERRAND_ID, calculation);

		final var captor = ArgumentCaptor.forClass(FaWarningEntity.class);
		verify(repositoryMock, times(2)).save(captor.capture());
		assertThat(captor.getAllValues()).extracting(FaWarningEntity::getSourceKey, FaWarningEntity::getStatus, FaWarningEntity::isAutoResolved)
			.containsExactly(tuple("Barnbidrag", "CLOSED", true), tuple("Bostadsbidrag", "CLOSED", true));
	}
}
