package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeResponse;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceFinalizeService;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.outcomeCarriesAmount;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionMapper.toView;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionService.TARGET_DECISION;

/**
 * Besluta och utbetala with the beslut read from Lifecare by careM itself.
 *
 * <p>
 * A finalize request without a decision is completed from the beslut the errand is linked to in Lifecare
 * ({@code lifecareDecisionId}): its outcome (from the beslutstyp's category), orsak, period, amount (0 for an avslag)
 * and beslutsmeddelande, as the Draken BFF built the request before. A request that carries a decision is finalized as
 * it is, so a client that still reads the beslut itself keeps working. Either way the Lifecare read happens here,
 * outside the finalize transaction, so no database transaction waits on Lifecare.
 * </p>
 */
@Service
public class LifecareFinalizeService {

	static final String ERROR_NOT_SAVED = "Spara beslutet innan du beslutar och betalar ut.";
	static final String ERROR_NOT_FINALIZABLE = "Beslutet i Lifecare har en beslutstyp som inte går att verkställa från Drakel.";
	static final String ERROR_INVALID = "The beslut in Lifecare cannot be recorded as it stands: %s";
	static final String ERROR_DATE = "Lifecare answered with a period careM cannot read";
	static final String ERROR_NO_DECIDER = "a decision can only be recorded by an identified user - the X-Sent-By header is required";

	private final LifecareErrandService errandService;
	private final LifecareDecisionClient lifecare;
	private final LifecareAccessRecorder accessRecorder;
	private final FinancialAssistanceFinalizeService finalizeService;
	private final Validator validator;

	LifecareFinalizeService(final LifecareErrandService errandService, final LifecareDecisionClient lifecare, final LifecareAccessRecorder accessRecorder,
		final FinancialAssistanceFinalizeService finalizeService, final Validator validator) {
		this.errandService = errandService;
		this.lifecare = lifecare;
		this.accessRecorder = accessRecorder;
		this.finalizeService = finalizeService;
		this.validator = validator;
	}

	/**
	 * Finalizes the errand, reading the decision from Lifecare when the request carries none.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @param  request        the finalize request
	 * @param  decidedBy      the caseworker (X-Sent-By)
	 * @return                the receipt
	 */
	public FinalizeResponse finalize(final String municipalityId, final String namespace, final String errandId, final FinalizeRequest request,
		final String decidedBy) {
		if (request.getDecision() != null) {
			return finalizeService.finalize(municipalityId, namespace, errandId, request, decidedBy);
		}
		if (!hasText(decidedBy)) {
			throw Problem.valueOf(BAD_REQUEST, ERROR_NO_DECIDER);
		}
		final var completed = FinalizeRequest.create()
			.withDecision(decisionFromLifecare(municipalityId, namespace, errandId))
			.withCommunication(request.getCommunication())
			.withHouseholdSizeChanged(request.getHouseholdSizeChanged());
		requireValid(completed);
		return finalizeService.finalize(municipalityId, namespace, errandId, completed, decidedBy);
	}

	private FinalizeDecision decisionFromLifecare(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var decisionId = errand.decision().orElseThrow(() -> Problem.valueOf(BAD_REQUEST, ERROR_NOT_SAVED));
		final var saved = lifecare.readDecision(decisionId);
		accessRecorder.read(errand, TARGET_DECISION, "Läste beslutet i Lifecare", String.valueOf(decisionId));

		final var view = toView(saved);
		final var outcome = Optional.ofNullable(view.outcome()).orElseThrow(() -> Problem.valueOf(BAD_REQUEST, ERROR_NOT_FINALIZABLE));
		return FinalizeDecision.create()
			.withOutcome(outcome)
			.withReason(view.reason())
			.withPeriodFrom(toDate(view.periodFrom()))
			.withPeriodTo(toDate(view.periodTo()))
			.withAmount(amountOf(outcome, view.amount()))
			.withDecisionMessage(view.message());
	}

	private static BigDecimal amountOf(final String outcome, final BigDecimal amount) {
		if (outcomeCarriesAmount(outcome)) {
			return amount;
		}
		return BigDecimal.ZERO;
	}

	/** Lifecare's yyyy-MM-dd, possibly followed by a time. */
	private static LocalDate toDate(final String value) {
		if (value == null) {
			return null;
		}
		try {
			return LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
		} catch (final DateTimeParseException _) {
			throw Problem.valueOf(BAD_GATEWAY, ERROR_DATE);
		}
	}

	/**
	 * The request built from Lifecare goes through the same rules as one a client sends (sizes, amount, period order).
	 * A beslut that breaks them is the caseworker's to fix in Lifecare, hence 422 rather than 400.
	 */
	private void requireValid(final FinalizeRequest request) {
		final var violations = validator.validate(request);
		if (!violations.isEmpty()) {
			final var detail = violations.stream()
				.map(LifecareFinalizeService::describe)
				.sorted()
				.collect(Collectors.joining(", "));
			throw Problem.valueOf(UNPROCESSABLE_CONTENT, ERROR_INVALID.formatted(detail));
		}
	}

	private static String describe(final ConstraintViolation<FinalizeRequest> violation) {
		return violation.getPropertyPath() + " " + violation.getMessage();
	}
}
