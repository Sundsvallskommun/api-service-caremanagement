package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.eventlog.spi.LifecareAccessEntry;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentOptions;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentStatus;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRegisteredPayment;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentBodies.buildPayeeCreate;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentBodies.findMatchingPayee;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentMapper.toPayee;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentMapper.toPaymentOptions;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentMapper.toPaymentStatus;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentMapper.toRegisteredPayments;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.array;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.field;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.flag;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.text;

/**
 * The utbetalningar, betalsätt and betalningsmottagare of an errand's insats, read and written in Lifecare, the
 * register of record for all of them. careM keeps none of it; every call lands in the errand's access log.
 */
@Service
public class ErrandLifecarePaymentService {

	static final String TARGET_PAYEES = "PAYEES";
	static final String TARGET_PAYEE = "PAYEE";
	static final String TARGET_PAYMENTS = "PAYMENTS";

	private final LifecareErrandService errandService;
	private final LifecareAccessRecorder accessRecorder;
	private final LifecarePaymentApi paymentApi;

	ErrandLifecarePaymentService(final LifecareErrandService errandService, final LifecareAccessRecorder accessRecorder, final LifecarePaymentApi paymentApi) {
		this.errandService = errandService;
		this.accessRecorder = accessRecorder;
		this.paymentApi = paymentApi;
	}

	/**
	 * Everything the utbetalning form needs: betalsätt, payees, konteringsrader, saldon, the months it may concern, and a
	 * proposal to start from.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the options
	 */
	public LifecarePaymentOptions paymentOptions(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var serviceId = errand.requireServiceId();
		final var underlag = paymentApi.readPaymentForCreate(serviceId);
		final var registered = paymentApi.readLatestPayments(serviceId);
		accessRecorder.read(errand, TARGET_PAYEES, "Läste utbetalningsunderlag i Lifecare");
		return toPaymentOptions(underlag, registered);
	}

	/**
	 * Whether the utbetalning for the errand's application month has been registered. Unavailable, rather than failing,
	 * when the errand has no application month, or the insats or Lifecare cannot be read: Lifecare being unreachable is
	 * a normal state for a status read.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the status
	 */
	public LifecarePaymentStatus paymentStatus(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		if (errand.periodYear() == null || errand.periodMonth() == null || errand.periodYear() == 0 || errand.periodMonth() == 0) {
			return new LifecarePaymentStatus(null, false, null, null, null, true);
		}
		final var applicationMonth = "%d-%02d".formatted(errand.periodYear(), errand.periodMonth());
		try {
			final var registered = paymentApi.readLatestPayments(errand.requireServiceId());
			accessRecorder.read(errand, TARGET_PAYMENTS, "Läste utbetalningar i Lifecare");
			return toPaymentStatus(registered, applicationMonth);
		} catch (final RuntimeException _) {
			return new LifecarePaymentStatus(applicationMonth, false, null, null, null, true);
		}
	}

	/**
	 * The utbetalningar registered on the insats: what has actually been paid, or is on its way.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the utbetalningar, newest payment date first
	 */
	public List<LifecareRegisteredPayment> registeredPayments(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var registered = paymentApi.readLatestPayments(errand.requireServiceId());
		accessRecorder.read(errand, TARGET_PAYMENTS, "Läste utbetalningar i Lifecare");
		return toRegisteredPayments(registered);
	}

	/**
	 * Adds a betalningsmottagare for the person the insats belongs to. Payee/Create is not idempotent, so a payee
	 * already paying to the same account and clearing with the same betalsätt is returned instead of being created
	 * twice. The personnummer the payee is filed under is taken from Lifecare's own underlag.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @param  request        the payee
	 * @return                the created, or already existing, payee
	 */
	public LifecarePayee createPayee(final String municipalityId, final String namespace, final String errandId, final LifecarePayeeRequest request) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		final var underlag = paymentApi.readPaymentForCreate(errand.requireServiceId());
		accessRecorder.read(errand, TARGET_PAYEES, "Läste betalningsmottagare i Lifecare");

		final var methods = array(underlag, "paymentMethods");
		final var offered = methods.stream()
			.anyMatch(method -> flag(method, "inUse") && request.paymentMethod().equals(integer(method, "paymentCode")));
		if (!offered) {
			throw Problem.valueOf(BAD_REQUEST, "Betalsättet finns inte på insatsen i Lifecare");
		}
		final var existing = findMatchingPayee(array(underlag, "payees"), request);
		if (existing.isPresent()) {
			return toPayee(existing.get(), methods);
		}
		final var personId = text(field(underlag, "payment"), "susPersonId");
		if (!StringUtils.hasText(personId)) {
			throw Problem.valueOf(BAD_GATEWAY, "Lifecare angav ingen person för insatsen");
		}

		final var created = paymentApi.createPayee(buildPayeeCreate(personId, request));
		accessRecorder.written(errand, LifecareAccessEntry.CREATE, TARGET_PAYEE, "Lade till en betalningsmottagare i Lifecare", String.valueOf(integer(created, "payeeId")));
		return toPayee(created, methods);
	}
}
