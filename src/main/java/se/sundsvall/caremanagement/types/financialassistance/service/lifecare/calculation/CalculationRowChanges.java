package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningRowInput;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.EXPENSES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.INCOMES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.PERSONS;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.SPECIAL_EXPENSES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationDraftFill.BUCKET_SPECIAL_EXPENSE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.NODES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.array;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.elements;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.find;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.idOf;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.integerOrNull;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.notZero;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.number;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.numberNode;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.objects;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.orNull;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.same;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.truthy;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.AMOUNT_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.DEVIATION_DAYS;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.GROSS_AMOUNT_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.HAS_CUSTOM_HOUSEHOLD_SIZE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.HOUSEHOLD_SIZE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.INCLUDED;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.INCOME_CODE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.NORM;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.NORM_ID;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.NORM_ROW;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.NORM_ROW_ID;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.PERSON_ID;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.ROW_ID;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.members;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.normRow;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.normRowName;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.typeOf;

/**
 * The caseworker's changes to a beräkning saved in Lifecare, made on the tree read for edit. Lifecare has no process
 * values, soft delete or row ids: a removed row goes to 0 (Lifecare drops it on save), and rows are named by their type
 * (an income by its code, an utgift by its bucket and code, a member by its personKey). Every method changes the
 * beräkning it is handed; callers hand it a copy.
 */
final class CalculationRowChanges {

	static final String BUCKET_EXPENSE = "EXPENSE";
	static final String EXPENSE_PREFIX = "E";
	static final String SPECIAL_EXPENSE_PREFIX = "S";

	private static final String APPLIED_AMOUNT = "appliedAmount";
	private static final String APPROVED_AMOUNT = "approvedAmount";
	private static final String EXPENSE_CODE = "expenseCode";
	private static final String NOTE = "note";
	private static final String TEXT = "text";
	private static final String ROW_GONE = "Raden finns inte längre i normberäkningen. Ladda om fliken.";

	private CalculationRowChanges() {}

	/**
	 * The shared cost a household size gives, and how the members' share of it is taken.
	 *
	 * @param normShared the norm's shared row for the size
	 * @param size       the household size
	 * @param members    the members included
	 */
	record SharedCost(JsonNode normShared, int size, int members) {

		/**
		 * The members' share: the amount times the members over the household size (2 030 x 3/4 = 1 523).
		 *
		 * @param  amount what Lifecare counts for a household of the size
		 * @return        the share
		 */
		long share(final double amount) {
			return Math.round(amount * members / size);
		}
	}

	// ---- incomes ---------------------------------------------------------------------------------------------------

	/** A row Lifecare keeps: one it would drop (every amount 0) is not shown. */
	static boolean keepsIncome(final JsonNode row) {
		return notZero(row, AMOUNT_APPLICANT) || notZero(row, "amountCoApplicant");
	}

	/**
	 * The applicant's amount as the caseworker entered it. On an income jobbstimulans applies to, Lifecare keeps the
	 * counted amount and the gross beside it; the caseworker works with the gross.
	 */
	static JsonNode enteredApplicantAmount(final JsonNode row, final JsonNode types) {
		final var gross = number(row, GROSS_AMOUNT_APPLICANT);
		final var jobStimulus = typeOf(types, row.path(INCOME_CODE)).filter(type -> truthy(type, "isJobStimulus")).isPresent();
		if (jobStimulus && gross > 0) {
			return row.path(GROSS_AMOUNT_APPLICANT);
		}
		return row.path(AMOUNT_APPLICANT);
	}

	/**
	 * Puts the gross back as the applicant's amount on every income jobbstimulans applies to, so a change starts from what
	 * the caseworker entered and jobbstimulans is not taken off twice.
	 */
	static ObjectNode withEnteredIncomes(final ObjectNode calculation, final JsonNode types) {
		objects(calculation, INCOMES).forEach(row -> CalculationJson.setOrRemove(row, AMOUNT_APPLICANT, enteredApplicantAmount(row, types).deepCopy()));
		return calculation;
	}

	/**
	 * Adds an income of a type from Lifecare's catalogue, picked by code or name. A type already on the beräkning is
	 * refused.
	 */
	static ObjectNode addIncome(final ObjectNode calculation, final JsonNode types, final NormberakningRowInput input) {
		final var type = elements(types).stream()
			.filter(candidate -> input.getTypeId() != null && same(candidate.path("id"), NODES.numberNode(input.getTypeId()))
				|| input.getTypeName() != null && input.getTypeName().equals(text(candidate, TEXT)))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(UNPROCESSABLE_CONTENT, "Lifecare har ingen inkomsttyp \"%s\".".formatted(
				Optional.ofNullable(input.getTypeName()).orElse(String.valueOf(input.getTypeId())))));
		final var code = type.path("id");
		final var incomes = objects(calculation, INCOMES);
		if (incomes.stream().anyMatch(row -> same(row.path(INCOME_CODE), code) && keepsIncome(row))) {
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, "%s finns redan i normberäkningen. Ändra den raden i stället.".formatted(text(type, TEXT)));
		}
		final var rows = new ArrayList<>(incomes.stream().filter(row -> !same(row.path(INCOME_CODE), code)).toList());
		final var row = NODES.objectNode();
		row.set("calculationId", orNull(calculation.path("calculationId")).deepCopy());
		row.put("serialNumber", 0);
		row.set("incomeType", type.path(TEXT).deepCopy());
		row.put(AMOUNT_APPLICANT, 0);
		row.put("applicantSearchDate", "");
		row.putNull("applicantNote");
		row.put("amountCoApplicant", 0);
		row.put("coApplicantSearchDate", "");
		row.put(GROSS_AMOUNT_APPLICANT, 0);
		row.put("grossAmountCoApplicant", 0);
		row.set(INCOME_CODE, code.deepCopy());
		row.put("changeable", true);
		row.put("isValid", true);
		applyIncomeFields(row, input);
		rows.add(row);
		calculation.set(INCOMES, array(rows));
		return calculation;
	}

	/** Changes the income row rowId (its income code). */
	static ObjectNode changeIncome(final ObjectNode calculation, final String rowId, final NormberakningRowInput input) {
		final var rows = objects(calculation, INCOMES).stream().filter(row -> idOf(row.path(INCOME_CODE)).equals(rowId)).toList();
		if (rows.isEmpty()) {
			throw Problem.valueOf(NOT_FOUND, ROW_GONE);
		}
		rows.forEach(row -> applyIncomeFields(row, input));
		return calculation;
	}

	/** Removes the income row rowId: its amounts go to 0 and Lifecare drops it on save. */
	static ObjectNode removeIncome(final ObjectNode calculation, final String rowId) {
		return changeIncome(calculation, rowId, NormberakningRowInput.create());
	}

	/**
	 * The income fields the caseworker sets. The tab always sends the whole row, so a field left out is empty. The
	 * applicant's amount goes as both counted and gross; jobbstimulans counts it down afterwards.
	 */
	private static void applyIncomeFields(final ObjectNode row, final NormberakningRowInput input) {
		row.set(AMOUNT_APPLICANT, amountNode(input.getApplicantCaseworkerAmount()));
		row.set(GROSS_AMOUNT_APPLICANT, amountNode(input.getApplicantCaseworkerAmount()));
		row.put("applicantSearchDate", lifecareDay(input.getApplicantAmountDate()));
		row.set("amountCoApplicant", amountNode(input.getCoapplicantCaseworkerAmount()));
		row.put("coApplicantSearchDate", lifecareDay(input.getCoapplicantAmountDate()));
		row.put("applicantNote", input.getNote());
	}

	// ---- expenses --------------------------------------------------------------------------------------------------

	/** A row Lifecare keeps: one with every amount 0 it drops. */
	static boolean keepsExpense(final JsonNode row) {
		return notZero(row, APPLIED_AMOUNT) || notZero(row, APPROVED_AMOUNT);
	}

	/** Each expense row's id: its bucket and code, with the occurrence when the code repeats (E-3, E-3-2). */
	static List<String> expenseRowIds(final List<ObjectNode> rows, final String bucket) {
		final var seen = new HashMap<String, Integer>();
		return rows.stream().map(row -> {
			final var code = idOf(row.path(EXPENSE_CODE));
			final var occurrence = seen.merge(code, 1, Integer::sum);
			final var id = prefixOf(bucket) + "-" + code;
			if (occurrence == 1) {
				return id;
			}
			return id + "-" + occurrence;
		}).toList();
	}

	/**
	 * Adds an utgift or a levnadskostnad i övrigt (by the input's bucket) of a type from Lifecare's catalogue, picked by
	 * code.
	 */
	static ObjectNode addExpense(final ObjectNode calculation, final JsonNode forEdit, final NormberakningRowInput input) {
		final var bucket = bucketOf(input.getBucket());
		final JsonNode types;
		if (BUCKET_EXPENSE.equals(bucket)) {
			types = forEdit.path("expenseTypes");
		} else {
			types = forEdit.path("specialExpenseTypes");
		}
		final var type = elements(types).stream()
			.filter(candidate -> idOf(candidate.path("id")).equals(input.getCostType()))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(UNPROCESSABLE_CONTENT, "Lifecare har ingen kostnadstyp \"%s\".".formatted(Optional.ofNullable(input.getCostType()).orElse(""))));
		final var approved = Optional.ofNullable(input.getCaseworkerAmount()).or(() -> Optional.ofNullable(input.getAppliedAmount())).orElse(BigDecimal.ZERO);
		final var list = listOf(bucket);
		final var rows = new ArrayList<>(objects(calculation, list));
		final var row = NODES.objectNode();
		row.set(EXPENSE_CODE, type.path("id").deepCopy());
		row.set("expenseType", type.path(TEXT).deepCopy());
		row.set(APPLIED_AMOUNT, numberNode(Optional.ofNullable(input.getAppliedAmount()).orElse(approved)));
		row.set(APPROVED_AMOUNT, numberNode(approved));
		row.put(NOTE, input.getNote());
		row.put("changeable", true);
		row.put("markForCopy", false);
		row.put("showMarkForCopy", false);
		rows.add(row);
		calculation.set(list, array(rows));
		return calculation;
	}

	/** Changes the expense row rowId (see expenseRowIds). */
	static ObjectNode changeExpense(final ObjectNode calculation, final String rowId, final NormberakningRowInput input) {
		final var bucket = bucketOfRow(rowId).orElseThrow(() -> Problem.valueOf(NOT_FOUND, ROW_GONE));
		final var rows = objects(calculation, listOf(bucket));
		final var index = expenseRowIds(rows, bucket).indexOf(rowId);
		if (index < 0) {
			throw Problem.valueOf(NOT_FOUND, ROW_GONE);
		}
		final var row = rows.get(index);
		if (input.getAppliedAmount() != null) {
			row.set(APPLIED_AMOUNT, numberNode(input.getAppliedAmount()));
		}
		row.set(APPROVED_AMOUNT, amountNode(input.getCaseworkerAmount()));
		row.put(NOTE, input.getNote());
		return calculation;
	}

	/** Removes the expense row rowId: its amounts go to 0 and Lifecare drops it on save. */
	static ObjectNode removeExpense(final ObjectNode calculation, final String rowId) {
		return changeExpense(calculation, rowId, NormberakningRowInput.create().withAppliedAmount(BigDecimal.ZERO).withCaseworkerAmount(BigDecimal.ZERO));
	}

	/** The bucket an input names: SPECIAL_EXPENSE, or EXPENSE for anything else. */
	static String bucketOf(final String bucket) {
		if (BUCKET_SPECIAL_EXPENSE.equals(bucket)) {
			return BUCKET_SPECIAL_EXPENSE;
		}
		return BUCKET_EXPENSE;
	}

	private static Optional<String> bucketOfRow(final String rowId) {
		if (rowId.startsWith(EXPENSE_PREFIX + "-")) {
			return Optional.of(BUCKET_EXPENSE);
		}
		if (rowId.startsWith(SPECIAL_EXPENSE_PREFIX + "-")) {
			return Optional.of(BUCKET_SPECIAL_EXPENSE);
		}
		return Optional.empty();
	}

	private static String prefixOf(final String bucket) {
		if (BUCKET_EXPENSE.equals(bucket)) {
			return EXPENSE_PREFIX;
		}
		return SPECIAL_EXPENSE_PREFIX;
	}

	private static String listOf(final String bucket) {
		if (BUCKET_EXPENSE.equals(bucket)) {
			return EXPENSES;
		}
		return SPECIAL_EXPENSES;
	}

	// ---- members ---------------------------------------------------------------------------------------------------

	/** A member's id on the beräkning: Lifecare's personKey, or its place for one not saved yet. */
	static String personRowId(final JsonNode person, final int index) {
		if (integer(person, "personKey") > 0) {
			return idOf(person.path("personKey"));
		}
		return "new-" + (index + 1);
	}

	/**
	 * Sets the member rowId's days in the household and normintervall. No days means the whole period. The amount is
	 * Lifecare's to count from these; Ingår från/till stay as Lifecare has them.
	 */
	static ObjectNode changePerson(final ObjectNode calculation, final String rowId, final NormberakningRowInput input) {
		final var persons = objects(calculation, PERSONS);
		ObjectNode member = null;
		for (var index = 0; index < persons.size() && member == null; index++) {
			if (personRowId(persons.get(index), index).equals(rowId)) {
				member = persons.get(index);
			}
		}
		if (member == null) {
			throw Problem.valueOf(NOT_FOUND, ROW_GONE);
		}
		Optional<ObjectNode> row = Optional.empty();
		if (input.getNormRowId() != null) {
			row = normRow(calculation, NODES.numberNode(input.getNormRowId()));
			if (row.isEmpty()) {
				throw Problem.valueOf(UNPROCESSABLE_CONTENT, "Normintervallet finns inte i normen. Ladda om fliken.");
			}
		}
		if (input.getCaseworkerDays() == null) {
			member.putNull(DEVIATION_DAYS);
		} else {
			member.put(DEVIATION_DAYS, input.getCaseworkerDays());
		}
		final var target = member;
		row.ifPresent(found -> {
			target.set(NORM_ROW_ID, found.path(ROW_ID).deepCopy());
			target.put(NORM_ROW, normRowName(Optional.ofNullable(text(found, "name")).orElse("")));
		});
		return calculation;
	}

	/**
	 * Whether a member already on the beräkning has other days or another normintervall than before: its amount then has
	 * to be counted again. A member just taken in has the amount Lifecare placed it with.
	 */
	static boolean needsRecount(final JsonNode before, final JsonNode member) {
		final var previous = find(before, PERSONS, candidate -> same(candidate.path(PERSON_ID), member.path(PERSON_ID)));
		return previous.isPresent()
			&& truthy(member, INCLUDED)
			&& integer(member, NORM_ROW_ID) > 0
			&& (!same(previous.get().path(NORM_ROW_ID), member.path(NORM_ROW_ID))
				|| !same(orNull(previous.get().path(DEVIATION_DAYS)), orNull(member.path(DEVIATION_DAYS))));
	}

	// ---- header ----------------------------------------------------------------------------------------------------

	/**
	 * Changes the beräkning's header: its norm, and its own household size (Annan hushållsstorlek) or taking that off so
	 * the members count. The period is Lifecare's. An own household size is saved for the household's coming beräkningar
	 * too, the web app's question about that answered Ja.
	 */
	static ObjectNode changeHeader(final ObjectNode calculation, final JsonNode forEdit, final NormHeaderInput input) {
		if (input.getNormType() != null || input.getCalculationFromDate() != null || input.getCalculationToDate() != null) {
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, "Perioden ändras i Lifecare. Från Drakel går normen och hushållsstorleken att ändra.");
		}
		if (input.getNormId() != null && !CalculationJson.hasNumber(calculation, NORM_ID, input.getNormId())) {
			final var norm = elements(forEdit.path("norms")).stream()
				.filter(candidate -> CalculationJson.hasNumber(candidate, NORM_ID, input.getNormId()))
				.findFirst()
				.orElseThrow(() -> Problem.valueOf(UNPROCESSABLE_CONTENT, "Normen finns inte i Lifecare. Ladda om fliken."));
			calculation.set(NORM_ID, norm.path(NORM_ID).deepCopy());
			calculation.set("normText", orNull(norm.path("name")).deepCopy());
		}
		if (input.getHasCustomHouseholdSize() == null && input.getHouseholdSize() == null) {
			return calculation;
		}
		final var custom = Optional.ofNullable(input.getHasCustomHouseholdSize()).orElseGet(() -> truthy(calculation, HAS_CUSTOM_HOUSEHOLD_SIZE));
		if (custom && (input.getHouseholdSize() == null || input.getHouseholdSize() < 1)) {
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, "Ange hushållsstorleken.");
		}
		calculation.put(HAS_CUSTOM_HOUSEHOLD_SIZE, custom);
		if (custom) {
			calculation.put(HOUSEHOLD_SIZE, input.getHouseholdSize());
		} else {
			calculation.put(HOUSEHOLD_SIZE, members(calculation));
		}
		calculation.put("saveHouseholdSize", custom);
		return calculation;
	}

	// ---- gemensamma kostnader --------------------------------------------------------------------------------------

	/** Whether the norm, the household size or the members counted changed: the gemensamma kostnader then change too. */
	static boolean householdSizeChanged(final JsonNode before, final JsonNode after) {
		return !same(before.path(NORM_ID), after.path(NORM_ID))
			|| sharedSize(before) != sharedSize(after)
			|| members(before) != members(after);
	}

	/**
	 * The gemensamma kostnader for the household: the norm's shared row for its size, and the members' share of what
	 * Lifecare counts for it. Empty when the norm has no row for that size.
	 */
	static Optional<SharedCost> sharedCostShare(final JsonNode calculation) {
		final var size = sharedSize(calculation);
		final var members = members(calculation);
		if (size == 0) {
			return Optional.empty();
		}
		return find(calculation.path(NORM), "shared", row -> CalculationJson.hasNumber(row, "noOfMembers", size))
			.map(normShared -> new SharedCost(normShared, size, members));
	}

	/** The household size the gemensamma kostnader are counted on: the own size when there is one, else the members. */
	private static int sharedSize(final JsonNode calculation) {
		final ToIntFunction<JsonNode> own = node -> Optional.ofNullable(integerOrNull(node, HOUSEHOLD_SIZE)).orElse(0);
		if (truthy(calculation, HAS_CUSTOM_HOUSEHOLD_SIZE) && own.applyAsInt(calculation) > 0) {
			return own.applyAsInt(calculation);
		}
		return members(calculation);
	}

	// ---- helpers ---------------------------------------------------------------------------------------------------

	private static JsonNode amountNode(final BigDecimal amount) {
		return numberNode(Optional.ofNullable(amount).orElse(BigDecimal.ZERO));
	}

	/** The day of a date or date-time, as Lifecare stores it; empty when there is none. */
	static String lifecareDay(final String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		return value.substring(0, Math.min(10, value.length()));
	}
}
