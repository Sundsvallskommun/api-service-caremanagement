package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatus;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatusService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.naturalOrder;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.service.NonRedDayCalendar.plusWorkingDays;
import static se.sundsvall.caremanagement.types.financialassistance.service.PaymentService.SOURCE_CASEWORKER;
import static se.sundsvall.caremanagement.types.financialassistance.service.PaymentService.STATUS_DRAFT;
import static se.sundsvall.caremanagement.types.financialassistance.service.PaymentService.STATUS_REGISTERED;

/**
 * Reads whether the Lifecare payments of a bifall have been effectuated. caremanagement makes no payment — Draken's BFF
 * registers them in Lifecare and reports each one's Lifecare id back; the process polls this to detect when they are
 * all there, and escalates to the caseworker once the answer is {@code overdue}.
 */
@Service
@Transactional
public class FinancialAssistancePaymentService {

	static final String DETAIL_NO_DECIDED_PAYMENTS = "Ärendet har inga beslutade utbetalningar";
	static final String DETAIL_NOT_REGISTERED = "%d av %d beslutade utbetalningar är inte registrerade i Lifecare";
	static final String DETAIL_NOT_FOUND_IN_LIFECARE = "%d av %d registrerade utbetalningar hittas inte som utbetalda i Lifecare";

	/** How long a bifall may wait for its payments before the process tells the caseworker (decided 2026-09-23). */
	static final int DEADLINE_WORKING_DAYS = 3;

	/** Working days are Swedish ones; the containers run on UTC. */
	private static final ZoneId SWEDISH_TIME = ZoneId.of("Europe/Stockholm");

	private final PaymentStatusService paymentStatusService;
	private final PaymentService paymentService;
	private final CitizenService citizenService;
	private final Clock clock;

	@Autowired
	FinancialAssistancePaymentService(final PaymentStatusService paymentStatusService, final PaymentService paymentService, final CitizenService citizenService) {
		this(paymentStatusService, paymentService, citizenService, Clock.system(SWEDISH_TIME));
	}

	FinancialAssistancePaymentService(final PaymentStatusService paymentStatusService, final PaymentService paymentService, final CitizenService citizenService,
		final Clock clock) {
		this.paymentStatusService = paymentStatusService;
		this.paymentService = paymentService;
		this.citizenService = citizenService;
		this.clock = clock;
	}

	/**
	 * Read whether the Lifecare payments of a bifall have been effectuated.
	 *
	 * <p>
	 * With an {@code errandId}, the decided payments are the errand's own caseworker payments past {@code DRAFT} — the
	 * rows {@code finalize} created. The status is effectuated only when every one of them is {@code REGISTERED} with a
	 * Lifecare id <em>and</em> that id is among the applicant's paid Lifecare payments. Another errand's payment, or a
	 * second payment for the same month, can therefore never close this errand. The Lifecare window stretches to cover
	 * the decided payment dates, so a payment dated outside the month around the application month is still found.
	 * </p>
	 *
	 * <p>
	 * A not-effectuated answer also says whether it is {@code overdue}: the deadline is {@value #DEADLINE_WORKING_DAYS}
	 * working days after the day finalize created the payments, and it is overdue from the day after. A bifall with no
	 * decided payments at all is overdue at once — waiting cannot resolve it. The status is never closed on a deadline;
	 * {@code overdue} only tells the process to raise it with a person.
	 * </p>
	 *
	 * <p>
	 * Without an {@code errandId} the answer is the old person-and-month read, for a caller that predates the field. It
	 * has no deadline.
	 * </p>
	 */
	public PaymentStatusResponse checkPaymentStatus(final String municipalityId, final String namespace, final PaymentStatusRequest request) {
		final var applicationMonth = YearMonth.parse(request.getApplicationMonth());
		if (!hasText(request.getErrandId())) {
			final var applicant = personalNumber(municipalityId, request.getApplicant());
			final PaymentStatus status = paymentStatusService.read(municipalityId, applicant, applicationMonth);
			return PaymentStatusResponse.create()
				.withEffectuated(status.effectuated())
				.withPaymentDate(status.paymentDate());
		}

		final var decided = paymentService.list(municipalityId, namespace, request.getErrandId()).stream()
			.filter(payment -> SOURCE_CASEWORKER.equals(payment.getSource()))
			.filter(payment -> !STATUS_DRAFT.equals(payment.getStatus()))
			.toList();
		if (decided.isEmpty()) {
			return notEffectuated(DETAIL_NO_DECIDED_PAYMENTS).withOverdue(true);
		}
		final var deadline = deadline(decided);
		final var overdue = LocalDate.now(clock).isAfter(deadline);

		final var unregistered = decided.stream()
			.filter(payment -> !STATUS_REGISTERED.equals(payment.getStatus()) || !hasText(payment.getLifecareId()))
			.count();
		if (unregistered > 0) {
			return notEffectuated(DETAIL_NOT_REGISTERED.formatted(unregistered, decided.size())).withDeadline(deadline.toString()).withOverdue(overdue);
		}

		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var paid = paymentStatusService.paidPaymentDates(municipalityId, applicant, windowStart(applicationMonth, decided), windowEnd(applicationMonth, decided));
		final var missing = decided.stream()
			.filter(payment -> !paid.containsKey(payment.getLifecareId()))
			.count();
		if (missing > 0) {
			return notEffectuated(DETAIL_NOT_FOUND_IN_LIFECARE.formatted(missing, decided.size())).withDeadline(deadline.toString()).withOverdue(overdue);
		}

		return PaymentStatusResponse.create()
			.withEffectuated(true)
			.withDeadline(deadline.toString())
			.withOverdue(false)
			.withPaymentDate(decided.stream()
				.map(payment -> paid.get(payment.getLifecareId()))
				.max(naturalOrder())
				.orElse(null));
	}

	private static PaymentStatusResponse notEffectuated(final String detail) {
		return PaymentStatusResponse.create()
			.withEffectuated(false)
			.withDetail(detail);
	}

	/**
	 * The last working day the payments may wait: {@value #DEADLINE_WORKING_DAYS} working days after the day the first of
	 * them was created, which is the day finalize ran. A row without a timestamp counts from today, so it can only
	 * postpone, never trigger, an escalation.
	 */
	private LocalDate deadline(final List<Payment> decided) {
		final var decidedOn = decided.stream()
			.map(Payment::getCreated)
			.filter(Objects::nonNull)
			.map(created -> created.atZoneSameInstant(SWEDISH_TIME).toLocalDate())
			.min(naturalOrder())
			.orElseGet(() -> LocalDate.now(clock));
		return plusWorkingDays(decidedOn, DEADLINE_WORKING_DAYS);
	}

	/** The month before the application month, or the earliest decided payment date when that is earlier. */
	private static LocalDate windowStart(final YearMonth applicationMonth, final List<Payment> decided) {
		final var monthStart = applicationMonth.minusMonths(1).atDay(1);
		return decided.stream()
			.map(Payment::getPaymentDate)
			.filter(Objects::nonNull)
			.filter(date -> date.isBefore(monthStart))
			.min(naturalOrder())
			.orElse(monthStart);
	}

	/** The end of the application month, or the latest decided payment date when that is later. */
	private static LocalDate windowEnd(final YearMonth applicationMonth, final List<Payment> decided) {
		final var monthEnd = applicationMonth.atEndOfMonth();
		return decided.stream()
			.map(Payment::getPaymentDate)
			.filter(Objects::nonNull)
			.filter(date -> date.isAfter(monthEnd))
			.max(naturalOrder())
			.orElse(monthEnd);
	}

	/** Resolve a partyId to the personnummer the Lifecare/SSBTEK pipeline needs, or 404 when the citizen is unknown. */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}
}
