package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebErrors;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebProperties;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionProposal;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionReason;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionSaveRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionView;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionBodies.buildCreate;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionBodies.buildUpdate;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapper.toPrefill;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapper.toReasons;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapper.toTypes;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapper.toView;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.integer;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * The errand's beslut, kept in Lifecare. The caseworker's Spara writes it there, Decision/Create the first time and
 * Decision/Update after that, and the errand keeps only the reference ({@code lifecareDecisionId}), so the same beslut
 * is found and changed again rather than a second one made. Previews and what goes to the applicant are Lifecare's own
 * print of it.
 *
 * <p>
 * Lifecare accepts a wrong beslut without complaint, so every write goes through the checks in
 * {@link LifecareDecisionBodies}, and a refusal is handed back in words the caseworker can act on.
 * </p>
 */
@Service
public class LifecareDecisionService {

	static final String TARGET_DECISION = "DECISION";
	static final String ERROR_NOT_SAVED = "Beslutet är inte sparat i Lifecare ännu.";
	static final String ERROR_CREATE_UNCERTAIN = "Lifecare svarade inte när beslutet sparades. Kontrollera i Lifecare om beslutet finns innan du sparar igen.";
	static final String ERROR_ALREADY_LINKED = "Ärendet har redan ett annat beslut kopplat. Beslut %s som just skapades i Lifecare är överflödigt – makulera det i Lifecare och läs om ärendet.";
	static final String ERROR_LINK_FAILED = "Beslutet sparades i Lifecare (beslut %s) men kunde inte kopplas till ärendet. Spara inte igen – då skapas ett beslut till.";

	private static final Logger LOG = LoggerFactory.getLogger(LifecareDecisionService.class);

	private final LifecareErrandService errandService;
	private final LifecareDecisionClient lifecare;
	private final LifecareAccessRecorder accessRecorder;
	private final ProfessionalWebProperties properties;

	LifecareDecisionService(final LifecareErrandService errandService, final LifecareDecisionClient lifecare, final LifecareAccessRecorder accessRecorder,
		final ProfessionalWebProperties properties) {
		this.errandService = errandService;
		this.lifecare = lifecare;
		this.accessRecorder = accessRecorder;
		this.properties = properties;
	}

	/**
	 * The errand's beslut as it stands in Lifecare.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the beslut, empty while none has been saved
	 */
	public Optional<LifecareDecisionView> read(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		return errand.decision().map(decisionId -> {
			final var saved = lifecare.readDecision(decisionId);
			accessRecorder.read(errand, TARGET_DECISION, "Läste beslutet i Lifecare", String.valueOf(decisionId));
			return toView(saved);
		});
	}

	/**
	 * The beslutstyper the errand's insats offers: Lifecare's own list, read from its underlag.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the active beslutstyper
	 */
	public List<LifecareDecisionType> types(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var proposal = lifecare.readProposal(errand.requireServiceId());
		accessRecorder.read(errand, TARGET_DECISION, "Läste beslutstyper i Lifecare");
		return toTypes(proposal);
	}

	/**
	 * What a new beslut gets from the errand's normberäkning: Lifecare's own underlag read with it, as its calculation
	 * view reads it for "Besluta". Amount and period are there only when the normberäkning is saved as final. Lifecare
	 * keeps no link between a beslut and a normberäkning, so this is how Drakel shows the amount before Spara and whether
	 * the caseworker's differs.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the proposal; without calculationId when the errand has no normberäkning linked
	 */
	public LifecareDecisionProposal proposal(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var serviceId = errand.requireServiceId();
		return errand.calculation().map(calculationId -> {
			final var proposal = lifecare.readProposal(serviceId, calculationId);
			accessRecorder.read(errand, TARGET_DECISION, "Läste beslutsunderlag från normberäkningen i Lifecare", String.valueOf(calculationId));
			return toPrefill(proposal, calculationId)
				.map(prefill -> new LifecareDecisionProposal(calculationId, true, prefill.amount(), prefill.periodFrom().toString(), Optional.ofNullable(prefill.periodTo()).map(
					LocalDate::toString).orElse(null)))
				.orElseGet(() -> new LifecareDecisionProposal(calculationId, false, null, null, null));
		}).orElseGet(() -> new LifecareDecisionProposal(null, false, null, null, null));
	}

	/**
	 * The orsaker a beslut of the beslutstyp can carry: Lifecare's catalogue, which belongs to the type, not a person.
	 *
	 * @param  decisionCode the beslutstyp
	 * @return              the orsaker
	 */
	public List<LifecareDecisionReason> reasons(final int decisionCode) {
		return toReasons(lifecare.readReasons(decisionCode));
	}

	/**
	 * Saves the beslut in Lifecare: creates it the first time and links the errand to it, changes the same beslut every
	 * time after. The beslutsfattare is the caseworker saving it (X-Sent-By), or the configured test decision maker.
	 *
	 * <p>
	 * With a normberäkning linked to the errand the underlag is read with it, as Lifecare's calculation view reads it.
	 * An amount or period the caseworker leaves out then comes from the normberäkning (when it is saved as final); one
	 * the caseworker gives always wins, since Lifecare lets the beslut differ from the normberäkning. Lifecare keeps no
	 * link between the two, so the access log records which normberäkning the beslut was made from and any amount that
	 * differs from it.
	 * </p>
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @param  request        the beslut
	 * @return                the beslut as it stands in Lifecare after the save
	 */
	public LifecareDecisionView save(final String municipalityId, final String namespace, final String errandId, final LifecareDecisionSaveRequest request) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var serviceId = errand.requireServiceId();
		final var decisionMaker = decisionMaker();

		final var calculationId = errand.calculation();
		final var proposal = calculationId.map(id -> lifecare.readProposal(serviceId, id)).orElseGet(() -> lifecare.readProposal(serviceId));
		final var saved = errand.decision().map(lifecare::readDecision);
		accessRecorder.read(errand, TARGET_DECISION, "Läste beslutsunderlag i Lifecare");
		final var prefill = calculationId.flatMap(id -> toPrefill(proposal, id));
		final var input = toInput(prefill.map(basis -> basis.fillIn(request)).orElse(request), decisionMaker);
		final var basisDescription = prefill.map(basis -> describeBasis(basis, input)).orElse("");

		if (saved.isPresent()) {
			final var decisionId = errand.decisionId();
			final var updated = lifecare.update(decisionId, buildUpdate(saved.get(), proposal, input));
			accessRecorder.written(errand, LifecareAccessEntry.UPDATE, TARGET_DECISION, describeWrite(input, "Ändrade och skrivskyddade beslutet i Lifecare",
				"Ändrade beslutet i Lifecare") + basisDescription, String.valueOf(decisionId));
			return toView(updated);
		}

		final var created = createInLifecare(serviceId, buildCreate(proposal, input));
		accessRecorder.written(errand, LifecareAccessEntry.CREATE, TARGET_DECISION, describeWrite(input, "Registrerade och skrivskyddade beslutet i Lifecare",
			"Registrerade beslutet i Lifecare") + basisDescription, String.valueOf(created));
		link(errand, created);
		return toView(lifecare.readDecision(created));
	}

	/**
	 * The errand's beslut as Lifecare prints it: for the preview and for what is sent to the applicant.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the PDF
	 */
	public byte[] pdf(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var decisionId = errand.decision().orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERROR_NOT_SAVED));
		final var pdf = lifecare.printDecision(decisionId);
		accessRecorder.read(errand, TARGET_DECISION, "Hämtade beslutet som PDF från Lifecare", String.valueOf(decisionId));
		return pdf;
	}

	private static LifecareDecisionInput toInput(final LifecareDecisionSaveRequest request, final String decisionMaker) {
		return new LifecareDecisionInput(request.decisionCode(), request.date(), request.periodFrom(), request.periodTo(), request.amount(),
			request.reasonCode(), request.decisionMessage(), Boolean.TRUE.equals(request.writeProtect()), decisionMaker, request.coApplicantReasonCode());
	}

	/**
	 * Which normberäkning the beslut was made from, and the amount when the caseworker's differs from it. Lifecare keeps
	 * neither, so the access log does.
	 */
	private static String describeBasis(final LifecareDecisionPrefill basis, final LifecareDecisionInput input) {
		final var from = ", från normberäkning %d".formatted(basis.calculationId());
		final var amount = Optional.ofNullable(input.amount()).orElse(BigDecimal.ZERO);
		if (amount.compareTo(basis.amount()) == 0) {
			return from;
		}
		return from + " (belopp %s i stället för normberäkningens %s)".formatted(amount.toPlainString(), basis.amount().toPlainString());
	}

	/** The configured test decision maker when there is one, otherwise the caseworker saving the beslut. */
	private String decisionMaker() {
		return Optional.ofNullable(properties.testDecisionMaker())
			.filter(StringUtils::hasText)
			.orElseGet(errandService::caller);
	}

	private static String describeWrite(final LifecareDecisionInput input, final String writeProtected, final String plain) {
		if (input.writeProtect()) {
			return writeProtected;
		}
		return plain;
	}

	/**
	 * Creates the beslut. A call Lifecare did not answer may still have written it, and saving again would then make a
	 * second one, so that case is told apart from a plain refusal.
	 */
	private int createInLifecare(final int serviceId, final JsonNode body) {
		final JsonNode created;
		try {
			created = lifecare.create(serviceId, body);
		} catch (final ThrowableProblem problem) {
			if (ProfessionalWebErrors.isRefusal(problem)) {
				throw problem;
			}
			throw Problem.valueOf(BAD_GATEWAY, ERROR_CREATE_UNCERTAIN);
		} catch (final RuntimeException _) {
			throw Problem.valueOf(BAD_GATEWAY, ERROR_CREATE_UNCERTAIN);
		}
		return integer(created.path("decisionId"))
			.filter(id -> id > 0)
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_CREATE_UNCERTAIN));
	}

	/** Links the errand to the new beslut. Without the link the next save would make a second beslut. */
	private void link(final LifecareErrand errand, final int decisionId) {
		try {
			errandService.linkDecision(errand, decisionId);
		} catch (final RuntimeException e) {
			if ((e instanceof final ThrowableProblem problem) && (problem.getStatus() == CONFLICT)) {
				// Another save linked its beslut first: this one is surplus in Lifecare, and saving again is safe.
				throw Problem.valueOf(CONFLICT, ERROR_ALREADY_LINKED.formatted(decisionId));
			}
			LOG.error("Beslut {} was created in Lifecare but errand {} could not be linked to it ({})", decisionId, sanitizeForLogging(errand.errandId()), e.getClass().getSimpleName());
			throw Problem.valueOf(BAD_GATEWAY, ERROR_LINK_FAILED.formatted(decisionId));
		}
	}
}
