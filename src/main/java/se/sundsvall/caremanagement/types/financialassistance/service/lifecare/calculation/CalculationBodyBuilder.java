package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.NODES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.array;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.isTrue;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.objects;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.orNull;

/**
 * The bodies of {@code Calculation/Create} and {@code Calculation/Update}, byte for byte the way Lifecare's web app
 * sends them (captures 2026-09-24, as reproduced by Drakel's BFF). This is the fragile part of the integration and the
 * only place that knows the shape.
 *
 * <p>
 * Lifecare's web app is a Knockout application, and it sends its view-model's internal state along with every member:
 * </p>
 * <ul>
 * <li>{@code isValid: true} on every member;</li>
 * <li>{@code normSubscription}, {@code dateSubscriptions} (two of them) and {@code daySubscription}: Knockout
 * subscription objects of the shape {da, Jb, Kb, hb} where da holds the observed value (the norm row id, an empty date,
 * the member's days) and Jb, Kb, hb are always false, null, null. A member not placed on the norm goes without a
 * normRowId at all, and its normSubscription without da;</li>
 * <li>the household size twice: in Lifecare's own camelCase fields where Lifecare read it (Update only), and again in
 * the PascalCase fields HasCustomHouseholdSize, HouseholdSize and NumberOfFamilyMembers appended at the end of the
 * body.</li>
 * </ul>
 *
 * <p>
 * Lifecare accepts the bodies without these additions in some cases and silently loses data in others, which is why
 * they are reproduced exactly rather than trimmed. Field order follows the tree Lifecare handed out, new fields last.
 * </p>
 */
final class CalculationBodyBuilder {

	static final String PERSONS = "calculationPersons";
	static final String INCOMES = "calculationIncomes";
	static final String EXPENSES = "calculationExpenses";
	static final String SPECIAL_EXPENSES = "calculationSpecialExpenses";

	private static final String NORM_ROW_ID = "normRowId";
	private static final String CHANGEABLE = "changeable";
	private static final String IS_VALID = "isValid";
	private static final String HOUSEHOLD_SIZE = "householdSize";
	private static final String NUMBER_OF_FAMILY_MEMBERS = "numberOfFamilyMembers";

	private CalculationBodyBuilder() {}

	/**
	 * The household size as it goes to Lifecare.
	 *
	 * @param custom  whether the household has an own size (Annan hushållsstorlek)
	 * @param size    the size the gemensamma kostnader are counted on
	 * @param members the members included
	 */
	record HouseholdSize(boolean custom, int size, int members) {}

	/**
	 * The Create body: the filled underlag with the web app's additions, every row marked changeable (and incomes valid),
	 * the three fields the web app leaves out of a new beräkning dropped, and the household size in its PascalCase fields
	 * at the end.
	 *
	 * @param  calculation the filled and placed beräkning
	 * @param  household   the household size
	 * @return             the body
	 */
	static ObjectNode create(final ObjectNode calculation, final HouseholdSize household) {
		final var body = calculation.deepCopy();
		body.set(PERSONS, array(objects(calculation, PERSONS).stream().map(CalculationBodyBuilder::asSentPerson).toList()));
		body.set(INCOMES, array(objects(calculation, INCOMES).stream().map(row -> changeable(row, true)).toList()));
		body.set(EXPENSES, array(objects(calculation, EXPENSES).stream().map(row -> changeable(row, false)).toList()));
		body.set(SPECIAL_EXPENSES, array(objects(calculation, SPECIAL_EXPENSES).stream().map(row -> changeable(row, false)).toList()));
		body.remove("aktualiseringId");
		body.remove(HOUSEHOLD_SIZE);
		body.remove(NUMBER_OF_FAMILY_MEMBERS);
		return withHouseholdFields(body, household);
	}

	/**
	 * The Update body: the saved beräkning as read for edit with the changes, members as the web app sends them, incomes
	 * marked changeable and valid, and the household size both where Lifecare read it and in the PascalCase fields at the
	 * end. Finalize saves it as slutlig: the same call with isFinalized set, after which Lifecare allows no change.
	 *
	 * @param  calculation the changed beräkning
	 * @param  household   the household size
	 * @param  finalize    whether to save it as slutlig
	 * @return             the body
	 */
	static ObjectNode update(final ObjectNode calculation, final HouseholdSize household, final boolean finalize) {
		final var body = calculation.deepCopy();
		body.set(PERSONS, array(objects(calculation, PERSONS).stream().map(CalculationBodyBuilder::asSentPerson).toList()));
		body.set(INCOMES, array(objects(calculation, INCOMES).stream().map(row -> changeable(row, true)).toList()));
		body.put(HOUSEHOLD_SIZE, household.size());
		body.put(NUMBER_OF_FAMILY_MEMBERS, household.members());
		body.put("isFinalized", finalize || isTrue(calculation, "isFinalized"));
		return withHouseholdFields(body, household);
	}

	/**
	 * A member the way the web app sends it: isValid and its Knockout subscriptions added, and a member never placed on
	 * the norm without a normRowId at all.
	 */
	static ObjectNode asSentPerson(final ObjectNode person) {
		final var placed = integer(person, NORM_ROW_ID) > 0;
		final var sent = person.deepCopy();
		final ObjectNode normSubscription;
		if (placed) {
			normSubscription = subscription(person.get(NORM_ROW_ID).deepCopy());
		} else {
			sent.remove(NORM_ROW_ID);
			normSubscription = subscriptionWithoutValue();
		}
		sent.put(IS_VALID, true);
		sent.set("normSubscription", normSubscription);
		final var dateSubscriptions = NODES.arrayNode();
		dateSubscriptions.add(subscription(NODES.stringNode("")));
		dateSubscriptions.add(subscription(NODES.stringNode("")));
		sent.set("dateSubscriptions", dateSubscriptions);
		sent.set("daySubscription", subscription(orNull(person.path("deviationDays")).deepCopy()));
		return sent;
	}

	private static ObjectNode changeable(final ObjectNode row, final boolean valid) {
		final var copy = row.deepCopy();
		copy.put(CHANGEABLE, true);
		if (valid) {
			copy.put(IS_VALID, true);
		}
		return copy;
	}

	/** A Knockout subscription observing the value. */
	private static ObjectNode subscription(final JsonNode value) {
		final var subscription = NODES.objectNode();
		subscription.set("da", value);
		subscription.put("Jb", false);
		subscription.putNull("Kb");
		subscription.putNull("hb");
		return subscription;
	}

	/** A Knockout subscription whose observed value is undefined, which JSON leaves out. */
	private static ObjectNode subscriptionWithoutValue() {
		final var subscription = NODES.objectNode();
		subscription.put("Jb", false);
		subscription.putNull("Kb");
		subscription.putNull("hb");
		return subscription;
	}

	private static ObjectNode withHouseholdFields(final ObjectNode body, final HouseholdSize household) {
		body.put("HasCustomHouseholdSize", household.custom());
		body.put("HouseholdSize", household.size());
		body.put("NumberOfFamilyMembers", household.members());
		return body;
	}
}
