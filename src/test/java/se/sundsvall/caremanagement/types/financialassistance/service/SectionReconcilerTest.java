package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_CASEWORKER;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_SYSTEM;

/**
 * The calculation merge invariant — the heart of the process-vs-caseworker ownership design. Exercised over income
 * rows but the logic is section-agnostic.
 */
class SectionReconcilerTest {

	private static final Function<FaNormIncomeEntity, String> KEY = entity -> String.valueOf(entity.getTypeId());
	private static final Predicate<FaNormIncomeEntity> IS_SYSTEM = entity -> ORIGIN_SYSTEM.equals(entity.getOrigin());
	private static final BiConsumer<FaNormIncomeEntity, FaNormIncomeEntity> COPY_PROCESS = (target, fresh) -> {
		target.setApplicantProcessAmount(fresh.getApplicantProcessAmount());
		target.setTypeName(fresh.getTypeName());
	};
	private static final Function<FaNormIncomeEntity, String> LABEL = FaNormIncomeEntity::getTypeName;

	private final List<FaNormIncomeEntity> persisted = new ArrayList<>();

	private SectionReconciler.Diff reconcile(final List<FaNormIncomeEntity> existing, final List<FaNormIncomeEntity> fresh) {
		return SectionReconciler.reconcile(existing, fresh, KEY, IS_SYSTEM, COPY_PROCESS, LABEL, persisted::add);
	}

	@Test
	void freshSystemRowWithNoMatchIsInsertedAndReportedAsAdded() {
		final var fresh = systemRow(20, "Bostadsbidrag", "1850");

		final var diff = reconcile(List.of(), List.of(fresh));

		assertThat(diff.added()).containsExactly("Bostadsbidrag");
		assertThat(diff.dropped()).isEmpty();
		assertThat(persisted).containsExactly(fresh);
	}

	@Test
	void matchedSystemRowRefreshesTheProcessColumnAndPreservesTheCaseworkerValue() {
		final var existing = systemRow(20, "Bostadsbidrag", "1850").withApplicantCaseworkerAmount(new BigDecimal("1900")).withNote("ok");
		final var fresh = systemRow(20, "Bostadsbidrag", "2000");

		final var diff = reconcile(new ArrayList<>(List.of(existing)), List.of(fresh));

		assertThat(diff.added()).isEmpty();
		assertThat(diff.dropped()).isEmpty();
		assertThat(existing.getApplicantProcessAmount()).isEqualByComparingTo("2000"); // process refreshed
		assertThat(existing.getApplicantCaseworkerAmount()).isEqualByComparingTo("1900"); // caseworker value untouched
		assertThat(existing.getNote()).isEqualTo("ok"); // note untouched
		assertThat(persisted).containsExactly(existing);
	}

	@Test
	void softDeletedSystemRowStaysDeletedAndIsNotResurrected() {
		final var existing = systemRow(20, "Bostadsbidrag", "1850").withDeleted(true);
		final var fresh = systemRow(20, "Bostadsbidrag", "2000");

		final var diff = reconcile(new ArrayList<>(List.of(existing)), List.of(fresh));

		assertThat(diff.added()).isEmpty(); // not re-added
		assertThat(existing.isDeleted()).isTrue(); // still deleted — never resurrected
		assertThat(existing.getApplicantProcessAmount()).isEqualByComparingTo("2000"); // process still refreshed in place
		assertThat(persisted).containsExactly(existing);
	}

	@Test
	void caseworkerAddedRowIsNeverMatchedOrRefreshedByTheProcess() {
		final var caseworkerRow = systemRow(20, "Bostadsbidrag", null).withOrigin(ORIGIN_CASEWORKER).withApplicantCaseworkerAmount(new BigDecimal("500"));
		final var fresh = systemRow(20, "Bostadsbidrag", "2000");

		final var diff = reconcile(new ArrayList<>(List.of(caseworkerRow)), List.of(fresh));

		assertThat(diff.added()).containsExactly("Bostadsbidrag"); // the fresh row is inserted as new
		assertThat(caseworkerRow.getApplicantProcessAmount()).isNull(); // caseworker row untouched
		assertThat(persisted).containsExactly(fresh); // only the new system row is persisted
	}

	@Test
	void systemRowThatDisappearsFromTheFreshSetIsKeptAndReportedAsDropped() {
		final var existing = systemRow(20, "Bostadsbidrag", "1850");

		final var diff = reconcile(new ArrayList<>(List.of(existing)), List.of());

		assertThat(diff.dropped()).containsExactly("Bostadsbidrag");
		assertThat(diff.added()).isEmpty();
		assertThat(persisted).isEmpty(); // not auto-deleted, not re-saved
	}

	private static FaNormIncomeEntity systemRow(final Integer typeId, final String typeName, final String processAmount) {
		return FaNormIncomeEntity.create()
			.withOrigin(ORIGIN_SYSTEM)
			.withTypeId(typeId)
			.withTypeName(typeName)
			.withApplicantProcessAmount(processAmount == null ? null : new BigDecimal(processAmount));
	}
}
