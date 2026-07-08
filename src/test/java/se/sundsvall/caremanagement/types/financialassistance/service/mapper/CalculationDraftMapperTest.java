package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.BUCKET_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.BUCKET_SPECIAL_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_CASEWORKER;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ROLE_CHILD;

class CalculationDraftMapperTest {

	private static final String ERRAND_ID = "errand-1";

	@Test
	void effectiveValuePrefersTheCaseworkerValueThenFallsBackToProcess() {
		assertThat(CalculationDraftMapper.effectiveAmount(new BigDecimal("5"), new BigDecimal("9"))).isEqualByComparingTo("5");
		assertThat(CalculationDraftMapper.effectiveAmount(null, new BigDecimal("9"))).isEqualByComparingTo("9");
		assertThat(CalculationDraftMapper.effectiveDays(12, 30)).isEqualTo(12);
		assertThat(CalculationDraftMapper.effectiveDays(null, 30)).isEqualTo(30);
	}

	@Test
	void toIncomeRowComputesTheEffectiveAmounts() {
		final var entity = FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(20)
			.withApplicantProcessAmount(new BigDecimal("1000")).withApplicantCaseworkerAmount(new BigDecimal("1200"))
			.withCoapplicantProcessAmount(new BigDecimal("300"));

		final var row = CalculationDraftMapper.toIncomeRow(entity);

		assertThat(row.getApplicantEffectiveAmount()).isEqualByComparingTo("1200"); // caseworker wins
		assertThat(row.getCoapplicantEffectiveAmount()).isEqualByComparingTo("300"); // no caseworker value → process
	}

	@Test
	void toExpenseRowComputesTheEffectiveAmount() {
		final var entity = FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("HOUSING_COST")
			.withProcessAmount(new BigDecimal("8000")); // no caseworker override

		assertThat(CalculationDraftMapper.toExpenseRow(entity).getEffectiveAmount()).isEqualByComparingTo("8000");
	}

	@Test
	void toPersonRowComputesTheEffectiveDays() {
		final var entity = FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD).withProcessDays(15).withCaseworkerDays(20);

		assertThat(CalculationDraftMapper.toPersonRow(entity).getEffectiveDays()).isEqualTo(20); // caseworker wins
	}

	@Test
	void toCalculationDraftSplitsBucketsAndSumsTheLiveRows() {
		final var header = FaCalculationDraftEntity.create().withErrandId(ERRAND_ID).withApplicationMonth("2026-06").withNormId(7);
		final var incomes = List.of(
			FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(20).withPosition(1).withApplicantCaseworkerAmount(new BigDecimal("1200")),
			FaNormIncomeEntity.create().withOrigin(ORIGIN_SYSTEM).withTypeId(21).withPosition(0).withApplicantProcessAmount(new BigDecimal("500")).withDeleted(true));
		final var expenses = List.of(
			FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("HOUSING_COST").withBucket(BUCKET_EXPENSE).withProcessAmount(new BigDecimal("8000")),
			FaNormExpenseEntity.create().withOrigin(ORIGIN_SYSTEM).withCostType("MEDICINE").withBucket(BUCKET_SPECIAL_EXPENSE).withCaseworkerAmount(new BigDecimal("300")));
		final var persons = List.of(FaNormPersonEntity.create().withOrigin(ORIGIN_SYSTEM).withRole(ROLE_CHILD).withProcessDays(30));

		final var draft = CalculationDraftMapper.toCalculationDraft(header, incomes, expenses, persons);

		assertThat(draft.getNormId()).isEqualTo(7);
		assertThat(draft.getIncomes()).hasSize(2);
		assertThat(draft.getIncomes().getFirst().getTypeId()).isEqualTo(21); // sorted by position (0 before 1)
		assertThat(draft.getExpenses()).hasSize(1); // normal bucket only
		assertThat(draft.getSpecialExpenses()).hasSize(1); // the SPECIAL_EXPENSE bucket
		assertThat(draft.getIncomeSum()).isEqualByComparingTo("1200"); // the deleted 500 income is excluded
		assertThat(draft.getExpenseSum()).isEqualByComparingTo("8000");
		assertThat(draft.getSpecialExpenseSum()).isEqualByComparingTo("300");
	}

	@Test
	void toNewIncomeEntityStampsCaseworkerOriginAndPosition() {
		final var input = new NormIncomeInput().withTypeId(20).withTypeName("Lön").withApplicantCaseworkerAmount(new BigDecimal("3000")).withNote("manuell");

		final var entity = CalculationDraftMapper.toNewIncomeEntity(ERRAND_ID, 5, input);

		assertThat(entity.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(entity.getOrigin()).isEqualTo(ORIGIN_CASEWORKER);
		assertThat(entity.getPosition()).isEqualTo(5);
		assertThat(entity.getTypeId()).isEqualTo(20);
		assertThat(entity.getApplicantCaseworkerAmount()).isEqualByComparingTo("3000");
		assertThat(entity.getNote()).isEqualTo("manuell");
	}

	@Test
	void toNewExpenseEntityDefaultsTheBucketButHonoursSpecialExpense() {
		assertThat(CalculationDraftMapper.toNewExpenseEntity(ERRAND_ID, 0, new NormExpenseInput().withCostType("HOUSING_COST")).getBucket()).isEqualTo(BUCKET_EXPENSE);
		assertThat(CalculationDraftMapper.toNewExpenseEntity(ERRAND_ID, 0, new NormExpenseInput().withBucket(BUCKET_SPECIAL_EXPENSE)).getBucket()).isEqualTo(BUCKET_SPECIAL_EXPENSE);
	}

	@Test
	void toNewPersonEntityDefaultsIncludedToTrueWhenUnset() {
		assertThat(CalculationDraftMapper.toNewPersonEntity(ERRAND_ID, 0, new NormPersonInput().withPartyId("p1").withRole(ROLE_CHILD)).isIncluded()).isTrue();
		assertThat(CalculationDraftMapper.toNewPersonEntity(ERRAND_ID, 0, new NormPersonInput().withIncluded(false)).isIncluded()).isFalse();
	}
}
