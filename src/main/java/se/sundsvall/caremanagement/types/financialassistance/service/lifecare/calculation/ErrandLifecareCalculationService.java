package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareCalculationView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningPreviousCalculation;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareAccessRecorder;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrand;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrandService;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.PERSONS;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationDraftFill.applyDraft;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.integerOrNull;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.objects;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.householdSizeOf;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.withJobStimulusIncomes;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareCalculationEditService.READ_CALCULATION;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareCalculationEditService.READ_UNDERLAG;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareCalculationEditService.TARGET;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareCalculationEditService.asObject;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningMapper.toLifecareCalculationView;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.NormberakningMapper.toPreviousCalculation;

/**
 * The errand's normberäkning, kept in Lifecare. careM's draft (filled from the application and SSBTEK) is the working
 * copy until the first save creates the beräkning in Lifecare (Calculation/Create); the errand then keeps only the
 * reference (lifecareCalculationId). After that Lifecare owns the beräkning, and Lifecare's own summering is the
 * result shown.
 *
 * <p>
 * careM's daily prepare may also create the beräkning (as the normberäkning proposal, through the FamilyCare API).
 * Whichever links first wins; the link is write-once.
 * </p>
 */
@Service
public class ErrandLifecareCalculationService {

	private static final Logger LOG = LoggerFactory.getLogger(ErrandLifecareCalculationService.class);
	private static final ZoneId SWEDISH_TIME = ZoneId.of("Europe/Stockholm");

	private final LifecareErrandService errandService;
	private final LifecareCalculationClient client;
	private final LifecareCalculationEditService editService;
	private final NormberakningDraftReader draftReader;
	private final LifecareAccessRecorder recorder;

	ErrandLifecareCalculationService(final LifecareErrandService errandService, final LifecareCalculationClient client, final LifecareCalculationEditService editService,
		final NormberakningDraftReader draftReader, final LifecareAccessRecorder recorder) {
		this.errandService = errandService;
		this.client = client;
		this.editService = editService;
		this.draftReader = draftReader;
		this.recorder = recorder;
	}

	/**
	 * The errand's beräkning as it stands in Lifecare, or empty while none has been saved.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the beräkning
	 */
	public Optional<LifecareCalculationView> read(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		return errand.calculation().map(calculationId -> {
			final var forEdit = client.readForEdit(calculationId);
			recorder.read(errand, TARGET, READ_CALCULATION, String.valueOf(calculationId));
			return toLifecareCalculationView(forEdit.path("calculation"));
		});
	}

	/**
	 * The errand's beräkning as Lifecare prints it; 404 while none is saved there.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the PDF
	 */
	public byte[] pdf(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var calculationId = errand.calculation()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Normberäkningen är inte sparad i Lifecare än."));
		final var pdf = client.print(calculationId);
		recorder.read(errand, TARGET, "Läste normberäkningen som PDF i Lifecare", String.valueOf(calculationId));
		return pdf;
	}

	/**
	 * Saves the beräkning in Lifecare. The first time it is created from careM's draft (Lifecare places the members on
	 * the norm, marks who has jobbstimulans and counts it) and the errand is linked to it. Once it exists Lifecare owns it
	 * and the draft is no longer used: saving again saves Lifecare's own beräkning, which is what finalize (slutlig, after
	 * which Lifecare allows no change) needs.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @param  finalize       whether to save it as slutlig
	 * @return                the beräkning as Lifecare counted it
	 */
	public LifecareCalculationView save(final String municipalityId, final String namespace, final String errandId, final boolean finalize) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var linked = errand.calculation();
		if (linked.isPresent()) {
			return toLifecareCalculationView(editService.change(errand, linked.get(), (calculation, _) -> calculation, finalize));
		}

		final var serviceId = errand.requireServiceId();
		final var draft = draftReader.read(errand);
		final var proposal = client.readProposal(serviceId);
		final var jobStimulus = client.readJobStimulus(serviceId);
		recorder.read(errand, TARGET, READ_UNDERLAG);

		final var base = asObject(proposal.path("calculation"));
		final var filled = applyDraft(base, objects(base, PERSONS), draft.draft(), draft.persons(), proposal, LocalDate.now(SWEDISH_TIME).toString());
		final var calculation = withJobStimulusIncomes(editService.placeAndMark(filled, jobStimulus, true), proposal.path("incomeTypes"));
		final var household = householdSizeOf(calculation, draft.draft().getHasCustomHouseholdSize(), draft.draft().getHouseholdSize());

		final var created = createInLifecare(serviceId, CalculationBodyBuilder.create(calculation, household));
		final var calculationId = Optional.ofNullable(integerOrNull(created, "calculationId"))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "Normberäkningen sparades i Lifecare men Lifecare svarade utan dess id. Kontrollera i Lifecare innan du sparar igen."));
		recorder.written(errand, LifecareAccessEntry.CREATE, TARGET, "Sparade normberäkningen i Lifecare", String.valueOf(calculationId));
		link(errand, calculationId);
		if (!finalize) {
			return toLifecareCalculationView(created);
		}
		// Slutlig is a change to a saved beräkning, so the new one is saved again as slutlig.
		return toLifecareCalculationView(editService.change(errand, calculationId, (saved, _) -> saved, true));
	}

	/**
	 * The beräkning preceding the errand's own period, read from Lifecare: which one comes from the insats's list of
	 * beräkningar, its rows and sums from GetCalculation. The errand's own period is its saved beräkning's, or careM's
	 * draft's before that.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the previous beräkning, empty when the insats has none before the period
	 */
	public Optional<NormberakningPreviousCalculation> readPrevious(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		if (errand.serviceId() == null) {
			return Optional.empty();
		}
		final var listed = client.listForService(errand.serviceId());
		recorder.read(errand, TARGET, "Läste insatsens normberäkningar i Lifecare");

		final var own = errand.calculation()
			.flatMap(ownId -> CalculationJson.elements(listed).stream().filter(item -> CalculationJson.hasNumber(item, "calculationId", ownId)).findFirst())
			.map(item -> text(item, "startDate"));
		final var periodStart = own.or(() -> draftReader.periodStart(errand)).orElse(null);

		return PreviousCalculationPicker.pick(listed, periodStart, errand.calculationId()).map(previous -> {
			final var previousId = integerOrNull(previous, "calculationId");
			final var calculation = client.read(previousId);
			recorder.read(errand, TARGET, "Läste föregående normberäkning i Lifecare", String.valueOf(previousId));
			return toPreviousCalculation(calculation);
		});
	}

	/** Creates the beräkning, telling a call Lifecare never answered apart from a refusal. A create is never retried. */
	private JsonNode createInLifecare(final int serviceId, final ObjectNode body) {
		try {
			return client.create(serviceId, body);
		} catch (final RuntimeException e) {
			if (e instanceof final ThrowableProblem problem && (problem.getStatus() == BAD_REQUEST || problem.getStatus() == UNPROCESSABLE_CONTENT)) {
				throw problem;
			}
			LOG.warn("Calculation/Create on insats {} got no answer ({})", serviceId, e.getClass().getSimpleName());
			throw Problem.valueOf(BAD_GATEWAY, "Lifecare svarade inte när normberäkningen sparades. Kontrollera i Lifecare om den finns innan du sparar igen.");
		}
	}

	/** Links the errand to the new beräkning. Without the link the next save would make a second one. */
	private void link(final LifecareErrand errand, final int calculationId) {
		try {
			errandService.linkCalculation(errand, calculationId);
		} catch (final RuntimeException e) {
			LOG.error("Calculation {} was created in Lifecare but errand {} could not be linked to it ({})", calculationId, errand.errandId(), e.getClass().getSimpleName());
			throw Problem.valueOf(BAD_GATEWAY, "Normberäkningen sparades i Lifecare (beräkning %d) men kunde inte kopplas till ärendet. Spara inte igen – då skapas en beräkning till."
				.formatted(calculationId));
		}
	}
}
