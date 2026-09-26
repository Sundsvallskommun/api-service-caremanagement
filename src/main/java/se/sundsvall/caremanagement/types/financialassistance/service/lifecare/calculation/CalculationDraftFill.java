package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.EXPENSES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.INCOMES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.PERSONS;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.SPECIAL_EXPENSES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.NODES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.array;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.elements;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.number;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.numberNode;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.objects;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.same;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.truthy;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.INCLUDED;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.PERSON_ID;

/**
 * Fills a Lifecare beräkning from careM's draft: the norm, the period, who in the household is included, and every
 * income, utgift and levnadskostnad i övrigt the draft holds. Lifecare places the members on the norm and counts the
 * rest itself.
 *
 * <p>
 * Refused with 422: a draft without a period, a household with an included co-applicant (how Lifecare takes the
 * co-applicant's amounts is not captured), no one included, and a row whose type Lifecare's catalogue does not have;
 * sending the beräkning without that row would quietly lack it.
 * </p>
 */
final class CalculationDraftFill {

	static final String ROLE_APPLICANT = "APPLICANT";
	static final String ROLE_CO_APPLICANT = "CO_APPLICANT";
	static final String BUCKET_SPECIAL_EXPENSE = "SPECIAL_EXPENSE";

	private static final String AMOUNT_APPLICANT = "amountApplicant";
	private static final String AMOUNT_CO_APPLICANT = "amountCoApplicant";
	private static final String APPLIED_AMOUNT = "appliedAmount";
	private static final String APPROVED_AMOUNT = "approvedAmount";
	private static final String NOTE = "note";
	private static final String APPLICANT_NOTE = "applicantNote";
	private static final String APPLICANT_SEARCH_DATE = "applicantSearchDate";
	private static final String CO_APPLICANT_SEARCH_DATE = "coApplicantSearchDate";
	private static final int LIFECARE_NOTE_MAX_LENGTH = 80;
	private static final String TEXT = "text";

	private CalculationDraftFill() {}

	/**
	 * A draft person with the personnummer resolved for it.
	 *
	 * @param row            the draft row
	 * @param personalNumber the personnummer, null when unknown
	 */
	record DraftPerson(NormPersonRow row, String personalNumber) {}

	/**
	 * The beräkning filled from the draft.
	 *
	 * @param  base       Lifecare's blank underlag, or a saved beräkning (not changed)
	 * @param  household  the members a saved beräkning leaves out (Lifecare keeps only the included)
	 * @param  draft      careM's draft
	 * @param  persons    the draft's persons with their personnummer
	 * @param  catalogues the norms, incomeTypes, expenseTypes and specialExpenseTypes
	 * @param  today      the calculation date, yyyy-MM-dd
	 * @return            the filled beräkning, a new tree
	 */
	static ObjectNode applyDraft(final ObjectNode base, final List<ObjectNode> household, final CalculationDraft draft, final List<DraftPerson> persons,
		final JsonNode catalogues, final String today) {

		if (draft.getCalculationFromDate() == null || draft.getCalculationToDate() == null) {
			throw refuse("Normberäkningen saknar period. Fyll i Från och Till.");
		}
		final var draftPersons = persons.stream().filter(person -> !person.row().isDeleted()).toList();
		if (draftPersons.stream().anyMatch(person -> ROLE_CO_APPLICANT.equals(person.row().getRole()) && person.row().isIncluded())) {
			throw refuse("Hushållet har en medsökande. Sådana normberäkningar kan inte sparas i Lifecare från Drakel ännu.");
		}

		final var calculation = base.deepCopy();
		final var members = householdOf(calculation, household);
		for (var index = 0; index < members.size(); index++) {
			final var member = members.get(index);
			draftPersonFor(member, index, draftPersons).ifPresent(person -> member.put(INCLUDED, person.row().isIncluded()));
		}
		if (members.stream().noneMatch(member -> truthy(member, INCLUDED))) {
			throw refuse("Ingen i hushållet ingår i normberäkningen.");
		}

		final var unknown = new ArrayList<String>();
		final var draftExpenses = Optional.ofNullable(draft.getExpenses()).orElse(List.of());
		final var expenseRows = draftExpenses.stream().filter(row -> !BUCKET_SPECIAL_EXPENSE.equals(row.getBucket())).toList();
		final var specialRows = Stream.concat(draftExpenses.stream().filter(row -> BUCKET_SPECIAL_EXPENSE.equals(row.getBucket())),
			Optional.ofNullable(draft.getSpecialExpenses()).orElse(List.of()).stream()).toList();

		if (draft.getNormId() != null && elements(catalogues.path("norms")).stream().anyMatch(norm -> same(norm.path("normId"), NODES.numberNode(draft.getNormId())))) {
			calculation.put("normId", draft.getNormId());
		}
		calculation.put("date", today);
		calculation.put("startDate", draft.getCalculationFromDate().toString());
		calculation.put("endDate", draft.getCalculationToDate().toString());
		calculation.set(PERSONS, array(members));
		calculation.set(INCOMES, array(fillIncomes(objects(base, INCOMES), draft.getIncomes(), catalogues.path("incomeTypes"), base.path("calculationId"), unknown)));
		calculation.set(EXPENSES, array(fillExpenses(objects(base, EXPENSES), expenseRows, catalogues.path("expenseTypes"), unknown)));
		calculation.set(SPECIAL_EXPENSES, array(fillExpenses(objects(base, SPECIAL_EXPENSES), specialRows, catalogues.path("specialExpenseTypes"), unknown)));
		calculation.put("hasCustomHouseholdSize", Boolean.TRUE.equals(draft.getHasCustomHouseholdSize()));

		if (!unknown.isEmpty()) {
			throw refuse("Lifecare känner inte till: " + String.join(", ", new LinkedHashSet<>(unknown))
				+ ". Ändra raden i normberäkningen eller för in den direkt i Lifecare.");
		}
		return calculation;
	}

	/**
	 * A personnummer or reserve number reduced to what two spellings of it share: 19880209T050 and 880209-T050 both give
	 * 880209T050.
	 */
	static String identityKey(final String identity) {
		final var cleaned = Optional.ofNullable(identity).orElse("").replaceAll("[^0-9A-Za-z]", "");
		return cleaned.substring(Math.max(0, cleaned.length() - 10)).toUpperCase(Locale.ROOT);
	}

	/** Every household member once: the beräkning's own, then those the household has that the beräkning lacks. */
	private static List<ObjectNode> householdOf(final ObjectNode calculation, final List<ObjectNode> household) {
		final var own = objects(calculation, PERSONS);
		final var members = new ArrayList<>(own);
		household.stream()
			.filter(member -> own.stream().noneMatch(present -> same(present.path(PERSON_ID), member.path(PERSON_ID))))
			.map(member -> {
				final var copy = member.deepCopy();
				copy.put(INCLUDED, false);
				return copy;
			})
			.forEach(members::add);
		return members;
	}

	/** The draft person a Lifecare household member is: by personnummer, else the applicant as Lifecare's first member. */
	private static Optional<DraftPerson> draftPersonFor(final JsonNode member, final int index, final List<DraftPerson> draftPersons) {
		final var byNumber = draftPersons.stream()
			.filter(person -> StringUtils.hasLength(person.personalNumber()))
			.filter(person -> identityKey(person.personalNumber()).equals(identityKey(text(member, PERSON_ID))))
			.findFirst();
		if (byNumber.isPresent() || index != 0) {
			return byNumber;
		}
		return draftPersons.stream().filter(person -> ROLE_APPLICANT.equals(person.row().getRole())).findFirst();
	}

	private static List<ObjectNode> fillIncomes(final List<ObjectNode> base, final List<NormIncomeRow> incomes, final JsonNode types,
		final JsonNode calculationId, final List<String> unknown) {
		// Every row starts from nothing, so a row the draft no longer has goes to 0, and Lifecare drops it.
		final var rows = new ArrayList<ObjectNode>();
		final var provenance = new IdentityHashMap<ObjectNode, List<NormIncomeRow>>();
		base.forEach(row -> {
			final var copy = row.deepCopy();
			copy.put(AMOUNT_APPLICANT, 0);
			copy.put(AMOUNT_CO_APPLICANT, 0);
			rows.add(copy);
		});
		for (final var income : Optional.ofNullable(incomes).orElse(List.of())) {
			final var applicant = amount(income.getApplicantEffectiveAmount());
			final var coApplicant = amount(income.getCoapplicantEffectiveAmount());
			if (income.isDeleted() || applicant == 0 && coApplicant == 0) {
				continue;
			}
			// By name first: careM's own type codes do not always equal Lifecare's incomeCode (Lön efter skatt).
			final var type = incomeType(types, income);
			if (type.isEmpty()) {
				unknown.add(Optional.ofNullable(income.getTypeName()).orElse(String.valueOf(income.getTypeId())));
				continue;
			}
			final var code = type.get().path("id");
			final var row = rows.stream().filter(candidate -> same(candidate.path("incomeCode"), code)).findFirst().orElseGet(() -> {
				final var created = newIncomeRow(calculationId, type.get(), 0, 0);
				rows.add(created);
				return created;
			});
			row.set(AMOUNT_APPLICANT, numberNode(number(row, AMOUNT_APPLICANT) + applicant));
			row.set(AMOUNT_CO_APPLICANT, numberNode(number(row, AMOUNT_CO_APPLICANT) + coApplicant));
			provenance.computeIfAbsent(row, _ -> new ArrayList<>()).add(income);
		}
		provenance.forEach(CalculationDraftFill::applyProvenance);
		return rows;
	}

	/**
	 * What the draft knew about where a Lifecare income row's amount came from — its note ("SSBTEK: …", "Ansökan: …",
	 * the handläggare's own) and the date each side's amount is attributed to — carried onto the row, so the beräkning in
	 * Lifecare shows the same as the draft did. Several draft rows of one type share a row: their notes are joined and
	 * the latest date wins.
	 */
	private static void applyProvenance(final ObjectNode row, final List<NormIncomeRow> incomes) {
		final var notes = incomes.stream().map(NormIncomeRow::getNote).filter(StringUtils::hasText).distinct().toList();
		if (notes.isEmpty()) {
			row.putNull(APPLICANT_NOTE);
		} else {
			row.put(APPLICANT_NOTE, toLifecareNote(String.join("; ", notes)));
		}
		row.put(APPLICANT_SEARCH_DATE, latestDay(incomes, NormIncomeRow::getApplicantEffectiveAmount, NormIncomeRow::getApplicantAmountDate));
		row.put(CO_APPLICANT_SEARCH_DATE, latestDay(incomes, NormIncomeRow::getCoapplicantEffectiveAmount, NormIncomeRow::getCoapplicantAmountDate));
	}

	/** The latest date among the rows with an amount on that side, as Lifecare's day ({@code yyyy-MM-dd}); "" when none. */
	private static String latestDay(final List<NormIncomeRow> incomes, final Function<NormIncomeRow, BigDecimal> amountOf,
		final Function<NormIncomeRow, OffsetDateTime> dateOf) {
		return incomes.stream()
			.filter(income -> amount(amountOf.apply(income)) != 0)
			.map(dateOf)
			.filter(Objects::nonNull)
			.map(OffsetDateTime::toLocalDate)
			.max(Comparator.naturalOrder())
			.map(LocalDate::toString)
			.orElse("");
	}

	private static Optional<ObjectNode> incomeType(final JsonNode types, final NormIncomeRow income) {
		final var byName = elements(types).stream()
			.filter(candidate -> income.getTypeName() != null && sameName(text(candidate, TEXT), income.getTypeName()))
			.findFirst();
		if (byName.isPresent() || income.getTypeId() == null) {
			return byName;
		}
		return elements(types).stream().filter(candidate -> same(candidate.path("id"), NODES.numberNode(income.getTypeId()))).findFirst();
	}

	private static ObjectNode newIncomeRow(final JsonNode calculationId, final ObjectNode type, final double applicant, final double coApplicant) {
		final var row = NODES.objectNode();
		row.set("calculationId", CalculationJson.orNull(calculationId).deepCopy());
		row.put("serialNumber", 0);
		row.set("incomeType", type.path(TEXT).deepCopy());
		row.set(AMOUNT_APPLICANT, numberNode(applicant));
		row.put(APPLICANT_SEARCH_DATE, "");
		row.putNull(APPLICANT_NOTE);
		row.set(AMOUNT_CO_APPLICANT, numberNode(coApplicant));
		row.put(CO_APPLICANT_SEARCH_DATE, "");
		row.put("grossAmountApplicant", 0);
		row.put("grossAmountCoApplicant", 0);
		row.set("incomeCode", type.path("id").deepCopy());
		row.put("changeable", true);
		row.put("isValid", true);
		return row;
	}

	private static List<ObjectNode> fillExpenses(final List<ObjectNode> base, final List<NormExpenseRow> draftRows, final JsonNode types, final List<String> unknown) {
		final var rows = new ArrayList<ObjectNode>();
		base.forEach(row -> {
			final var copy = row.deepCopy();
			copy.put(APPLIED_AMOUNT, 0);
			copy.put(APPROVED_AMOUNT, 0);
			copy.putNull(NOTE);
			rows.add(copy);
		});
		for (final var expense : draftRows) {
			final var approved = amount(expense.getEffectiveAmount());
			if (expense.isDeleted() || approved == 0) {
				continue;
			}
			// The draft keeps the cost type as its own code, with Lifecare's label beside it.
			final var label = Optional.ofNullable(expense.getCostTypeDisplayName()).or(() -> Optional.ofNullable(expense.getCostType())).orElse("");
			final var type = elements(types).stream().filter(candidate -> sameName(text(candidate, TEXT), label)).findFirst();
			if (type.isEmpty()) {
				unknown.add(label);
				continue;
			}
			final var applied = Optional.ofNullable(expense.getAppliedAmount()).map(BigDecimal::doubleValue).orElse(approved);
			final var note = Optional.ofNullable(expense.getSpecification()).or(() -> Optional.ofNullable(expense.getNote())).orElse(null);
			final var code = type.get().path("id");
			rows.stream().filter(row -> same(row.path("expenseCode"), code)).findFirst().ifPresentOrElse(existing -> {
				existing.set(APPLIED_AMOUNT, numberNode(number(existing, APPLIED_AMOUNT) + applied));
				existing.set(APPROVED_AMOUNT, numberNode(number(existing, APPROVED_AMOUNT) + approved));
				final var notes = Stream.of(text(existing, NOTE), note).filter(StringUtils::hasLength).toList();
				if (notes.isEmpty()) {
					existing.putNull(NOTE);
				} else {
					existing.put(NOTE, toLifecareNote(String.join("; ", notes)));
				}
			}, () -> rows.add(newExpenseRow(type.get(), applied, approved, note)));
		}
		return rows;
	}

	/** A note as Lifecare can take it: FamilyCare refuses the whole calculation when a row's note exceeds 80 characters. */
	private static String toLifecareNote(final String note) {
		if ((note == null) || (note.length() <= LIFECARE_NOTE_MAX_LENGTH)) {
			return note;
		}
		return note.substring(0, LIFECARE_NOTE_MAX_LENGTH);
	}

	private static ObjectNode newExpenseRow(final ObjectNode type, final double applied, final double approved, final String note) {
		final var row = NODES.objectNode();
		row.set("expenseCode", type.path("id").deepCopy());
		row.set("expenseType", type.path(TEXT).deepCopy());
		row.set(APPLIED_AMOUNT, numberNode(applied));
		row.set(APPROVED_AMOUNT, numberNode(approved));
		row.put(NOTE, toLifecareNote(note));
		row.put("changeable", true);
		row.put("markForCopy", false);
		row.put("showMarkForCopy", false);
		return row;
	}

	private static double amount(final BigDecimal value) {
		return Optional.ofNullable(value).map(BigDecimal::doubleValue).orElse(0.0);
	}

	private static boolean sameName(final String first, final String second) {
		return first != null && first.trim().toLowerCase(Locale.ROOT).equals(second.trim().toLowerCase(Locale.ROOT));
	}

	private static RuntimeException refuse(final String reason) {
		return Problem.valueOf(UNPROCESSABLE_CONTENT, reason);
	}
}
