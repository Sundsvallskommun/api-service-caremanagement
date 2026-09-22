package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.PaymentView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PaymentProposal;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ProposedPayment;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.ProposalMapper;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.Comparator.reverseOrder;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.service.ProposalBasisService.EXPLANATION_NO_NORM;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.PAYMENT_PROPOSAL_TYPES;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.TYPE_CO_APPLICANT_SPLIT_PAYMENT;

/**
 * The payment proposal (utbetalningsförslag) — verksamheten's rule for the PAYMENT tab once the decision is
 * komplett-markerad: propose the payment from the previous Lifecare payment — the 27th (weekday before on a weekend)
 * of the calculation's month, the whole bistånd, the same payee (with every payee seen on the applicant's previous
 * payments to pick from) — and warn when there is a medsökande so the caseworker checks for delad utbetalning. The
 * proposal is a list of payments with one entry so the frontend can split it.
 *
 * <p>
 * Payee choice: when the applicant stated in the application that the account is <em>not</em> the same as before
 * ({@code paymentSameAsPrevious=false}) and named one, that account is proposed ({@code payeeSource=APPLICATION});
 * otherwise the previous Lifecare payment's payee ({@code PREVIOUS_PAYMENT}). Kontering is always null — FamilyCare
 * exposes no accounting code.
 * </p>
 *
 * <p>
 * Compute-on-read and idempotent like {@link DecisionProposalService}; only the PAYMENT-section warnings are persisted.
 * Also run when the DECISION section is approved. Lifecare reads are best-effort.
 * </p>
 */
@Service
public class PaymentProposalService {

	private static final Logger LOG = LoggerFactory.getLogger(PaymentProposalService.class);

	/** How far back the previous-payments lookup reaches from the application month. */
	static final int PREVIOUS_PAYMENT_LOOKBACK_MONTHS = 12;

	static final String PAYEE_SOURCE_PREVIOUS_PAYMENT = "PREVIOUS_PAYMENT";
	static final String PAYEE_SOURCE_APPLICATION = "APPLICATION";

	static final String WARNING_CO_APPLICANT_SPLIT_PAYMENT = "Det finns medsökande i ärendet – kontrollera om det ska vara delad utbetalning";

	private final ProposalBasisService proposalBasisService;
	private final LifecareCaseHistoryService lifecareCaseHistoryService;
	private final WarningService warningService;

	PaymentProposalService(final ProposalBasisService proposalBasisService, final LifecareCaseHistoryService lifecareCaseHistoryService,
		final WarningService warningService) {
		this.proposalBasisService = proposalBasisService;
		this.lifecareCaseHistoryService = lifecareCaseHistoryService;
		this.warningService = warningService;
	}

	/**
	 * Compute the payment proposal for an errand and reconcile its PAYMENT-section warnings. Scoped: throws {@code 404}
	 * when the errand is missing in this namespace/municipality or has no calculation draft.
	 */
	@Transactional
	public PaymentProposal get(final String municipalityId, final String namespace, final String errandId) {
		final var basis = proposalBasisService.basis(municipalityId, namespace, errandId);
		final var household = basis.household();
		final var previousPayments = household.applicantPersonalNumber()
			.flatMap(applicant -> basis.applicationMonth().map(month -> previousPayments(municipalityId, applicant, month)))
			.orElseGet(List::of);
		final var previousPayment = previousPayments.stream()
			.filter(payment -> hasText(payment.payDate()))
			.max(comparing(PaymentView::payDate, nullsLast(reverseOrder())).reversed());

		final var applicationPayee = household.applicantPerson()
			.filter(person -> Boolean.FALSE.equals(person.getPaymentSameAsPrevious()))
			.flatMap(person -> ProposalMapper.toPayee(person, household.applicantName().orElse(null)));
		final var previousPayee = previousPayment.map(ProposalMapper::toPayee);

		final Optional<Payee> payee;
		final String payeeSource;
		if (applicationPayee.isPresent()) {
			payee = applicationPayee;
			payeeSource = PAYEE_SOURCE_APPLICATION;
		} else if (previousPayee.isPresent()) {
			payee = previousPayee;
			payeeSource = PAYEE_SOURCE_PREVIOUS_PAYMENT;
		} else {
			payee = Optional.empty();
			payeeSource = null;
		}

		final var warnings = warningService.reconcileByTypes(errandId, PAYMENT_PROPOSAL_TYPES, warningInputs(household.coApplicantPresent()));

		return PaymentProposal.create()
			.withPayments(List.of(ProposedPayment.create()
				.withPaymentDate(basis.applicationMonth().map(PaymentDateRule::paymentDate).orElse(null))
				.withAmount(basis.estimatedAmount().orElse(null))
				.withConcernedMonth(basis.applicationMonth().map(YearMonth::toString).orElse(null))
				.withPayee(payee.orElse(null))
				.withAccountingCode(null)))
			.withPayeeOptions(ProposalMapper.distinctPayees(previousPayments))
			.withPayeeSource(payeeSource)
			.withPreviousPayment(previousPayment.map(ProposalMapper::toPreviousPayment).orElse(null))
			.withExplanation(explanation(basis))
			.withWarnings(warnings);
	}

	private static String explanation(final ProposalBasisService.ProposalBasis basis) {
		if (basis.estimatedAmount().isPresent()) {
			return null;
		}
		return EXPLANATION_NO_NORM;
	}

	private static List<WarningService.WarningInput> warningInputs(final boolean coApplicantPresent) {
		if (!coApplicantPresent) {
			return List.of();
		}
		return List.of(new WarningService.WarningInput(TYPE_CO_APPLICANT_SPLIT_PAYMENT, "co-applicant", WARNING_CO_APPLICANT_SPLIT_PAYMENT));
	}

	/** The applicant's Lifecare payments in the lookback window ending at the application month, best-effort. */
	private List<PaymentView> previousPayments(final String municipalityId, final String applicant, final YearMonth applicationMonth) {
		try {
			return lifecareCaseHistoryService.listPayments(municipalityId, applicant, applicationMonth.minusMonths(PREVIOUS_PAYMENT_LOOKBACK_MONTHS).atDay(1), applicationMonth.atEndOfMonth());
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous Lifecare payments — the payment proposal is computed without them", e);
			return List.of();
		}
	}
}
