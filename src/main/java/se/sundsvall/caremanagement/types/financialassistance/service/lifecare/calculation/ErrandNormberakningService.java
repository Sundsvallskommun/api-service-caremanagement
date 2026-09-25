package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningRowInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningTypeOption;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningTypes;
import se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceTypes;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceCalculationService;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceDraftRowService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareAccessRecorder;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrand;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrandService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareCalculationEditService.CalculationChange;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareCalculationEditService.TARGET;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningMapper.toCaremDraftView;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningMapper.toExpenseInput;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningMapper.toIncomeInput;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningMapper.toNormOptions;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningMapper.toPersonInput;

/**
 * The Normberäkning tab's rows, wherever they are kept. Until the beräkning is first saved in Lifecare they are careM's
 * draft (filled from the application and SSBTEK), and every change goes to the draft with careM's type catalogues. Once
 * saved, Lifecare owns the beräkning: rows are read from it and every change is made there, with Lifecare's own
 * catalogues; the draft is then frozen and no longer shown.
 */
@Service
public class ErrandNormberakningService {

	/** The persons section. */
	public static final String PERSONS = "persons";
	/** The incomes section. */
	public static final String INCOMES = "incomes";
	/** The expenses section. */
	public static final String EXPENSES = "expenses";

	private static final Logger LOG = LoggerFactory.getLogger(ErrandNormberakningService.class);
	private static final String HOUSING_GROUP = "HOUSING";

	private final LifecareErrandService errandService;
	private final LifecareCalculationEditService editService;
	private final LifecareCalculationClient client;
	private final NormberakningDraftReader draftReader;
	private final FinancialAssistanceCalculationService calculationService;
	private final FinancialAssistanceDraftRowService draftRowService;
	private final LifecareAccessRecorder recorder;

	ErrandNormberakningService(final LifecareErrandService errandService, final LifecareCalculationEditService editService, final LifecareCalculationClient client,
		final NormberakningDraftReader draftReader, final FinancialAssistanceCalculationService calculationService, final FinancialAssistanceDraftRowService draftRowService,
		final LifecareAccessRecorder recorder) {
		this.errandService = errandService;
		this.editService = editService;
		this.client = client;
		this.draftReader = draftReader;
		this.calculationService = calculationService;
		this.draftRowService = draftRowService;
		this.recorder = recorder;
	}

	/**
	 * The rows: careM's draft (source CAREM), or Lifecare's beräkning once saved there (source LIFECARE).
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the rows
	 */
	public NormberakningDraft readDraft(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		if (errand.calculation().isPresent()) {
			return editService.readDraftView(errand, errand.calculation().get());
		}
		final var draft = draftReader.read(errand);
		return toCaremDraftView(draft.draft(), draft.personalNumbers());
	}

	/**
	 * The norms, income and cost types a new row can have: Lifecare's once the beräkning is there, careM's before (with
	 * Lifecare's norms for the insats, best-effort).
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the catalogues
	 */
	public NormberakningTypes types(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		if (errand.calculation().isPresent()) {
			return editService.readTypes(errand, errand.calculation().get());
		}
		// The HOUSING section is Lifecare's boendekostnader (utgifter); the other sections are levnadskostnader i övrigt.
		return NormberakningTypes.create()
			.withNorms(lifecareNorms(errand))
			.withIncomeTypes(FinancialAssistanceTypes.INCOME_TYPES.stream().map(NormberakningMapper::toTypeOption).toList())
			.withCostTypes(FinancialAssistanceTypes.COST_TYPES.stream().filter(type -> HOUSING_GROUP.equals(type.getGroup())).map(NormberakningMapper::toTypeOption).toList())
			.withLivingCostTypes(FinancialAssistanceTypes.COST_TYPES.stream().filter(type -> !HOUSING_GROUP.equals(type.getGroup())).map(NormberakningMapper::toTypeOption).toList());
	}

	/**
	 * Changes the header: in careM's draft anything it holds; in Lifecare the norm and the household size (the period is
	 * Lifecare's).
	 *
	 * @param municipalityId the municipality
	 * @param namespace      the namespace
	 * @param errandId       the errand
	 * @param input          the header change
	 */
	public void updateHeader(final String municipalityId, final String namespace, final String errandId, final NormHeaderInput input) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		if (errand.calculation().isPresent()) {
			editService.change(errand, errand.calculation().get(), (calculation, forEdit) -> CalculationRowChanges.changeHeader(calculation, forEdit, input), false);
			return;
		}
		calculationService.patchDraftHeader(municipalityId, namespace, errandId, input);
	}

	/**
	 * Adds a caseworker row to a section. Who is in the household is Lifecare's once the beräkning is there.
	 *
	 * @param municipalityId the municipality
	 * @param namespace      the namespace
	 * @param errandId       the errand
	 * @param section        persons, incomes or expenses
	 * @param input          the row
	 */
	public void addRow(final String municipalityId, final String namespace, final String errandId, final String section, final NormberakningRowInput input) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		if (errand.calculation().isPresent()) {
			final CalculationChange change = switch (section) {
				case INCOMES -> (calculation, forEdit) -> CalculationRowChanges.addIncome(calculation, forEdit.path("incomeTypes"), input);
				case EXPENSES -> (calculation, forEdit) -> CalculationRowChanges.addExpense(calculation, forEdit, input);
				case PERSONS -> refusal("Personer läggs till i hushållet i Lifecare.");
				default -> throw unknownSection(section);
			};
			editService.change(errand, errand.calculation().get(), change, false);
			return;
		}
		switch (section) {
			case INCOMES -> draftRowService.addDraftIncome(municipalityId, namespace, errandId, toIncomeInput(input));
			case EXPENSES -> draftRowService.addDraftExpense(municipalityId, namespace, errandId, toExpenseInput(input));
			case PERSONS -> draftRowService.addDraftPerson(municipalityId, namespace, errandId, toPersonInput(input));
			default -> throw unknownSection(section);
		}
	}

	/**
	 * Sets the caseworker's values on a row.
	 *
	 * @param municipalityId the municipality
	 * @param namespace      the namespace
	 * @param errandId       the errand
	 * @param section        persons, incomes or expenses
	 * @param rowId          the row
	 * @param input          the values
	 */
	public void updateRow(final String municipalityId, final String namespace, final String errandId, final String section, final String rowId, final NormberakningRowInput input) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		if (errand.calculation().isPresent()) {
			final CalculationChange change = switch (section) {
				case INCOMES -> (calculation, _) -> CalculationRowChanges.changeIncome(calculation, rowId, input);
				case EXPENSES -> (calculation, _) -> CalculationRowChanges.changeExpense(calculation, rowId, input);
				case PERSONS -> (calculation, _) -> CalculationRowChanges.changePerson(calculation, rowId, input);
				default -> throw unknownSection(section);
			};
			editService.change(errand, errand.calculation().get(), change, false);
			return;
		}
		switch (section) {
			case INCOMES -> draftRowService.patchDraftIncome(municipalityId, namespace, errandId, rowId, toIncomeInput(input));
			case EXPENSES -> draftRowService.patchDraftExpense(municipalityId, namespace, errandId, rowId, toExpenseInput(input));
			case PERSONS -> draftRowService.patchDraftPerson(municipalityId, namespace, errandId, rowId, toPersonInput(input));
			default -> throw unknownSection(section);
		}
	}

	/**
	 * Removes a row: a soft delete in careM's draft, the amounts to 0 in Lifecare (which then drops the row).
	 *
	 * @param municipalityId the municipality
	 * @param namespace      the namespace
	 * @param errandId       the errand
	 * @param section        persons, incomes or expenses
	 * @param rowId          the row
	 */
	public void deleteRow(final String municipalityId, final String namespace, final String errandId, final String section, final String rowId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		if (errand.calculation().isPresent()) {
			final CalculationChange change = switch (section) {
				case INCOMES -> (calculation, _) -> CalculationRowChanges.removeIncome(calculation, rowId);
				case EXPENSES -> (calculation, _) -> CalculationRowChanges.removeExpense(calculation, rowId);
				case PERSONS -> refusal("Personer tas inte bort ur normberäkningen från Drakel.");
				default -> throw unknownSection(section);
			};
			editService.change(errand, errand.calculation().get(), change, false);
			return;
		}
		setDeleted(municipalityId, namespace, errandId, section, rowId, true);
	}

	/**
	 * Restores a soft-deleted row of careM's draft. A beräkning in Lifecare has no soft delete.
	 *
	 * @param municipalityId the municipality
	 * @param namespace      the namespace
	 * @param errandId       the errand
	 * @param section        persons, incomes or expenses
	 * @param rowId          the row
	 */
	public void restoreRow(final String municipalityId, final String namespace, final String errandId, final String section, final String rowId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		if (errand.calculation().isPresent()) {
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, "En borttagen rad går inte att ändra från Drakel när normberäkningen är sparad i Lifecare. Gör det i Lifecare.");
		}
		setDeleted(municipalityId, namespace, errandId, section, rowId, false);
	}

	private void setDeleted(final String municipalityId, final String namespace, final String errandId, final String section, final String rowId, final boolean deleted) {
		switch (section) {
			case INCOMES -> draftRowService.setDraftIncomeDeleted(municipalityId, namespace, errandId, rowId, deleted);
			case EXPENSES -> draftRowService.setDraftExpenseDeleted(municipalityId, namespace, errandId, rowId, deleted);
			case PERSONS -> draftRowService.setDraftPersonDeleted(municipalityId, namespace, errandId, rowId, deleted);
			default -> throw unknownSection(section);
		}
	}

	/**
	 * The norms a beräkning on the insats can have, from Lifecare's underlag for a new one. Best-effort: without them the
	 * norm cannot be changed, the rest still works.
	 */
	private List<NormberakningTypeOption> lifecareNorms(final LifecareErrand errand) {
		if (errand.serviceId() == null) {
			return List.of();
		}
		try {
			final var proposal = client.readProposal(errand.serviceId());
			recorder.read(errand, TARGET, "Läste normer i Lifecare");
			return toNormOptions(proposal.path("norms"));
		} catch (final RuntimeException e) {
			LOG.info("No Lifecare norms for errand {} ({})", errand.errandId(), e.getClass().getSimpleName());
			return List.of();
		}
	}

	private static CalculationChange refusal(final String reason) {
		return (_, _) -> {
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, reason);
		};
	}

	private static RuntimeException unknownSection(final String section) {
		return Problem.valueOf(BAD_REQUEST, "Unknown normberäkning section: %s".formatted(section));
	}
}
