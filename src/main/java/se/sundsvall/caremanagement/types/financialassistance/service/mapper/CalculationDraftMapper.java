package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCalculationDraftEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormExpenseEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormIncomeEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.BUCKET_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.BUCKET_SPECIAL_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_CASEWORKER;

/**
 * Maps the draft calculation between its persisted entities ({@link FaCalculationDraftEntity} header +
 * {@code FaNorm*Entity} section rows) and the API models — the per-row read views ({@code Norm*Row}), the caseworker
 * edit payloads ({@code Norm*Input}) and the full {@link CalculationDraft} view. The effective value shown to the
 * caseworker is the caseworker value when set, otherwise the process value ({@link #effectiveAmount} /
 * {@link #effectiveDays}). Null-safe throughout.
 */
public final class CalculationDraftMapper {

	private CalculationDraftMapper() {}

	// ------------------------------------------------------------------------------------------------------------------
	// The full draft view — header + the three sections, expenses split into normal vs the SPECIAL_EXPENSE bucket, with
	// the section sums over the live (non-deleted) rows.
	// ------------------------------------------------------------------------------------------------------------------

	public static CalculationDraft toCalculationDraft(final FaCalculationDraftEntity header,
		final List<FaNormIncomeEntity> incomeEntities, final List<FaNormExpenseEntity> expenseEntities, final List<FaNormPersonEntity> personEntities) {

		final var incomes = incomeEntities.stream()
			.sorted(comparing(FaNormIncomeEntity::getPosition, nullsLast(naturalOrder()))).map(CalculationDraftMapper::toIncomeRow).toList();
		final var allExpenses = expenseEntities.stream()
			.sorted(comparing(FaNormExpenseEntity::getPosition, nullsLast(naturalOrder()))).map(CalculationDraftMapper::toExpenseRow).toList();
		final var expenses = allExpenses.stream().filter(row -> !BUCKET_SPECIAL_EXPENSE.equals(row.getBucket())).toList();
		final var specialExpenses = allExpenses.stream().filter(row -> BUCKET_SPECIAL_EXPENSE.equals(row.getBucket())).toList();
		final var persons = personEntities.stream()
			.sorted(comparing(FaNormPersonEntity::getPosition, nullsLast(naturalOrder()))).map(CalculationDraftMapper::toPersonRow).toList();

		return CalculationDraft.create()
			.withErrandId(header.getErrandId())
			.withApplicationMonth(header.getApplicationMonth())
			.withNormId(header.getNormId())
			.withNormType(header.getNormType())
			.withCalculationFromDate(header.getCalculationFromDate())
			.withCalculationToDate(header.getCalculationToDate())
			.withCalculationDate(header.getCalculationDate())
			.withHasCustomHouseholdSize(header.getHasCustomHouseholdSize())
			.withHouseholdSize(header.getHouseholdSize())
			.withPersons(persons)
			.withIncomes(incomes)
			.withExpenses(expenses)
			.withSpecialExpenses(specialExpenses)
			.withIncomeSum(sum(incomes.stream().filter(row -> !row.isDeleted()).flatMap(CalculationDraftMapper::incomeEffectiveAmounts)))
			.withExpenseSum(sum(expenses.stream().filter(row -> !row.isDeleted()).map(NormExpenseRow::getEffectiveAmount)))
			.withSpecialExpenseSum(sum(specialExpenses.stream().filter(row -> !row.isDeleted()).map(NormExpenseRow::getEffectiveAmount)))
			.withCreated(header.getCreated())
			.withUpdated(header.getUpdated());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Entity -> view row
	// ------------------------------------------------------------------------------------------------------------------

	public static NormIncomeRow toIncomeRow(final FaNormIncomeEntity e) {
		return NormIncomeRow.create()
			.withId(e.getId()).withOrigin(e.getOrigin()).withPosition(e.getPosition()).withTypeId(e.getTypeId()).withTypeName(e.getTypeName())
			.withApplicantProcessAmount(e.getApplicantProcessAmount()).withApplicantCaseworkerAmount(e.getApplicantCaseworkerAmount())
			.withApplicantEffectiveAmount(effectiveAmount(e.getApplicantCaseworkerAmount(), e.getApplicantProcessAmount())).withApplicantAmountDate(e.getApplicantAmountDate())
			.withCoapplicantProcessAmount(e.getCoapplicantProcessAmount()).withCoapplicantCaseworkerAmount(e.getCoapplicantCaseworkerAmount())
			.withCoapplicantEffectiveAmount(effectiveAmount(e.getCoapplicantCaseworkerAmount(), e.getCoapplicantProcessAmount())).withCoapplicantAmountDate(e.getCoapplicantAmountDate())
			.withDeleted(e.isDeleted()).withNote(e.getNote()).withCreated(e.getCreated()).withUpdated(e.getUpdated());
	}

	public static NormExpenseRow toExpenseRow(final FaNormExpenseEntity e) {
		final var effective = effectiveAmount(e.getCaseworkerAmount(), e.getProcessAmount());
		return NormExpenseRow.create()
			.withId(e.getId()).withOrigin(e.getOrigin()).withPosition(e.getPosition()).withBucket(e.getBucket()).withCostType(e.getCostType()).withOtherSubType(e.getOtherSubType())
			.withSpecification(e.getSpecification())
			.withAppliedAmount(e.getAppliedAmount()).withProcessAmount(e.getProcessAmount()).withCaseworkerAmount(e.getCaseworkerAmount())
			.withEffectiveAmount(effective).withDeleted(e.isDeleted()).withNote(e.getNote())
			.withCreated(e.getCreated()).withUpdated(e.getUpdated());
	}

	public static NormPersonRow toPersonRow(final FaNormPersonEntity e) {
		final var effective = effectiveDays(e.getCaseworkerDays(), e.getProcessDays());
		return NormPersonRow.create()
			.withId(e.getId()).withOrigin(e.getOrigin()).withPosition(e.getPosition()).withPartyId(e.getPartyId()).withRole(e.getRole()).withName(e.getName())
			.withProcessDays(e.getProcessDays()).withCaseworkerDays(e.getCaseworkerDays()).withEffectiveDays(effective)
			.withIncluded(e.isIncluded()).withDeviationFromDate(e.getDeviationFromDate()).withDeviationToDate(e.getDeviationToDate())
			.withNormInterval(e.getNormInterval()).withJobStimulusAmount(e.getJobStimulusAmount())
			.withDeleted(e.isDeleted()).withNote(e.getNote()).withCreated(e.getCreated()).withUpdated(e.getUpdated());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Caseworker add — input -> a new caseworker row at the given (next free) position.
	// ------------------------------------------------------------------------------------------------------------------

	public static FaNormIncomeEntity toNewIncomeEntity(final String errandId, final int position, final NormIncomeInput input) {
		return FaNormIncomeEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_CASEWORKER).withPosition(position)
			.withTypeId(input.getTypeId()).withTypeName(input.getTypeName())
			.withApplicantCaseworkerAmount(input.getApplicantCaseworkerAmount()).withApplicantAmountDate(input.getApplicantAmountDate())
			.withCoapplicantCaseworkerAmount(input.getCoapplicantCaseworkerAmount()).withCoapplicantAmountDate(input.getCoapplicantAmountDate())
			.withNote(input.getNote());
	}

	public static FaNormExpenseEntity toNewExpenseEntity(final String errandId, final int position, final NormExpenseInput input) {
		return FaNormExpenseEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_CASEWORKER).withPosition(position).withBucket(bucketOrDefault(input.getBucket()))
			.withCostType(input.getCostType()).withOtherSubType(input.getOtherSubType()).withSpecification(input.getSpecification())
			.withAppliedAmount(input.getAppliedAmount()).withCaseworkerAmount(input.getCaseworkerAmount()).withNote(input.getNote());
	}

	public static FaNormPersonEntity toNewPersonEntity(final String errandId, final int position, final NormPersonInput input) {
		return FaNormPersonEntity.create()
			.withErrandId(errandId).withOrigin(ORIGIN_CASEWORKER).withPosition(position)
			.withPartyId(input.getPartyId()).withRole(input.getRole()).withName(input.getName())
			.withCaseworkerDays(input.getCaseworkerDays()).withIncluded(input.getIncluded() == null || input.getIncluded())
			.withDeviationFromDate(input.getDeviationFromDate()).withDeviationToDate(input.getDeviationToDate())
			.withNormInterval(input.getNormInterval()).withJobStimulusAmount(input.getJobStimulusAmount()).withNote(input.getNote());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Effective value = caseworker value when set, otherwise the process value.
	// ------------------------------------------------------------------------------------------------------------------

	public static BigDecimal effectiveAmount(final BigDecimal caseworkerAmount, final BigDecimal processAmount) {
		return ofNullable(caseworkerAmount).orElse(processAmount);
	}

	public static Integer effectiveDays(final Integer caseworkerDays, final Integer processDays) {
		return ofNullable(caseworkerDays).orElse(processDays);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Internals
	// ------------------------------------------------------------------------------------------------------------------

	private static String bucketOrDefault(final String bucket) {
		if (BUCKET_SPECIAL_EXPENSE.equals(bucket)) {
			return BUCKET_SPECIAL_EXPENSE;
		}
		return BUCKET_EXPENSE;
	}

	private static Stream<BigDecimal> incomeEffectiveAmounts(final NormIncomeRow row) {
		return Stream.of(row.getApplicantEffectiveAmount(), row.getCoapplicantEffectiveAmount());
	}

	private static BigDecimal sum(final Stream<BigDecimal> amounts) {
		return amounts.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
