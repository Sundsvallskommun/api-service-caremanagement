package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedPaymentDTO;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

/**
 * Reads whether a Lifecare payment concerning an application month has been effectuated for an applicant. The
 * payment itself is a manual caseworker step in Lifecare (FamilyCare exposes no payment write) — this service only
 * reads the
 * registered payments via {@link LifecareFamilyCareIntegration}. Mirrors {@link ActualisationService}.
 */
@Service
public class PaymentStatusService {

	private final LifecareFamilyCareIntegration lifecareFamilyCareIntegration;

	public PaymentStatusService(final LifecareFamilyCareIntegration lifecareFamilyCareIntegration) {
		this.lifecareFamilyCareIntegration = lifecareFamilyCareIntegration;
	}

	/**
	 * Read the payment status for the applicant and application month. A payment counts as effectuated when it has a
	 * PayDate and its ConcernedMonth carries the application month (yyyy-MM). The query window spans the month before the
	 * application month (payments are typically made late in the preceding month) through the application month.
	 *
	 * @param  applicantPersonId the applicant's personal identity number
	 * @param  applicationMonth  the month the payment concerns
	 * @return                   the effectuated flag and, when effectuated, the Lifecare PayDate
	 */
	public PaymentStatus read(final String applicantPersonId, final YearMonth applicationMonth) {
		final var from = applicationMonth.minusMonths(1).atDay(1).format(ISO_LOCAL_DATE);
		final var to = applicationMonth.atEndOfMonth().format(ISO_LOCAL_DATE);

		final var payments = ofNullable(lifecareFamilyCareIntegration.getPayments(applicantPersonId, from, to))
			.map(ApiPaginationCompositePersonBasedPaymentDTO::getResult)
			.orElseGet(List::of);

		final var monthKey = applicationMonth.toString();
		return payments.stream()
			.filter(payment -> hasText(payment.getPayDate()))
			.filter(payment -> hasText(payment.getConcernedMonth()) && payment.getConcernedMonth().contains(monthKey))
			.findFirst()
			.map(payment -> new PaymentStatus(true, payment.getPayDate()))
			.orElseGet(() -> new PaymentStatus(false, null));
	}
}
