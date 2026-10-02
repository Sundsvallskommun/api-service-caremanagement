package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPaymentDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCare;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.StringUtils.hasText;

/**
 * Reads whether a Lifecare payment concerning an application month has been effectuated for an applicant. The payment
 * itself is a manual caseworker step in Lifecare (FamilyCare exposes no payment write) — this service only reads the
 * registered payments via {@link LifecareFamilyCare}. Mirrors {@link ActualisationService}.
 */
@Service
public class PaymentStatusService {

	private final LifecareFamilyCare lifecareFamilyCareIntegration;

	public PaymentStatusService(final LifecareFamilyCare lifecareFamilyCareIntegration) {
		this.lifecareFamilyCareIntegration = lifecareFamilyCareIntegration;
	}

	/**
	 * Read the payment status for the applicant and application month. A payment counts as effectuated when it has a
	 * PayDate and its ConcernedMonth carries the application month (yyyy-MM). The query window spans the month before the
	 * application month (payments are typically made late in the preceding month) through the application month.
	 *
	 * <p>
	 * This matches <em>any</em> payment for the person and month — another errand's or an older insats's counts too. It
	 * is only the answer for a caller that names no errand; with an errand, the decided payments are verified by id
	 * through {@link #paidPaymentDates}.
	 * </p>
	 *
	 * @param  applicantPartyId the applicant's partyId
	 * @param  applicationMonth the month the payment concerns
	 * @return                  the effectuated flag and, when effectuated, the Lifecare PayDate
	 */
	public PaymentStatus read(final String municipalityId, final String applicantPartyId, final YearMonth applicationMonth) {
		final var monthKey = applicationMonth.toString();
		return payments(municipalityId, applicantPartyId, applicationMonth.minusMonths(1).atDay(1), applicationMonth.atEndOfMonth()).stream()
			.filter(payment -> hasText(payment.getPayDate()))
			.filter(payment -> hasText(payment.getConcernedMonth()) && payment.getConcernedMonth().contains(monthKey))
			.findFirst()
			.map(payment -> new PaymentStatus(true, payment.getPayDate()))
			.orElseGet(() -> new PaymentStatus(false, null));
	}

	/**
	 * The applicant's Lifecare payments in a date window that carry a PayDate, keyed on their Lifecare id. This is what
	 * lets a caller verify specific payments — the ones a decision registered — instead of any payment for the person.
	 *
	 * @return Lifecare payment id to PayDate; payments without an id or a PayDate are left out
	 */
	public Map<String, String> paidPaymentDates(final String municipalityId, final String applicantPartyId, final LocalDate from, final LocalDate to) {
		return payments(municipalityId, applicantPartyId, from, to).stream()
			.filter(payment -> payment.getId() != null && hasText(payment.getPayDate()))
			.collect(toMap(payment -> String.valueOf(payment.getId()), PersonBasedPaymentDTO::getPayDate, (first, second) -> first));
	}

	/**
	 * The applicant's Lifecare payments in a date window, as Lifecare holds them: id, the insats (service) they are
	 * registered on, the month they concern and the PayDate when there is one. Payments without an id are left out. What
	 * lets a caller find the payments of one decision when it has not been told their ids.
	 */
	public List<LifecarePayment> registeredPayments(final String municipalityId, final String applicantPartyId, final LocalDate from, final LocalDate to) {
		return payments(municipalityId, applicantPartyId, from, to).stream()
			.filter(payment -> payment.getId() != null)
			.map(payment -> new LifecarePayment(String.valueOf(payment.getId()), payment.getServiceId(), payment.getConcernedMonth(), payment.getPayDate()))
			.toList();
	}

	private List<PersonBasedPaymentDTO> payments(final String municipalityId, final String applicantPartyId, final LocalDate from, final LocalDate to) {
		return ofNullable(lifecareFamilyCareIntegration.getPayments(municipalityId, applicantPartyId, from, to))
			.map(ApiPaginationCompositePersonBasedPaymentDTO::getResult)
			.orElseGet(List::of);
	}
}
