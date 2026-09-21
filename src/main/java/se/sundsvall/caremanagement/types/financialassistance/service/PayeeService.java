package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.PaymentView;
import se.sundsvall.caremanagement.rpa.service.RpaService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeLifecareResult;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeOption;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaPayeeRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPayeeEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.PayeeMapper;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Comparator.reverseOrder;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.rpa.service.RpaAction.ADD_PAYEE;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.PayeeMapper.toPayeeOption;

/**
 * The selectable betalningsmottagare for an errand's payment form.
 *
 * <p>
 * Verksamheten's rule is to pick an <em>existing</em> payee with pre-filled data rather than to fill in an account per
 * betalsätt. FamilyCare exposes no payee register to pick from, so the list is derived from the applicant's actual
 * Lifecare payments in the last 12 months — a past payment is the only evidence a payee exists — and topped up with
 * the payees the caseworker has added by hand on this errand.
 * </p>
 *
 * <p>
 * This read is deliberately separate from the payment proposal, which carries the same options today. The proposal
 * reconciles and <em>persists</em> the PAYMENT-section warnings on every read and 404s when the errand has no
 * calculation draft; neither is acceptable for filling a dropdown. Nothing here writes anything on a read.
 * </p>
 *
 * <p>
 * A manually added payee is a bridging state, not a second register: {@code POST} stores it so it is selectable now and
 * queues the {@code ADD_PAYEE} robot to put it into Lifecare, after which the ordinary payment history carries it. When
 * that has happened the manual row and its Lifecare twin collapse into one option, so the list does not grow a
 * duplicate.
 * </p>
 */
@Service
public class PayeeService {

	private static final Logger LOG = LoggerFactory.getLogger(PayeeService.class);

	/** How far back the Lifecare payment history is read for payee options — the same window the payment proposal uses. */
	static final int PAYEE_LOOKBACK_MONTHS = 12;

	static final String LIFECARE_STATUS_PENDING = "PENDING";
	static final String LIFECARE_STATUS_SYNCED = "SYNCED";
	static final String LIFECARE_STATUS_FAILED = "FAILED";

	static final String OUTCOME_ADDED = "ADDED";
	static final String OUTCOME_ALREADY_EXISTS = "ALREADY_EXISTS";
	static final String OUTCOME_FAILED = "FAILED";

	static final String KEY_PAYEE_ID = "payeeId";

	static final String ERROR_DETAIL_REQUIRED = "detail is required when outcome is FAILED — it is Lifecare's own message, shown to the caseworker";
	static final String ERROR_ALREADY_SYNCED = "Payee is already SYNCED in Lifecare and cannot be reported as FAILED";

	static final String WARNING_PAYEE_PENDING = "Betalningsmottagaren \"%s\" är inte upplagd i Lifecare ännu – utbetalningen kan inte registreras förrän roboten har lagt upp den.";
	static final String WARNING_PAYEE_FAILED = "Betalningsmottagaren \"%s\" kunde inte läggas upp i Lifecare: %s";

	private final ErrandService errandService;
	private final HouseholdPartyService householdPartyService;
	private final LifecareCaseHistoryService lifecareCaseHistoryService;
	private final FaPayeeRepository payeeRepository;
	private final RpaService rpaService;

	PayeeService(final ErrandService errandService, final HouseholdPartyService householdPartyService,
		final LifecareCaseHistoryService lifecareCaseHistoryService, final FaPayeeRepository payeeRepository, final RpaService rpaService) {
		this.errandService = errandService;
		this.householdPartyService = householdPartyService;
		this.lifecareCaseHistoryService = lifecareCaseHistoryService;
		this.payeeRepository = payeeRepository;
		this.rpaService = rpaService;
	}

	/**
	 * Every payee the caseworker can pick for this errand: the ones seen on the applicant's Lifecare payments in the
	 * lookback window, most recently paid first, followed by the manually added ones that are not already among them.
	 * Scoped: throws {@code 404} when the errand is missing here. The Lifecare read is best-effort — an outage degrades
	 * the list to the manual rows rather than failing the payment form.
	 */
	@Transactional(readOnly = true)
	public List<PayeeOption> list(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)

		final var lifecareOptions = lifecareOptions(municipalityId, namespace, errandId);
		final var seen = new LinkedHashSet<PayeeMapper.PayeeKey>(lifecareOptions.stream().map(PayeeMapper::key).toList());

		final var options = new ArrayList<>(lifecareOptions);
		manualEntities(errandId).stream()
			.map(PayeeMapper::toPayeeOption)
			.filter(option -> seen.add(PayeeMapper.key(option)))
			.forEach(options::add);
		return options;
	}

	/**
	 * A single payee on an errand — the robot's second call. Scoped: throws {@code 404} when the errand or payee is
	 * missing.
	 */
	@Transactional(readOnly = true)
	public PayeeOption get(final String municipalityId, final String namespace, final String errandId, final String payeeId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return toPayeeOption(requirePayee(errandId, payeeId));
	}

	/**
	 * Add a payee by hand and queue the robot that puts it into Lifecare. An identical payee already on the errand is
	 * reused rather than duplicated, so a double submit does not put the same account in the dropdown twice.
	 *
	 * <p>
	 * The enqueue is guarded: a queue outage must not lose the caseworker's payee. The row stays {@code PENDING}, which
	 * is exactly what it would be while waiting for the robot anyway, and re-posting the same payee queues it again.
	 * </p>
	 */
	@Transactional
	public PayeeOption create(final String municipalityId, final String namespace, final String errandId, final PayeeRequest request) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)

		final var existing = findSameAccount(errandId, request);
		if (existing.isPresent()) {
			return toPayeeOption(existing.get());
		}

		final var saved = payeeRepository.save(PayeeMapper.applyRequest(FaPayeeEntity.create(), request)
			.withErrandId(errandId)
			.withLifecareStatus(LIFECARE_STATUS_PENDING));

		enqueueAddPayee(municipalityId, namespace, errandId, saved.getId());
		return toPayeeOption(saved);
	}

	/**
	 * Record the {@code ADD_PAYEE} robot's report. {@code ALREADY_EXISTS} counts as success — the robot is told not to
	 * create a duplicate, and the caseworker's intent is satisfied either way. Re-posting the same outcome is
	 * idempotent; reporting {@code FAILED} on a payee already {@code SYNCED} is a {@code 409}, because that would
	 * silently un-sync a payee a payment may already be pointing at.
	 */
	@Transactional
	public PayeeOption recordLifecareResult(final String municipalityId, final String namespace, final String errandId, final String payeeId,
		final PayeeLifecareResult result) {

		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var entity = requirePayee(errandId, payeeId);

		if (OUTCOME_FAILED.equals(result.getOutcome())) {
			if (!hasText(result.getDetail())) {
				throw Problem.valueOf(BAD_REQUEST, ERROR_DETAIL_REQUIRED);
			}
			if (LIFECARE_STATUS_SYNCED.equals(entity.getLifecareStatus())) {
				throw Problem.valueOf(CONFLICT, ERROR_ALREADY_SYNCED);
			}
			return toPayeeOption(payeeRepository.save(entity
				.withLifecareStatus(LIFECARE_STATUS_FAILED)
				.withLifecareDetail(result.getDetail())));
		}

		return toPayeeOption(payeeRepository.save(entity
			.withLifecareStatus(LIFECARE_STATUS_SYNCED)
			.withLifecarePayeeId(result.getLifecarePayeeId())
			.withLifecareDetail(null)));
	}

	/** Remove a manually added payee. Scoped: throws {@code 404} when the errand or payee is missing here. */
	@Transactional
	public void delete(final String municipalityId, final String namespace, final String errandId, final String payeeId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		payeeRepository.delete(requirePayee(errandId, payeeId));
	}

	/**
	 * The warnings finalize surfaces for the payees a decision actually pays to: a payment cannot be registered in
	 * Lifecare against a payee the robot has not managed to create there.
	 *
	 * <p>
	 * Matched per payee rather than per errand on purpose — a payee the caseworker added and then did not use must not
	 * warn about a payment it has nothing to do with. A payee that is not among the errand's manual rows at all came
	 * from the Lifecare payment history and is in Lifecare by definition, so it never warns.
	 * </p>
	 *
	 * <p>
	 * Returns text, and finalize does not fail on it: the decision is the caseworker's, the payment rows and the queue
	 * items are created either way, and a robot that cannot find the payee reports that on its own queue item. Blocking
	 * here would strand a decision that is otherwise complete.
	 * </p>
	 */
	@Transactional(readOnly = true)
	public List<String> unsyncedPayeeWarnings(final String errandId, final List<Payee> payees) {
		final var unsynced = manualEntities(errandId).stream()
			.filter(entity -> !LIFECARE_STATUS_SYNCED.equals(entity.getLifecareStatus()))
			.collect(toMap(entity -> PayeeMapper.key(toPayeeOption(entity)), identity(), (first, _) -> first, LinkedHashMap::new));

		return ofNullable(payees).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.map(PayeeMapper::key)
			.distinct()
			.map(unsynced::get)
			.filter(Objects::nonNull)
			.map(PayeeService::warningText)
			.toList();
	}

	/** Lifecare's own message when the robot failed, otherwise "not created yet" — both are actionable, differently. */
	private static String warningText(final FaPayeeEntity entity) {
		final var name = ofNullable(entity.getName()).orElse("");
		if (LIFECARE_STATUS_FAILED.equals(entity.getLifecareStatus())) {
			return WARNING_PAYEE_FAILED.formatted(name, ofNullable(entity.getLifecareDetail()).orElse(""));
		}
		return WARNING_PAYEE_PENDING.formatted(name);
	}

	private List<FaPayeeEntity> manualEntities(final String errandId) {
		return payeeRepository.findByErrandId(errandId).stream()
			.sorted(comparing(FaPayeeEntity::getCreated, nullsLast(naturalOrder())))
			.toList();
	}

	/** The payees seen on the applicant's Lifecare payments in the lookback window, most recently paid first, distinct. */
	private List<PayeeOption> lifecareOptions(final String municipalityId, final String namespace, final String errandId) {
		final var applicant = householdPartyService.household(municipalityId, namespace, errandId).applicantPersonalNumber();
		if (applicant.isEmpty()) {
			return List.of();
		}

		final var today = LocalDate.now();
		final List<PaymentView> payments;
		try {
			payments = lifecareCaseHistoryService.listPayments(applicant.get(), today.minusMonths(PAYEE_LOOKBACK_MONTHS), today);
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the applicant's Lifecare payments — the payee list falls back to the manually added ones", e);
			return List.of();
		}

		final var seen = new LinkedHashSet<PayeeMapper.PayeeKey>();
		return payments.stream()
			.sorted(comparing(PaymentView::payDate, nullsLast(reverseOrder())))
			.map(PayeeMapper::toPayeeOption)
			.filter(option -> hasText(option.getName()) || hasText(option.getAccountNumber()))
			.filter(option -> seen.add(PayeeMapper.key(option)))
			.toList();
	}

	/** An existing manual payee on the errand describing the same account, if any. */
	private Optional<FaPayeeEntity> findSameAccount(final String errandId, final PayeeRequest request) {
		final var wanted = PayeeMapper.key(PayeeMapper.toPayeeOption(PayeeMapper.applyRequest(FaPayeeEntity.create(), request)));
		return payeeRepository.findByErrandId(errandId).stream()
			.filter(entity -> wanted.equals(PayeeMapper.key(toPayeeOption(entity))))
			.findFirst();
	}

	private FaPayeeEntity requirePayee(final String errandId, final String payeeId) {
		return payeeRepository.findByIdAndErrandId(payeeId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Payee not found on errand"));
	}

	/**
	 * Queue the robot, keyed per payee so the Orchestrator's dedup does not collapse a second payee on the same errand
	 * into the first. Best-effort: the payee is already saved and usable locally, and losing the queue item must not
	 * lose it.
	 */
	private void enqueueAddPayee(final String municipalityId, final String namespace, final String errandId, final String payeeId) {
		try {
			rpaService.enqueue(municipalityId, namespace, errandId, ADD_PAYEE, payeeId, Map.of(KEY_PAYEE_ID, payeeId));
		} catch (final RuntimeException e) {
			LOG.warn("Could not queue the ADD_PAYEE task for payee {} — the payee stays PENDING", payeeId, e);
		}
	}
}
