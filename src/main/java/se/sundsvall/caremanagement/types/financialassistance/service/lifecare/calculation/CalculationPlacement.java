package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.util.List;
import java.util.Optional;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.HouseholdSize;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.INCOMES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.PERSONS;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.find;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.integerOrNull;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.isTrue;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.number;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.numberNode;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.objects;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.same;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.setOrRemove;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.truthy;

/**
 * How the members sit on the norm and what that makes of the amounts: placement on normintervall, jobbstimulans on the
 * incomes, the household size and the gemensamma kostnader. Every method changes the beräkning it is handed; callers
 * hand it a copy of what Lifecare sent.
 */
final class CalculationPlacement {

	static final String NORM_ROW_ID = "normRowId";
	static final String NORM_ROW = "normRow";
	static final String AMOUNT = "amount";
	static final String INCLUDED = "included";
	static final String PERSON_ID = "personId";
	static final String DEVIATION_DAYS = "deviationDays";
	static final String AMOUNT_APPLICANT = "amountApplicant";
	static final String GROSS_AMOUNT_APPLICANT = "grossAmountApplicant";
	static final String INCOME_CODE = "incomeCode";
	static final String HOUSEHOLD_SIZE = "householdSize";
	static final String HAS_CUSTOM_HOUSEHOLD_SIZE = "hasCustomHouseholdSize";
	static final String NORM_ID = "normId";
	static final String NORM = "norm";
	static final String ROWS = "rows";
	static final String ROW_ID = "rowId";

	private static final String AMOUNT_SUFFIX = "\\s+\\d+(?:[.,]\\d+)?$";

	private CalculationPlacement() {}

	/**
	 * A normintervall's name as a member carries it: the row's name without its amount.
	 *
	 * @param  name the norm row's name, e.g. Ensamstående 3940.00
	 * @return      the name, e.g. Ensamstående
	 */
	static String normRowName(final String name) {
		return name.replaceAll(AMOUNT_SUFFIX, "").trim();
	}

	/** The norm row with the id on the beräkning's norm. */
	static Optional<ObjectNode> normRow(final JsonNode calculation, final JsonNode rowId) {
		return find(calculation.path(NORM), ROWS, row -> same(row.path(ROW_ID), rowId));
	}

	/**
	 * The name of each placed member's normintervall, from the norm's rows. Lifecare's placement answers with the row but
	 * not its name, and the web app saves the name with the member.
	 */
	static ObjectNode withNormRowNames(final ObjectNode calculation) {
		objects(calculation, PERSONS).forEach(member -> {
			if (integer(member, NORM_ROW_ID) > 0 && !truthy(member, NORM_ROW)) {
				normRow(calculation, member.path(NORM_ROW_ID))
					.ifPresent(row -> member.put(NORM_ROW, normRowName(Optional.ofNullable(text(row, "name")).orElse(""))));
			}
		});
		return calculation;
	}

	/**
	 * The members on the norm: one already on a normintervall keeps it and its amount (the caseworker may have picked it),
	 * and one not yet placed takes the row and amount Lifecare placed it on. Members left out are taken off the norm. On a
	 * new norm (keepPlacements false) every member takes what Lifecare placed it on, since the old norm's rows mean
	 * nothing on the new one; one Lifecare finds no row for stays unplaced, for the caseworker to pick.
	 */
	static ObjectNode withPlacedPersons(final ObjectNode calculation, final List<ObjectNode> placed, final boolean keepPlacements) {
		objects(calculation, PERSONS).forEach(member -> {
			if (!truthy(member, INCLUDED)) {
				unplace(member);
				return;
			}
			if (keepPlacements && integer(member, NORM_ROW_ID) > 0) {
				return;
			}
			placed.stream()
				.filter(candidate -> same(candidate.path(PERSON_ID), member.path(PERSON_ID)))
				.findFirst()
				.ifPresentOrElse(placement -> {
					setOrRemove(member, NORM_ROW_ID, placement.path(NORM_ROW_ID));
					setOrRemove(member, NORM_ROW, placement.path(NORM_ROW));
					setOrRemove(member, AMOUNT, placement.path(AMOUNT));
				}, () -> unplace(member));
		});
		return calculation;
	}

	private static void unplace(final ObjectNode member) {
		member.put(NORM_ROW_ID, 0);
		member.putNull(NORM_ROW);
		member.put(AMOUNT, 0);
	}

	/**
	 * Counts jobbstimulans on the applicant's incomes the way the web app sends it: on an income type jobbstimulans
	 * applies to, the entered amount is the gross and the counted amount what is left once the type's percent is taken
	 * off (Lön efter skatt 5 000 at 25 % goes as 5 000 gross and 3 750 counted). Only when Lifecare has marked the
	 * applicant as having jobbstimulans in the period.
	 */
	static ObjectNode withJobStimulusIncomes(final ObjectNode calculation, final JsonNode types) {
		if (!isTrue(calculation, "hasApplicantJobStimuli")) {
			return calculation;
		}
		objects(calculation, INCOMES).forEach(row -> {
			final var type = typeOf(types, row.path(INCOME_CODE));
			final var percent = type.map(found -> number(found, "jobStimulusPercent")).orElse(0.0);
			final var jobStimulus = type.filter(found -> truthy(found, "isJobStimulus")).isPresent();
			if (!jobStimulus || percent == 0 || number(row, AMOUNT_APPLICANT) == 0) {
				return;
			}
			final var gross = row.path(AMOUNT_APPLICANT).deepCopy();
			final var counted = Math.round(gross.doubleValue() * (100 - percent)) / 100.0;
			row.set(AMOUNT_APPLICANT, numberNode(counted));
			row.set(GROSS_AMOUNT_APPLICANT, gross);
		});
		return calculation;
	}

	/** The type in a Lifecare catalogue (an array of {id, text, ...}) with the id. */
	static Optional<ObjectNode> typeOf(final JsonNode types, final JsonNode id) {
		return LifecareJson.elements(types).stream().filter(candidate -> same(candidate.path("id"), id)).findFirst();
	}

	/** The members included. */
	static int members(final JsonNode calculation) {
		return (int) objects(calculation, PERSONS).stream().filter(person -> truthy(person, INCLUDED)).count();
	}

	/**
	 * The household size as the draft says it: the caseworker's own, or the members included.
	 *
	 * @param  calculation            the beräkning
	 * @param  hasCustomHouseholdSize the draft's flag
	 * @param  householdSize          the draft's size
	 * @return                        the household size
	 */
	static HouseholdSize householdSizeOf(final JsonNode calculation, final Boolean hasCustomHouseholdSize, final Integer householdSize) {
		final var members = members(calculation);
		final var custom = Boolean.TRUE.equals(hasCustomHouseholdSize) && householdSize != null;
		if (custom) {
			return new HouseholdSize(true, householdSize, members);
		}
		return new HouseholdSize(false, members, members);
	}

	/** The household size a saved beräkning holds: its own when the caseworker set one, else the members included. */
	static HouseholdSize householdSizeOfSaved(final JsonNode calculation) {
		final var members = members(calculation);
		final var saved = integerOrNull(calculation, HOUSEHOLD_SIZE);
		if (truthy(calculation, HAS_CUSTOM_HOUSEHOLD_SIZE) && saved != null) {
			return new HouseholdSize(true, saved, members);
		}
		return new HouseholdSize(false, members, members);
	}
}
