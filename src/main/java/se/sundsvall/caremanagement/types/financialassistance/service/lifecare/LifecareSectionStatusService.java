package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.LinkedHashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareSectionStatus;

import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareDecisionClient.SERVICE_BUSINESS_TYPE;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.elements;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.isTrue;

/**
 * Which of the errand's Normberäkning, Beslut and Utbetalning are done, as Lifecare has them: the checks on the tabs,
 * in place of careM's section approvals. Normberäkning is done once its beräkning is slutlig, Beslut once the beslut is
 * saved in Lifecare, and Utbetalning once an utbetalning for the errand's month is registered there. Each is read on
 * its own: one Lifecare read failing leaves that check off, not the others.
 */
@Service
public class LifecareSectionStatusService {

	static final String PATH_LIST_CALCULATIONS = "api2/Calculation/ListCalculations";
	static final String PATH_LATEST_PAYMENTS = "api2/Payment/GetLatestPayments";

	private static final Logger LOG = LoggerFactory.getLogger(LifecareSectionStatusService.class);
	private static final String BUSINESS_TYPE = "businessType";
	private static final String BUSINESS_ID = "businessId";

	private final LifecareErrandService errandService;
	private final ProfessionalWebClient client;
	private final LifecareAccessRecorder accessRecorder;

	LifecareSectionStatusService(final LifecareErrandService errandService, final ProfessionalWebClient client, final LifecareAccessRecorder accessRecorder) {
		this.errandService = errandService;
		this.client = client;
		this.accessRecorder = accessRecorder;
	}

	/**
	 * The errand's section checks.
	 *
	 * @param  municipalityId the municipality
	 * @param  namespace      the namespace
	 * @param  errandId       the errand
	 * @return                the checks
	 */
	public LifecareSectionStatus read(final String municipalityId, final String namespace, final String errandId) {
		final var errand = errandService.load(municipalityId, namespace, errandId);
		return new LifecareSectionStatus(calculationFinalized(errand), errand.decision().isPresent(), paymentRegistered(errand));
	}

	/** Whether the errand's beräkning is slutlig, from the insats's list: one Lifecare read without any rows. */
	private boolean calculationFinalized(final LifecareErrand errand) {
		if (errand.serviceId() == null || errand.calculationId() == null) {
			return false;
		}
		try {
			final var params = new LinkedHashMap<String, String>();
			params.put(BUSINESS_TYPE, SERVICE_BUSINESS_TYPE);
			params.put(BUSINESS_ID, String.valueOf(errand.serviceId()));
			params.put("investigationId", "0");
			params.put("serviceId", String.valueOf(errand.serviceId()));
			params.put("onlylatest", "true");
			final var listed = client.get(PATH_LIST_CALCULATIONS, params);
			accessRecorder.read(errand, "CALCULATION", "Läste insatsens normberäkningar i Lifecare");
			return elements(listed).stream()
				.anyMatch(calculation -> Objects.equals(integer(calculation.path("calculationId")).orElse(null), errand.calculationId())
					&& isTrue(calculation.path("isFinalized")));
		} catch (final RuntimeException e) {
			LOG.warn("Could not read whether the beräkning of errand {} is slutlig in Lifecare ({})", errand.errandId(), e.getClass().getSimpleName());
			return false;
		}
	}

	/**
	 * Whether an utbetalning for the errand's month is registered on the insats: one concerning that month that is not
	 * makulerad. False when the errand has no month or insats, or Lifecare could not be read.
	 */
	private boolean paymentRegistered(final LifecareErrand errand) {
		if (errand.periodYear() == null || errand.periodMonth() == null || errand.serviceId() == null) {
			return false;
		}
		final var month = "%04d%02d".formatted(errand.periodYear(), errand.periodMonth());
		try {
			final var params = new LinkedHashMap<String, String>();
			params.put(BUSINESS_TYPE, SERVICE_BUSINESS_TYPE);
			params.put(BUSINESS_ID, String.valueOf(errand.serviceId()));
			final var registered = client.get(PATH_LATEST_PAYMENTS, params);
			accessRecorder.read(errand, "PAYMENTS", "Läste utbetalningar i Lifecare");
			return elements(registered).stream()
				.filter(payment -> payment.path("concernedMonth").isString() && month.equals(payment.path("concernedMonth").stringValue()))
				// Lifecare leaves cancellationDate empty until the utbetalning is makulerad.
				.anyMatch(payment -> payment.path("cancellationDate").isString() && payment.path("cancellationDate").stringValue().isEmpty());
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the utbetalningar of errand {} in Lifecare ({})", errand.errandId(), e.getClass().getSimpleName());
			return false;
		}
	}
}
