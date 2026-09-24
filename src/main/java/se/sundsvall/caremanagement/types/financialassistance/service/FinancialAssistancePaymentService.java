package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.LifecarePayment;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatus;
import se.sundsvall.caremanagement.lifecare.service.PaymentStatusService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentStatusResponse;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.naturalOrder;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.service.NonRedDayCalendar.plusWorkingDays;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.FinalizeMapper.DECISION_TYPE_PAYMENT;

/**
 * Reads whether the Lifecare payments of a bifall have been effectuated. caremanagement makes no payment and stores no
 * payment data — Draken's BFF registers the payments directly in Lifecare; the process polls this to detect when
 * Lifecare has paid them, and escalates to the caseworker once the answer is {@code overdue}. Lifecare is the
 * authority: careM keeps at most the Lifecare ids of the errand's payments as references ({@code lifecarePaymentIds}),
 * and a payment counts as effectuated only when Lifecare itself reports a PayDate for it.
 */
@Service
@Transactional
public class FinancialAssistancePaymentService {

	static final String DETAIL_NO_INSATS = "Ärendets insats i Lifecare är okänd – utbetalningen kan inte kontrolleras";
	static final String DETAIL_NONE_ON_INSATS = "Ingen utbetalning för %s hittas på ärendets insats i Lifecare ännu";
	static final String DETAIL_ON_INSATS_NOT_PAID = "%d av %d utbetalningar på ärendets insats är inte utbetalda i Lifecare ännu";
	static final String DETAIL_LINKED_NOT_PAID = "%d av %d kopplade utbetalningar är inte utbetalda i Lifecare ännu";

	/** How long a bifall may wait for its payments before the process tells the caseworker (decided 2026-09-23). */
	static final int DEADLINE_WORKING_DAYS = 3;

	/**
	 * How far past today the Lifecare read of the errand's payments reaches: a payment registered for a coming pay day is
	 * dated ahead of the day the process asks.
	 */
	static final int WINDOW_MONTHS_AHEAD = 1;

	/** Working days are Swedish ones; the containers run on UTC. */
	private static final ZoneId SWEDISH_TIME = ZoneId.of("Europe/Stockholm");

	private final PaymentStatusService paymentStatusService;
	private final CitizenService citizenService;
	private final ErrandService errandService;
	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final DecisionService decisionService;
	private final LifecareServiceIdService lifecareServiceIdService;
	private final Clock clock;

	@Autowired
	FinancialAssistancePaymentService(final PaymentStatusService paymentStatusService, final CitizenService citizenService,
		final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository, final DecisionService decisionService,
		final LifecareServiceIdService lifecareServiceIdService) {
		this(paymentStatusService, citizenService, errandService, financialAssistanceRepository, decisionService, lifecareServiceIdService,
			Clock.system(SWEDISH_TIME));
	}

	FinancialAssistancePaymentService(final PaymentStatusService paymentStatusService, final CitizenService citizenService,
		final ErrandService errandService, final FinancialAssistanceRepository financialAssistanceRepository, final DecisionService decisionService,
		final LifecareServiceIdService lifecareServiceIdService, final Clock clock) {
		this.paymentStatusService = paymentStatusService;
		this.citizenService = citizenService;
		this.errandService = errandService;
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.decisionService = decisionService;
		this.lifecareServiceIdService = lifecareServiceIdService;
		this.clock = clock;
	}

	/**
	 * Read whether the Lifecare payments of a bifall have been effectuated.
	 *
	 * <p>
	 * With an {@code errandId}, the check is always errand-specific, in the first way that applies:
	 * </p>
	 * <ol>
	 * <li>the errand carries {@code lifecarePaymentIds}: exactly those ids must be reported paid by Lifecare
	 * ({@link #checkLinkedPayments});</li>
	 * <li>otherwise — Draken registers the payments in Lifecare without telling careM their ids — the payments are found
	 * in Lifecare on the errand's own insats for the application month, leaving out every id another errand references
	 * ({@link #checkPaymentsOnTheInsats}). Once they are all paid they are linked to the errand, so from then on they are
	 * this decision's and can never close another errand.</li>
	 * </ol>
	 *
	 * <p>
	 * A not-effectuated answer also says whether it is {@code overdue}: the deadline is {@value #DEADLINE_WORKING_DAYS}
	 * working days after the day of the decision, and it is overdue from the day after. Nothing is overdue before that — a
	 * bifall whose payments careM has not seen yet is given the same days as any other. The status is never closed on a
	 * deadline; {@code overdue} only tells the process to raise it with a person.
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

		final var errandId = request.getErrandId();
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var entity = financialAssistanceRepository.findByErrandId(errandId).orElse(null);
		final var linked = ofNullable(entity).map(FinancialAssistanceEntity::getLifecarePaymentIds).orElseGet(List::of);
		if (!linked.isEmpty()) {
			return checkLinkedPayments(municipalityId, namespace, request, applicationMonth, List.copyOf(linked));
		}
		return checkPaymentsOnTheInsats(municipalityId, namespace, request, applicationMonth, entity);
	}

	/**
	 * The payments Draken registered in Lifecare without linking their ids: the applicant's Lifecare payments on the
	 * errand's insats that concern the application month, less every id another errand references (its
	 * {@code lifecarePaymentIds}). Effectuated when there is at
	 * least one and Lifecare reports a PayDate for each; they are then linked to the errand, which turns every later
	 * check into {@link #checkLinkedPayments} and keeps them from ever counting for another errand. The deadline counts
	 * from the day of the errand's {@code PAYMENT} decision.
	 */
	private PaymentStatusResponse checkPaymentsOnTheInsats(final String municipalityId, final String namespace, final PaymentStatusRequest request,
		final YearMonth applicationMonth, final FinancialAssistanceEntity entity) {

		final var errandId = request.getErrandId();
		final var deadline = plusWorkingDays(decisionDay(municipalityId, namespace, errandId), DEADLINE_WORKING_DAYS);
		final var today = LocalDate.now(clock);
		final var overdue = today.isAfter(deadline);

		final var serviceId = lifecareServiceIdService.currentOrResolve(municipalityId, namespace, errandId);
		if (entity == null || serviceId == null) {
			return notEffectuated(DETAIL_NO_INSATS).withDeadline(deadline.toString()).withOverdue(overdue);
		}

		final var monthKey = applicationMonth.toString();
		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var onTheInsats = paymentStatusService.registeredPayments(municipalityId, applicant, applicationMonth.minusMonths(1).atDay(1),
			latest(applicationMonth.atEndOfMonth(), today.plusMonths(WINDOW_MONTHS_AHEAD))).stream()
			.filter(payment -> serviceId.equals(payment.serviceId()))
			.filter(payment -> hasText(payment.concernedMonth()) && payment.concernedMonth().contains(monthKey))
			.toList();
		if (onTheInsats.isEmpty()) {
			return notEffectuated(DETAIL_NONE_ON_INSATS.formatted(monthKey)).withDeadline(deadline.toString()).withOverdue(overdue);
		}
		final var ids = onTheInsats.stream().map(LifecarePayment::id).collect(toCollection(LinkedHashSet::new));
		final var taken = new HashSet<>(financialAssistanceRepository.findLifecarePaymentIdsLinkedElsewhere(ids, errandId));
		final var own = onTheInsats.stream()
			.filter(payment -> !taken.contains(payment.id()))
			.toList();

		if (own.isEmpty()) {
			return notEffectuated(DETAIL_NONE_ON_INSATS.formatted(monthKey)).withDeadline(deadline.toString()).withOverdue(overdue);
		}
		final var unpaid = own.stream()
			.filter(payment -> !hasText(payment.payDate()))
			.count();
		if (unpaid > 0) {
			return notEffectuated(DETAIL_ON_INSATS_NOT_PAID.formatted(unpaid, own.size())).withDeadline(deadline.toString()).withOverdue(overdue);
		}

		// The insats id is set on the entity too: the lookup above may have stored it in a transaction of its own, and
		// this save must not write the stale null back over it.
		entity.setLifecareServiceId(serviceId);
		entity.setLifecarePaymentIds(new ArrayList<>(own.stream().map(LifecarePayment::id).distinct().toList()));
		financialAssistanceRepository.save(entity);

		return PaymentStatusResponse.create()
			.withEffectuated(true)
			.withDeadline(deadline.toString())
			.withOverdue(false)
			.withPaymentDate(own.stream()
				.map(LifecarePayment::payDate)
				.max(naturalOrder())
				.orElse(null));
	}

	/**
	 * The payments Draken registered in Lifecare and linked to the errand, verified by their Lifecare ids. The deadline
	 * counts from the day of the errand's {@code PAYMENT} decision.
	 */
	private PaymentStatusResponse checkLinkedPayments(final String municipalityId, final String namespace, final PaymentStatusRequest request,
		final YearMonth applicationMonth, final List<String> linked) {

		final var deadline = plusWorkingDays(decisionDay(municipalityId, namespace, request.getErrandId()), DEADLINE_WORKING_DAYS);
		final var today = LocalDate.now(clock);
		final var overdue = today.isAfter(deadline);

		final var applicant = personalNumber(municipalityId, request.getApplicant());
		final var paid = paymentStatusService.paidPaymentDates(municipalityId, applicant, applicationMonth.minusMonths(1).atDay(1),
			latest(applicationMonth.atEndOfMonth(), today.plusMonths(WINDOW_MONTHS_AHEAD)));
		final var unpaid = linked.stream()
			.filter(lifecarePaymentId -> !paid.containsKey(lifecarePaymentId))
			.count();
		if (unpaid > 0) {
			return notEffectuated(DETAIL_LINKED_NOT_PAID.formatted(unpaid, linked.size())).withDeadline(deadline.toString()).withOverdue(overdue);
		}

		return PaymentStatusResponse.create()
			.withEffectuated(true)
			.withDeadline(deadline.toString())
			.withOverdue(false)
			.withPaymentDate(linked.stream()
				.map(paid::get)
				.max(naturalOrder())
				.orElse(null));
	}

	/**
	 * The day the errand was decided: its {@code PAYMENT} decision's date, else the day that decision was recorded, else
	 * today — so a missing date can only postpone, never trigger, an escalation.
	 */
	private LocalDate decisionDay(final String municipalityId, final String namespace, final String errandId) {
		return decisionService.readAll(municipalityId, namespace, errandId).stream()
			.filter(decision -> DECISION_TYPE_PAYMENT.equals(decision.getDecisionType()))
			.map(FinancialAssistancePaymentService::dayOf)
			.filter(Objects::nonNull)
			.min(naturalOrder())
			.orElseGet(() -> LocalDate.now(clock));
	}

	private static LocalDate dayOf(final Decision decision) {
		return ofNullable(decision.getDecisionDate())
			.orElseGet(() -> ofNullable(decision.getCreated())
				.map(created -> created.atZoneSameInstant(SWEDISH_TIME).toLocalDate())
				.orElse(null));
	}

	private static LocalDate latest(final LocalDate first, final LocalDate second) {
		if (second.isAfter(first)) {
			return second;
		}
		return first;
	}

	private static PaymentStatusResponse notEffectuated(final String detail) {
		return PaymentStatusResponse.create()
			.withEffectuated(false)
			.withDetail(detail);
	}

	/** Resolve a partyId to the personnummer the Lifecare/SSBTEK pipeline needs, or 404 when the citizen is unknown. */
	private String personalNumber(final String municipalityId, final String partyId) {
		return citizenService.getPersonalNumber(municipalityId, partyId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No citizen found for partyId " + partyId));
	}
}
