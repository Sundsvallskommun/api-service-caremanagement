package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningTypes;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareAccessRecorder;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrand;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.PERSONS;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.array;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.numberNode;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.objects;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.same;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.setOrRemove;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.truthy;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.AMOUNT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.INCLUDED;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.NORM;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.NORM_ID;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.NORM_ROW_ID;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.householdSizeOfSaved;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.normRow;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.withJobStimulusIncomes;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.withNormRowNames;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.withPlacedPersons;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.householdSizeChanged;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.needsRecount;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.sharedCostShare;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.withEnteredIncomes;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningMapper.toLifecareDraftView;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningMapper.toNormOptions;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningMapper.toTypeOptions;

/**
 * The errand's beräkning once it is saved in Lifecare, read and changed there directly: Lifecare owns it from then on.
 * Every change goes the way Lifecare's web app saves one: read for edit, change, have Lifecare place the members on
 * the norm and mark jobbstimulans, count the amounts that changed and the gemensamma kostnader, count jobbstimulans on
 * the incomes, then Calculation/Update. Reads and writes are logged on the errand.
 */
@Service
class LifecareCalculationEditService {

	static final String TARGET = "CALCULATION";
	static final String READ_UNDERLAG = "Läste beräkningsunderlag i Lifecare";
	static final String READ_CALCULATION = "Läste normberäkningen i Lifecare";

	private static final String CALCULATION = "calculation";
	private static final String INCOME_TYPES = "incomeTypes";
	private static final String START_DATE = "startDate";
	private static final String END_DATE = "endDate";

	private final LifecareCalculationClient client;
	private final LifecareAccessRecorder recorder;

	LifecareCalculationEditService(final LifecareCalculationClient client, final LifecareAccessRecorder recorder) {
		this.client = client;
		this.recorder = recorder;
	}

	/**
	 * A change to a saved beräkning: the beräkning as the caseworker entered it in (a copy, free to change), the changed
	 * one out.
	 */
	@FunctionalInterface
	interface CalculationChange {
		ObjectNode apply(ObjectNode calculation, JsonNode forEdit);
	}

	/**
	 * The saved beräkning in the tab's draft shape. The read is logged.
	 *
	 * @param  errand        the errand
	 * @param  calculationId the linked beräkning
	 * @return               the view
	 */
	NormberakningDraft readDraftView(final LifecareErrand errand, final int calculationId) {
		final var forEdit = client.readForEdit(calculationId);
		recorder.read(errand, TARGET, READ_CALCULATION, String.valueOf(calculationId));
		return toLifecareDraftView(forEdit, applicationMonthOf(errand));
	}

	/**
	 * Lifecare's own norms, income, utgift and levnadskostnad types for the beräkning. The read is logged.
	 *
	 * @param  errand        the errand
	 * @param  calculationId the linked beräkning
	 * @return               the catalogues
	 */
	NormberakningTypes readTypes(final LifecareErrand errand, final int calculationId) {
		final var forEdit = client.readForEdit(calculationId);
		recorder.read(errand, TARGET, READ_CALCULATION, String.valueOf(calculationId));
		return NormberakningTypes.create()
			.withNorms(toNormOptions(forEdit.path("norms")))
			.withIncomeTypes(toTypeOptions(forEdit.path(INCOME_TYPES)))
			.withCostTypes(toTypeOptions(forEdit.path("expenseTypes")))
			.withLivingCostTypes(toTypeOptions(forEdit.path("specialExpenseTypes")));
	}

	/**
	 * Makes the change to the saved beräkning and saves it in Lifecare; finalize saves it as slutlig, after which Lifecare
	 * allows no change.
	 *
	 * @param  errand        the errand
	 * @param  calculationId the linked beräkning
	 * @param  change        the change
	 * @param  finalize      whether to save it as slutlig
	 * @return               the beräkning as Lifecare counted it
	 */
	JsonNode change(final LifecareErrand errand, final int calculationId, final CalculationChange change, final boolean finalize) {
		final var serviceId = errand.requireServiceId();
		final var forEdit = client.readForEdit(calculationId);
		final var before = asObject(forEdit.path(CALCULATION));
		if (truthy(before, "isFinalized")) {
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, "Normberäkningen är sparad som slutlig i Lifecare och kan inte ändras.");
		}
		final var jobStimulus = client.readJobStimulus(serviceId);
		recorder.read(errand, TARGET, READ_UNDERLAG);

		final var types = forEdit.path(INCOME_TYPES);
		final var changed = change.apply(withEnteredIncomes(before.deepCopy(), types), forEdit);
		// On a new norm Lifecare decides afresh which normintervall each member is on.
		final var keepPlacements = same(changed.path(NORM_ID), before.path(NORM_ID));
		final var counted = withCountedAmounts(before, placeAndMark(changed, jobStimulus, keepPlacements));
		final var calculation = withJobStimulusIncomes(withSharedCost(before, counted), types);

		final var updated = client.update(calculationId, CalculationBodyBuilder.update(calculation, householdSizeOfSaved(calculation), finalize));
		final String description;
		if (finalize) {
			description = "Sparade normberäkningen som slutlig i Lifecare";
		} else {
			description = "Ändrade normberäkningen i Lifecare";
		}
		recorder.written(errand, LifecareAccessEntry.UPDATE, TARGET, description, String.valueOf(calculationId));
		return updated;
	}

	/**
	 * Has Lifecare place the included members on the norm for the period and mark who has jobbstimulans in it, as the web
	 * app asks before every save. Members left out are taken off the norm.
	 *
	 * @param  calculation    the beräkning, changed in place
	 * @param  jobStimulus    the insats's jobbstimulans periods
	 * @param  keepPlacements false on a new norm: every member then takes what Lifecare placed it on
	 * @return                the beräkning
	 */
	ObjectNode placeAndMark(final ObjectNode calculation, final JsonNode jobStimulus, final boolean keepPlacements) {
		final var body = CalculationJson.NODES.objectNode();
		body.set(START_DATE, calculation.path(START_DATE).deepCopy());
		body.set(END_DATE, calculation.path(END_DATE).deepCopy());
		body.set(NORM_ID, calculation.path(NORM_ID).deepCopy());
		body.set(PERSONS, array(objects(calculation, PERSONS).stream().filter(person -> truthy(person, INCLUDED)).map(ObjectNode::deepCopy).toList()));
		final var placed = client.placePersons(body);

		withPlacedPersons(calculation, objects(placed, PERSONS), keepPlacements);
		// The norm comes back with its rows and gemensamma kostnader: the new one's, when the norm was changed.
		final var norm = placed.path(NORM);
		if (!norm.isMissingNode() && !norm.isNull()) {
			calculation.set(NORM, norm.deepCopy());
		}
		withNormRowNames(calculation);

		final var marked = client.withJobStimuli(calculation.deepCopy(), jobStimulus);
		setOrRemove(calculation, "hasApplicantJobStimuli", marked.path("hasApplicantJobStimuli"));
		setOrRemove(calculation, "hasCoApplicantJobStimuli", marked.path("hasCoApplicantJobStimuli"));
		return calculation;
	}

	/**
	 * Has Lifecare count the amount of every member whose days in the household or normintervall changed; the amount
	 * follows both, by Lifecare's own rules. On a new norm every member was just placed afresh, with the amount Lifecare
	 * placed it with.
	 */
	private ObjectNode withCountedAmounts(final JsonNode before, final ObjectNode calculation) {
		if (!same(before.path(NORM_ID), calculation.path(NORM_ID))) {
			return calculation;
		}
		objects(calculation, PERSONS).stream()
			.filter(member -> needsRecount(before, member))
			.forEach(member -> normRow(calculation, member.path(NORM_ROW_ID)).ifPresent(row -> setOrRemove(member, AMOUNT,
				client.amountFor(member.deepCopy(), row.deepCopy(), calculation.path(START_DATE), calculation.path(END_DATE)))));
		return calculation;
	}

	/**
	 * Has Lifecare count the gemensamma kostnader again when the norm, the household size or the members counted changed,
	 * and takes the members' share of them.
	 */
	private ObjectNode withSharedCost(final JsonNode before, final ObjectNode calculation) {
		if (!householdSizeChanged(before, calculation)) {
			return calculation;
		}
		sharedCostShare(calculation).ifPresent(shared -> {
			final var amount = client.sharedCost(calculation.path(START_DATE), calculation.path(END_DATE), shared.normShared());
			if (!amount.isNumber()) {
				throw Problem.valueOf(BAD_GATEWAY, "Lifecare did not count the gemensamma kostnader");
			}
			calculation.set("amountForHouseholdSize", amount.deepCopy());
			calculation.set("commonHouseholdCost", numberNode(shared.share(amount.doubleValue())));
		});
		return calculation;
	}

	/** The errand's application month as yyyy-MM, or null when careM has no period for it. */
	static String applicationMonthOf(final LifecareErrand errand) {
		if (errand.periodYear() == null || errand.periodMonth() == null) {
			return null;
		}
		return "%d-%02d".formatted(errand.periodYear(), errand.periodMonth());
	}

	static ObjectNode asObject(final JsonNode node) {
		if (node instanceof final ObjectNode object) {
			return object;
		}
		throw Problem.valueOf(BAD_GATEWAY, "Lifecare answered without a beräkning");
	}
}
