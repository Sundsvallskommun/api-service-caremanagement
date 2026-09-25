package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.TypeOption;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareCalculationSummary;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareCalculationView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningNormRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningPreviousCalculation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningPreviousExpense;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningPreviousIncome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningPreviousPerson;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningRowInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningTypeOption;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.EXPENSES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.INCOMES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.PERSONS;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.SPECIAL_EXPENSES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationDraftFill.BUCKET_SPECIAL_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.INCLUDED;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.INCOME_CODE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.NORM_ROW_ID;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.members;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.typeOf;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.BUCKET_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.enteredApplicantAmount;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.expenseRowIds;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.keepsExpense;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.personRowId;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.dateOrNull;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.decimal;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.elements;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.idOf;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.integerOrNull;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.isTrue;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.objects;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.textOrEmpty;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.truthy;

/**
 * The normberäkning's API views: careM's draft and a beräkning saved in Lifecare in the one shape the Normberäkning tab
 * reads, Lifecare's summering signed the way the caseworker reads it, the previous beräkning, and the type catalogues.
 * The views mirror Drakel's BFF responses field for field.
 */
final class NormberakningMapper {

	static final String SOURCE_CAREM = "CAREM";
	static final String SOURCE_LIFECARE = "LIFECARE";

	/** Every row the tab edits in Lifecare is the caseworker's own. */
	private static final String CASEWORKER_ORIGIN = "CASEWORKER";
	private static final String ROLE_CHILD = "CHILD";
	private static final String ROLE_VISITATION_CHILD = "VISITATION_CHILD";
	private static final int ADULT_AGE = 18;
	private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
	private static final Pattern NAME_WITH_AMOUNT = Pattern.compile("\\d+[.,]\\d{2}$");

	private NormberakningMapper() {}

	// ---- careM's draft ---------------------------------------------------------------------------------------------

	/**
	 * careM's draft in the tab's shape, each person carrying its personnummer.
	 *
	 * @param  draft           the draft
	 * @param  personalNumbers personnummer by partyId, best-effort
	 * @return                 the view
	 */
	static NormberakningDraft toCaremDraftView(final CalculationDraft draft, final Map<String, String> personalNumbers) {
		return NormberakningDraft.create()
			.withErrandId(draft.getErrandId())
			.withApplicationMonth(draft.getApplicationMonth())
			.withNormId(draft.getNormId())
			.withNormType(draft.getNormType())
			.withNormTypeDisplayNames(draft.getNormTypeDisplayNames())
			.withCalculationFromDate(asString(draft.getCalculationFromDate()))
			.withCalculationToDate(asString(draft.getCalculationToDate()))
			.withCalculationDate(asString(draft.getCalculationDate()))
			.withHasCustomHouseholdSize(draft.getHasCustomHouseholdSize())
			.withHouseholdSize(draft.getHouseholdSize())
			.withPersons(list(draft.getPersons(), person -> toCaremPersonRow(person, personalNumbers)))
			.withIncomes(list(draft.getIncomes(), NormberakningMapper::toCaremIncomeRow))
			.withExpenses(list(draft.getExpenses(), NormberakningMapper::toCaremExpenseRow))
			.withSpecialExpenses(list(draft.getSpecialExpenses(), NormberakningMapper::toCaremExpenseRow))
			.withIncomeSum(draft.getIncomeSum())
			.withExpenseSum(draft.getExpenseSum())
			.withSpecialExpenseSum(draft.getSpecialExpenseSum())
			.withCreated(asString(draft.getCreated()))
			.withUpdated(asString(draft.getUpdated()))
			.withSource(SOURCE_CAREM);
	}

	private static NormberakningPersonRow toCaremPersonRow(final NormPersonRow row, final Map<String, String> personalNumbers) {
		return NormberakningPersonRow.create()
			.withId(row.getId())
			.withPosition(row.getPosition())
			.withOrigin(row.getOrigin())
			.withPartyId(row.getPartyId())
			.withPersonalNumber(Optional.ofNullable(row.getPartyId()).map(personalNumbers::get).orElse(null))
			.withRole(row.getRole())
			.withRoleDisplayName(row.getRoleDisplayName())
			.withName(row.getName())
			.withProcessDays(row.getProcessDays())
			.withCaseworkerDays(row.getCaseworkerDays())
			.withEffectiveDays(row.getEffectiveDays())
			.withIncluded(row.isIncluded())
			.withDeviationFromDate(asString(row.getDeviationFromDate()))
			.withDeviationToDate(asString(row.getDeviationToDate()))
			.withNormInterval(row.getNormInterval())
			.withAmount(row.getAmount())
			.withDeleted(row.isDeleted())
			.withNote(row.getNote());
	}

	private static NormberakningIncomeRow toCaremIncomeRow(final NormIncomeRow row) {
		return NormberakningIncomeRow.create()
			.withId(row.getId())
			.withPosition(row.getPosition())
			.withOrigin(row.getOrigin())
			.withTypeId(row.getTypeId())
			.withTypeName(row.getTypeName())
			.withApplicantProcessAmount(row.getApplicantProcessAmount())
			.withApplicantCaseworkerAmount(row.getApplicantCaseworkerAmount())
			.withApplicantEffectiveAmount(row.getApplicantEffectiveAmount())
			.withApplicantAmountDate(asString(row.getApplicantAmountDate()))
			.withCoapplicantProcessAmount(row.getCoapplicantProcessAmount())
			.withCoapplicantCaseworkerAmount(row.getCoapplicantCaseworkerAmount())
			.withCoapplicantEffectiveAmount(row.getCoapplicantEffectiveAmount())
			.withCoapplicantAmountDate(asString(row.getCoapplicantAmountDate()))
			.withDeleted(row.isDeleted())
			.withNote(row.getNote());
	}

	private static NormberakningExpenseRow toCaremExpenseRow(final NormExpenseRow row) {
		return NormberakningExpenseRow.create()
			.withId(row.getId())
			.withPosition(row.getPosition())
			.withOrigin(row.getOrigin())
			.withBucket(row.getBucket())
			.withCostType(row.getCostType())
			.withCostTypeDisplayName(row.getCostTypeDisplayName())
			.withOtherSubType(row.getOtherSubType())
			.withSpecification(row.getSpecification())
			.withAppliedAmount(row.getAppliedAmount())
			.withProcessAmount(row.getProcessAmount())
			.withCaseworkerAmount(row.getCaseworkerAmount())
			.withEffectiveAmount(row.getEffectiveAmount())
			.withDeleted(row.isDeleted())
			.withNote(row.getNote());
	}

	// ---- the beräkning in Lifecare ---------------------------------------------------------------------------------

	/**
	 * The saved beräkning in the tab's draft shape, marked as Lifecare's.
	 *
	 * @param  forEdit          GetCalculationForEdit's answer
	 * @param  applicationMonth the errand's application month, yyyy-MM
	 * @return                  the view
	 */
	static NormberakningDraft toLifecareDraftView(final JsonNode forEdit, final String applicationMonth) {
		final var calculation = forEdit.path("calculation");
		final var incomeTypes = forEdit.path("incomeTypes");
		final var jobStimulus = isTrue(calculation, "hasApplicantJobStimuli");
		final var periodStart = textOrEmpty(calculation, "startDate");
		final var persons = objects(calculation, PERSONS);
		final var incomes = objects(calculation, INCOMES).stream().filter(CalculationRowChanges::keepsIncome).toList();
		final var normTypeDisplayNames = new ArrayList<String>();
		Optional.ofNullable(text(calculation, "normText")).filter(name -> !name.isEmpty()).ifPresent(normTypeDisplayNames::add);

		return NormberakningDraft.create()
			.withApplicationMonth(applicationMonth)
			.withNormId(integerOrNull(calculation, "normId"))
			.withNormTypeDisplayNames(normTypeDisplayNames)
			.withCalculationFromDate(dateOrNull(calculation, "startDate"))
			.withCalculationToDate(dateOrNull(calculation, "endDate"))
			.withCalculationDate(dateOrNull(calculation, "date"))
			.withHasCustomHouseholdSize(truthy(calculation, "hasCustomHouseholdSize"))
			.withHouseholdSize(integerOrNull(calculation, "householdSize"))
			.withPersons(IntStream.range(0, persons.size()).mapToObj(index -> toLifecarePersonRow(persons.get(index), index, periodStart)).toList())
			.withApplicantJobStimulus(jobStimulus)
			.withIncomes(IntStream.range(0, incomes.size()).mapToObj(index -> toLifecareIncomeRow(incomes.get(index), index, incomeTypes, jobStimulus)).toList())
			.withExpenses(toLifecareExpenseRows(objects(calculation, EXPENSES), BUCKET_EXPENSE))
			.withSpecialExpenses(toLifecareExpenseRows(objects(calculation, SPECIAL_EXPENSES), BUCKET_SPECIAL_EXPENSE))
			.withNormRows(objects(calculation.path("norm"), "rows").stream()
				.map(row -> NormberakningNormRow.create().withId(integerOrNull(row, "rowId")).withName(normRowLabel(row)))
				.toList())
			.withAmountForHouseholdSize(decimal(calculation, "amountForHouseholdSize").orElse(null))
			.withCommonHouseholdCost(decimal(calculation, "commonHouseholdCost").orElse(null))
			.withFamilyMembers(members(calculation))
			.withIncomeSum(decimal(calculation, "sumInk").orElse(null))
			.withExpenseSum(decimal(calculation, "sumUtg").orElse(null))
			.withSpecialExpenseSum(decimal(calculation, "sumSpec").orElse(null))
			.withUpdated(dateOrNull(calculation, "updateTimestamp"))
			.withSource(SOURCE_LIFECARE)
			.withFinalized(truthy(calculation, "isFinalized"));
	}

	private static NormberakningPersonRow toLifecarePersonRow(final ObjectNode person, final int index, final String periodStart) {
		final var days = integerOrNull(person, "deviationDays");
		Integer normRowId = null;
		if (integer(person, NORM_ROW_ID) > 0) {
			normRowId = integerOrNull(person, NORM_ROW_ID);
		}
		return NormberakningPersonRow.create()
			.withId(personRowId(person, index))
			.withRole(roleOf(person, index, periodStart))
			.withPosition(index)
			.withOrigin(CASEWORKER_ORIGIN)
			.withPersonalNumber(text(person, "personIdFormatted"))
			.withName(text(person, "name"))
			.withIncluded(truthy(person, INCLUDED))
			.withAmount(decimal(person, "amount").orElse(null))
			.withDeviationFromDate(dateOrNull(person, "deviationFromDate"))
			.withDeviationToDate(dateOrNull(person, "deviationToDate"))
			.withEffectiveDays(days)
			.withCaseworkerDays(days)
			.withNormRowId(normRowId)
			.withNormInterval(text(person, "normRow"));
	}

	/**
	 * The member's role the way careM's draft names it. Lifecare has no roles on a beräkning: the first member is the
	 * applicant, a bonusbarn counts as an umgängesbarn, and anyone else under 18 at the start of the period as a barn.
	 */
	static String roleOf(final JsonNode person, final int index, final String periodStart) {
		if (index == 0) {
			return CalculationDraftFill.ROLE_APPLICANT;
		}
		if (isTrue(person, "isBonusChild")) {
			return ROLE_VISITATION_CHILD;
		}
		if (isMinorOn(text(person, "birthDate"), periodStart)) {
			return ROLE_CHILD;
		}
		return null;
	}

	private static boolean isMinorOn(final String birthDate, final String day) {
		if (birthDate == null || !ISO_DATE.matcher(birthDate).matches() || day.isEmpty()) {
			return false;
		}
		final var adultOn = (Integer.parseInt(birthDate.substring(0, 4)) + ADULT_AGE) + birthDate.substring(4);
		return adultOn.compareTo(day) > 0;
	}

	/** A norm row the way Lifecare's list names it: its name and monthly amount, e.g. Make/maka/sambo 3550.00. */
	static String normRowLabel(final JsonNode row) {
		final var name = textOrEmpty(row, "name");
		final var monthly = row.path("monthlyAmount");
		if (monthly.isNumber() && !NAME_WITH_AMOUNT.matcher(name).find()) {
			return name + " " + String.format(Locale.ROOT, "%.2f", monthly.doubleValue());
		}
		return name;
	}

	/**
	 * An income as the tab shows it. When the applicant has jobbstimulans in the period, an income it applies to is
	 * entered as a gross (Brutto S) and Lifecare counts the amount (Belopp S) from it.
	 */
	private static NormberakningIncomeRow toLifecareIncomeRow(final ObjectNode row, final int index, final JsonNode types, final boolean applicantHasJobStimulus) {
		final var applicant = decimalOf(enteredApplicantAmount(row, types));
		final var jobStimulusApplies = applicantHasJobStimulus && typeOf(types, row.path(INCOME_CODE)).filter(type -> isTrue(type, "isJobStimulus")).isPresent();
		final var view = NormberakningIncomeRow.create()
			.withId(idOf(row.path(INCOME_CODE)))
			.withPosition(index)
			.withOrigin(CASEWORKER_ORIGIN)
			.withTypeId(integerOrNull(row, INCOME_CODE))
			.withTypeName(text(row, "incomeType"))
			.withApplicantCaseworkerAmount(applicant)
			.withApplicantEffectiveAmount(applicant)
			.withApplicantAmountDate(dateOrNull(row, "applicantSearchDate"))
			.withCoapplicantCaseworkerAmount(decimal(row, "amountCoApplicant").orElse(null))
			.withCoapplicantEffectiveAmount(decimal(row, "amountCoApplicant").orElse(null))
			.withCoapplicantAmountDate(dateOrNull(row, "coApplicantSearchDate"))
			.withNote(dateOrNull(row, "applicantNote"));
		if (jobStimulusApplies) {
			view.withApplicantJobStimulus(true).withApplicantCountedAmount(decimal(row, "amountApplicant").orElse(null));
		}
		return view;
	}

	/** The rows Lifecare keeps, named the way changeExpense finds them again: counted over the whole list. */
	private static List<NormberakningExpenseRow> toLifecareExpenseRows(final List<ObjectNode> rows, final String bucket) {
		final var ids = expenseRowIds(rows, bucket);
		final var kept = IntStream.range(0, rows.size()).filter(index -> keepsExpense(rows.get(index))).boxed().toList();
		return IntStream.range(0, kept.size()).mapToObj(position -> {
			final var row = rows.get(kept.get(position));
			return NormberakningExpenseRow.create()
				.withId(ids.get(kept.get(position)))
				.withPosition(position)
				.withOrigin(CASEWORKER_ORIGIN)
				.withBucket(bucket)
				.withCostType(idOf(row.path("expenseCode")))
				.withCostTypeDisplayName(text(row, "expenseType"))
				.withAppliedAmount(decimal(row, "appliedAmount").orElse(null))
				.withCaseworkerAmount(decimal(row, "approvedAmount").orElse(null))
				.withEffectiveAmount(decimal(row, "approvedAmount").orElse(null))
				.withNote(dateOrNull(row, "note"));
		}).toList();
	}

	/**
	 * The saved beräkning, cleaned up for the tabs: the household and its personnummer are left behind, and Lifecare's
	 * summering is signed the way the caseworker reads it.
	 *
	 * @param  calculation a beräkning as Lifecare answers it
	 * @return             the view
	 */
	static LifecareCalculationView toLifecareCalculationView(final JsonNode calculation) {
		final var summary = calculation.path("calculationSummary");
		LifecareCalculationSummary summaryView = null;
		if (summary.isObject()) {
			summaryView = LifecareCalculationSummary.create()
				.withIncome(decimal(summary, "income").orElse(null))
				.withJobStimulus(decimal(summary, "jobStimulus").orElse(null))
				.withJobStimulusDeduction(decimal(summary, "jobStimulusDeduction").orElse(null))
				// Lifecare signs what reduces the result negative; the view carries it as an amount.
				.withNorm(decimal(summary, "norm").map(BigDecimal::negate).orElse(null))
				.withFamilyCost(decimal(summary, "familyCost").orElse(null))
				.withCommonHouseholdCost(decimal(summary, "commonHouseholdCost").orElse(null))
				.withExpenses(decimal(summary, "expences").map(BigDecimal::negate).orElse(null))
				.withSum(decimal(summary, "sum").orElse(null))
				.withSpecialExpenses(decimal(summary, "specialPurpose").orElse(null))
				.withResult(decimal(summary, "balance").orElse(null));
		}
		return LifecareCalculationView.create()
			.withId(integerOrNull(calculation, "calculationId"))
			.withNormName(text(calculation, "normText"))
			.withDate(text(calculation, "date"))
			.withStartDate(text(calculation, "startDate"))
			.withEndDate(text(calculation, "endDate"))
			.withFinalized(truthy(calculation, "isFinalized"))
			.withUpdated(text(calculation, "updateTimestamp"))
			.withSummary(summaryView);
	}

	/**
	 * GetCalculation's answer as the previous-beräkning view. The sums are Lifecare's own and positive; only the result
	 * keeps its sign. Members go without their personnummer.
	 *
	 * @param  calculation GetCalculation's answer
	 * @return             the view
	 */
	static NormberakningPreviousCalculation toPreviousCalculation(final JsonNode calculation) {
		final var summary = calculation.path("calculationSummary");
		return NormberakningPreviousCalculation.create()
			.withId(integerOrNull(calculation, "calculationId"))
			.withNorm(text(calculation, "normText"))
			.withFromDate(emptyAsNull(text(calculation, "startDate")))
			.withToDate(emptyAsNull(text(calculation, "endDate")))
			.withIncomeSum(decimal(calculation, "sumInk").orElse(null))
			.withExpenseSum(decimal(calculation, "sumUtg").orElse(null))
			.withSpecialExpenseSum(decimal(calculation, "sumSpec").orElse(null))
			.withNormSum(decimal(calculation, "sumNorm").orElse(null))
			.withCommonHouseholdCost(decimal(calculation, "commonHouseholdCost").orElse(null))
			.withFamilyCost(decimal(summary, "familyCost").orElse(null))
			.withBalance(decimal(summary, "balance").orElse(null))
			.withTotalSum(decimal(calculation, "totSum").orElse(null))
			.withIsFinal(truthy(calculation, "isFinalized"))
			.withPersons(objects(calculation, PERSONS).stream()
				.filter(person -> truthy(person, INCLUDED))
				.map(person -> NormberakningPreviousPerson.create()
					.withName(text(person, "name"))
					.withAmount(decimal(person, "amount").orElse(null))
					.withDeviationFromDate(emptyAsNull(text(person, "deviationFromDate")))
					.withDeviationToDate(emptyAsNull(text(person, "deviationToDate"))))
				.toList())
			.withIncomes(objects(calculation, INCOMES).stream()
				.map(income -> NormberakningPreviousIncome.create()
					.withType(text(income, "incomeType"))
					.withAmountApplicant(decimal(income, "amountApplicant").orElse(null))
					.withApplicantSearchDate(emptyAsNull(text(income, "applicantSearchDate")))
					.withAmountCoApplicant(decimal(income, "amountCoApplicant").orElse(null))
					.withCoApplicantSearchDate(emptyAsNull(text(income, "coApplicantSearchDate"))))
				.toList())
			.withExpenses(objects(calculation, EXPENSES).stream().map(NormberakningMapper::toPreviousExpense).toList())
			.withSpecialExpenses(objects(calculation, SPECIAL_EXPENSES).stream().map(NormberakningMapper::toPreviousExpense).toList());
	}

	private static NormberakningPreviousExpense toPreviousExpense(final JsonNode expense) {
		return NormberakningPreviousExpense.create()
			.withType(text(expense, "expenseType"))
			.withAppliedAmount(decimal(expense, "appliedAmount").orElse(null))
			.withApprovedAmount(decimal(expense, "approvedAmount").orElse(null));
	}

	// ---- catalogues ------------------------------------------------------------------------------------------------

	/** Lifecare's active types as options: the code is the Lifecare id. */
	static List<NormberakningTypeOption> toTypeOptions(final JsonNode types) {
		return elements(types).stream()
			.filter(type -> truthy(type, "isActive"))
			.map(type -> NormberakningTypeOption.create().withCode(idOf(type.path("id"))).withDisplayName(text(type, "text")))
			.toList();
	}

	/** Lifecare's norms as the Norm list offers them: the code is the normId. */
	static List<NormberakningTypeOption> toNormOptions(final JsonNode norms) {
		return elements(norms).stream()
			.map(norm -> NormberakningTypeOption.create().withCode(idOf(norm.path("normId"))).withDisplayName(text(norm, "name")))
			.toList();
	}

	/** careM's type as an option, preferring the Lifecare label over the citizen one. */
	static NormberakningTypeOption toTypeOption(final TypeOption type) {
		return NormberakningTypeOption.create()
			.withCode(type.getCode())
			.withDisplayName(Optional.ofNullable(type.getInternalDisplayName())
				.or(() -> Optional.ofNullable(type.getExternalDisplayName()))
				.orElse(type.getCode()));
	}

	// ---- row input to careM's draft --------------------------------------------------------------------------------

	/** The income fields of a row input, for careM's draft. */
	static NormIncomeInput toIncomeInput(final NormberakningRowInput input) {
		return NormIncomeInput.create()
			.withTypeId(input.getTypeId())
			.withTypeName(input.getTypeName())
			.withApplicantCaseworkerAmount(input.getApplicantCaseworkerAmount())
			.withApplicantAmountDate(toDateTime(input.getApplicantAmountDate()))
			.withCoapplicantCaseworkerAmount(input.getCoapplicantCaseworkerAmount())
			.withCoapplicantAmountDate(toDateTime(input.getCoapplicantAmountDate()))
			.withNote(input.getNote());
	}

	/** The expense fields of a row input, for careM's draft. */
	static NormExpenseInput toExpenseInput(final NormberakningRowInput input) {
		return NormExpenseInput.create()
			.withCostType(input.getCostType())
			.withBucket(input.getBucket())
			.withOtherSubType(input.getOtherSubType())
			.withSpecification(input.getSpecification())
			.withAppliedAmount(input.getAppliedAmount())
			.withCaseworkerAmount(input.getCaseworkerAmount())
			.withNote(input.getNote());
	}

	/** The person fields of a row input, for careM's draft. */
	static NormPersonInput toPersonInput(final NormberakningRowInput input) {
		return NormPersonInput.create()
			.withPartyId(input.getPartyId())
			.withRole(input.getRole())
			.withName(input.getName())
			.withCaseworkerDays(input.getCaseworkerDays())
			.withIncluded(input.getIncluded())
			.withDeviationFromDate(toDate(input.getDeviationFromDate()))
			.withDeviationToDate(toDate(input.getDeviationToDate()))
			.withNormInterval(input.getNormInterval())
			.withNote(input.getNote());
	}

	private static OffsetDateTime toDateTime(final String value) {
		try {
			return Optional.ofNullable(value).map(OffsetDateTime::parse).orElse(null);
		} catch (final DateTimeParseException _) {
			throw Problem.valueOf(BAD_REQUEST, "'%s' is not an ISO date-time with offset".formatted(value));
		}
	}

	private static LocalDate toDate(final String value) {
		try {
			return Optional.ofNullable(value).map(LocalDate::parse).orElse(null);
		} catch (final DateTimeParseException _) {
			throw Problem.valueOf(BAD_REQUEST, "'%s' is not an ISO date".formatted(value));
		}
	}

	// ---- helpers ---------------------------------------------------------------------------------------------------

	private static BigDecimal decimalOf(final JsonNode value) {
		if (value.isNumber()) {
			return value.decimalValue();
		}
		return null;
	}

	private static String emptyAsNull(final String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		return value;
	}

	private static String asString(final Object value) {
		if (value instanceof final OffsetDateTime dateTime) {
			return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
		}
		return Optional.ofNullable(value).map(Objects::toString).orElse(null);
	}

	private static <T, R> List<R> list(final List<T> source, final Function<T, R> mapper) {
		return Optional.ofNullable(source).orElse(List.of()).stream().map(mapper).toList();
	}
}
