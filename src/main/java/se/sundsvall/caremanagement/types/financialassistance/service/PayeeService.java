package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.PaymentView;
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
 * Nothing here writes anything on a read.
 * </p>
 *
 * <p>
 * A manually added payee is a bridging state, not a second register: {@code POST} stores it so it is selectable now and
 * waits for whoever creates it in Lifecare to report back through {@code .../payees/{payeeId}/lifecare-result}, after
 * which the ordinary payment history carries it. When that has happened the manual row and its Lifecare twin collapse
 * into one option, so the list does not grow a duplicate.
 * </p>
 */
@Service
public class PayeeService {

	private static final Logger LOG = LoggerFactory.getLogger(PayeeService.class);

	/** How far back the Lifecare payment history is read for payee options. */
	static final int PAYEE_LOOKBACK_MONTHS = 12;

	static final String LIFECARE_STATUS_PENDING = "PENDING";
	static final String LIFECARE_STATUS_SYNCED = "SYNCED";
	static final String LIFECARE_STATUS_FAILED = "FAILED";

	static final String OUTCOME_ADDED = "ADDED";
	static final String OUTCOME_ALREADY_EXISTS = "ALREADY_EXISTS";
	static final String OUTCOME_FAILED = "FAILED";

	static final String ERROR_DETAIL_REQUIRED = "detail is required when outcome is FAILED — it is Lifecare's own message, shown to the caseworker";
	static final String ERROR_ALREADY_SYNCED = "Payee is already SYNCED in Lifecare and cannot be reported as FAILED";

	static final String WARNING_PAYEE_PENDING = "Betalningsmottagaren \"%s\" är inte upplagd i Lifecare ännu – utbetalningen kan inte registreras förrän den har lagts upp där.";
	static final String WARNING_PAYEE_FAILED = "Betalningsmottagaren \"%s\" kunde inte läggas upp i Lifecare: %s";

	private final ErrandService errandService;
	private final HouseholdPartyService householdPartyService;
	private final LifecareCaseHistoryService lifecareCaseHistoryService;
	private final FaPayeeRepository payeeRepository;

	PayeeService(final ErrandService errandService, final HouseholdPartyService householdPartyService,
		final LifecareCaseHistoryService lifecareCaseHistoryService, final FaPayeeRepository payeeRepository) {
		this.errandService = errandService;
		this.householdPartyService = householdPartyService;
		this.lifecareCaseHistoryService = lifecareCaseHistoryService;
		this.payeeRepository = payeeRepository;
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
	 * A single payee on an errand. Scoped: throws {@code 404} when the errand or payee is
	 * missing.
	 */
	@Transactional(readOnly = true)
	public PayeeOption get(final String municipalityId, final String namespace, final String errandId, final String payeeId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return toPayeeOption(requirePayee(errandId, payeeId));
	}

	/**
	 * Add a payee by hand. It starts {@code PENDING} until its Lifecare creation is reported back. An identical payee
	 * already on the errand is reused rather than duplicated, so a double submit does not put the same account in the
	 * dropdown twice.
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

		return toPayeeOption(saved);
	}

	/**
	 * Record the report of the payee's creation in Lifecare. {@code ALREADY_EXISTS} counts as success — the writer does
	 * not create a duplicate, and the caseworker's intent is satisfied either way. Re-posting the same outcome is
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
	 * Lifecare against a payee that has not been created there.
	 *
	 * <p>
	 * Matched per payee rather than per errand on purpose — a payee the caseworker added and then did not use must not
	 * warn about a payment it has nothing to do with. A payee that is not among the errand's manual rows at all came
	 * from the Lifecare payment history and is in Lifecare by definition, so it never warns.
	 * </p>
	 *
	 * <p>
	 * Returns text, and finalize does not fail on it: the decision is the caseworker's and the payment rows are created
	 * either way. Blocking here would strand a decision that is otherwise complete.
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

	/** Lifecare's own message when the creation failed, otherwise "not created yet" — both are actionable, differently. */
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
			payments = lifecareCaseHistoryService.listPayments(municipalityId, applicant.get(), today.minusMonths(PAYEE_LOOKBACK_MONTHS), today);
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

}
