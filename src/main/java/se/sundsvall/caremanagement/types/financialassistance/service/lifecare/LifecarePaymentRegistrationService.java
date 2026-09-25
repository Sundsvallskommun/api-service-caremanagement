package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebErrors;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentCreated;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentRequest;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentBodies.buildPaymentCreate;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentBodies.findRegisteredPayment;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.field;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.refuse;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.textOrEmpty;

/**
 * Registers the caseworker's utbetalning straight in Lifecare with Payment/Create, the register of record.
 *
 * <p>
 * Payment/Create is not idempotent and moves money, so every step leans towards not paying: one the builder cannot
 * vouch for is refused with the reason and nothing is sent; one Lifecare already holds is refused (409) rather than
 * made
 * again, which is also what makes a retry after an unanswered call safe; a refusal Lifecare itself gave is passed on in
 * Lifecare's words; and a call Lifecare did not answer is reported as such, since whether it paid is then unknown. It
 * is never retried. Once made, the utbetalning is linked to the errand in the same request.
 * </p>
 */
@Service
public class LifecarePaymentRegistrationService {

	static final String TARGET_PAYEES = "PAYEES";
	static final String TARGET_PAYMENT = "PAYMENT";
	static final String NO_ANSWER = "Lifecare svarade inte. Kontrollera utbetalningarna i Lifecare innan du försöker igen — en som ändå kom fram känns igen och skapas inte två gånger.";

	private static final Logger LOG = LoggerFactory.getLogger(LifecarePaymentRegistrationService.class);

	private final LifecareErrandService errandService;
	private final LifecareAccessRecorder accessRecorder;
	private final LifecarePaymentApi paymentApi;

	LifecarePaymentRegistrationService(final LifecareErrandService errandService, final LifecareAccessRecorder accessRecorder, final LifecarePaymentApi paymentApi) {
		this.errandService = errandService;
		this.accessRecorder = accessRecorder;
		this.paymentApi = paymentApi;
	}

	/**
	 * Registers the utbetalning on the errand's insats and links it to the errand.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @param  request        the utbetalning
	 * @return                Lifecare's id for it, and whether the errand points at it
	 */
	public LifecarePaymentCreated register(final String municipalityId, final String namespace, final String errandId, final LifecarePaymentRequest request) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var serviceId = errand.requireServiceId();
		// A fresh underlag every time: the saldo it carries changes with each utbetalning registered.
		final var underlag = paymentApi.readPaymentForCreate(serviceId);
		accessRecorder.read(errand, TARGET_PAYEES, "Läste utbetalningsunderlag i Lifecare");

		final var body = buildPaymentCreate(underlag, request);
		// Lifecare's web app checks the person has a hushåll on the payment date before it saves; so does this.
		final var personId = text(field(underlag, "payment"), "susPersonId");
		if (!StringUtils.hasText(personId) || !paymentApi.hasHouseholdOn(personId, textOrEmpty(body, "payDate"))) {
			throw refuse("Personen har inget hushåll i Lifecare på utbetalningsdagen.");
		}

		final var duplicate = findRegisteredPayment(paymentApi.readLatestPayments(serviceId), body);
		if (duplicate.isPresent()) {
			throw Problem.valueOf(CONFLICT, "En likadan utbetalning finns redan i Lifecare (id %s): samma belopp, månad, konto och datum."
				.formatted(integer(duplicate.get(), "paymentId")));
		}

		final var lifecareId = create(serviceId, body);
		final var linked = link(errand, lifecareId);
		accessRecorder.written(errand, LifecareAccessEntry.CREATE, TARGET_PAYMENT, "Registrerade en utbetalning i Lifecare", lifecareId);
		return new LifecarePaymentCreated(lifecareId, linked);
	}

	private String create(final int serviceId, final JsonNode body) {
		final JsonNode created;
		try {
			created = paymentApi.createPayment(serviceId, body);
		} catch (final ThrowableProblem problem) {
			if (ProfessionalWebErrors.isRefusal(problem)) {
				throw problem;
			}
			throw Problem.valueOf(BAD_GATEWAY, NO_ANSWER);
		} catch (final RuntimeException _) {
			throw Problem.valueOf(BAD_GATEWAY, NO_ANSWER);
		}
		// Lifecare answered, so something may have been paid: without an id the caseworker has to look in Lifecare.
		return Optional.ofNullable(integer(created, "paymentId"))
			.map(String::valueOf)
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, NO_ANSWER));
	}

	/**
	 * Points the errand at the utbetalning. It is registered already, so a link that does not get through is logged and
	 * reported in the answer rather than raised: raising would invite the caseworker to register it again.
	 */
	private boolean link(final LifecareErrand errand, final String lifecareId) {
		try {
			errandService.linkPayment(errand, lifecareId);
			return true;
		} catch (final RuntimeException e) {
			LOG.error("Lifecare payment {} was registered but errand {} could not be linked to it ({})", lifecareId, errand.errandId(), e.getClass().getSimpleName());
			return false;
		}
	}
}
