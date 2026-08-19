package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatus;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatusService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Reads whether the manual Lifecare payment for the application month has been effectuated for the applicant.
 * caremanagement makes no payment — the caseworker does it in Lifecare; the process polls this to detect when the
 * payment is registered.
 */
@Service
@Transactional
public class FinancialAssistancePaymentService {

	private final PaymentStatusService paymentStatusService;
	private final CitizenService citizenService;

	FinancialAssistancePaymentService(final PaymentStatusService paymentStatusService, final CitizenService citizenService) {
		this.paymentStatusService = paymentStatusService;
		this.citizenService = citizenService;
	}

	/**
	 * Read whether the manual Lifecare payment for the application month has been effectuated for the applicant.
	 * caremanagement makes no payment — the caseworker does it in Lifecare; the process polls this to detect when the
	 * payment is registered. Returns the effectuated flag and, when effectuated, the Lifecare PayDate.
	 */
	public PaymentStatusResponse checkPaymentStatus(final String municipalityId, final PaymentStatusRequest request) {
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final PaymentStatus status = paymentStatusService.read(applicant, YearMonth.parse(request.getApplicationMonth()));
		return PaymentStatusResponse.create()
			.withEffectuated(status.effectuated())
			.withPaymentDate(status.paymentDate());
	}

	/** Resolve a partyId to the personnummer the Lifecare/SSBTEK pipeline needs, or 404 when the citizen is unknown. */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}
}
