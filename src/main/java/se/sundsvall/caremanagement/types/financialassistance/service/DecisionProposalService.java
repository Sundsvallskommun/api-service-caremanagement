package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.model.DecisionView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.DecisionProposal;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceSchema;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.ProposalMapper;

import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.service.ProposalBasisService.EXPLANATION_NO_NORM;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.DECISION_PROPOSAL_TYPES;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.TYPE_EXPENSE_PARTIALLY_REJECTED;
import static se.sundsvall.caremanagement.types.financialassistance.service.WarningService.TYPE_PREVIOUS_DECISION_ADVANCE_ON_BENEFIT;

/**
 * The decision proposal (beslutsförslag) — verksamheten's rule for the DECISION tab once the normberäkning is
 * komplett-markerad: propose the outcome from the calculation (BIFALL / DELAVSLAG / AVSLAG), the calculation's period,
 * the previous Lifecare decision's orsak (plus every other orsak to pick from), and the frastext; and raise the
 * DECISION-section warnings — the previous decision being förskott på förmån, and one per expense not approved in
 * full.
 *
 * <p>
 * Compute-on-read: the proposal is derived data and nothing but the warnings is persisted, so a read is idempotent and
 * always fresh. It is also run when the CALCULATION section is approved (see
 * {@link FinancialAssistanceApprovalService}). Lifecare reads are best-effort — a failed read leaves the previous
 * decision (and the warnings that depend on it) out rather than failing the proposal.
 * </p>
 */
@Service
public class DecisionProposalService {

	private static final Logger LOG = LoggerFactory.getLogger(DecisionProposalService.class);

	/** How far back the previous-decision lookup reaches from the application month. */
	static final int PREVIOUS_DECISION_LOOKBACK_MONTHS = 12;

	/**
	 * The seeded orsak list. FamilyCare exposes no reason catalogue, so this is the EB set verksamheten works with; the
	 * previous decision's reason is added on top when it is not already here. Extend as verksamheten names more.
	 */
	static final List<String> DEFAULT_REASON_OPTIONS = List.of(
		"Försörjningsstöd",
		"Livsföring i övrigt",
		"Ekonomiskt bistånd",
		"Förskott på förmån");

	static final String WARNING_PREVIOUS_DECISION_ADVANCE_ON_BENEFIT = "Föregående beslut i Lifecare var förskott på förmån – kontrollera vilket beslut som ska fattas";
	static final String WARNING_EXPENSE_PARTIALLY_REJECTED = "Ansökt belopp för %s är %s kronor, %s kronor har inte godkänts – delavslag";

	private final ProposalBasisService proposalBasisService;
	private final LifecareCaseHistoryService lifecareCaseHistoryService;
	private final WarningService warningService;

	DecisionProposalService(final ProposalBasisService proposalBasisService, final LifecareCaseHistoryService lifecareCaseHistoryService,
		final WarningService warningService) {
		this.proposalBasisService = proposalBasisService;
		this.lifecareCaseHistoryService = lifecareCaseHistoryService;
		this.warningService = warningService;
	}

	/**
	 * Compute the decision proposal for an errand and reconcile its DECISION-section warnings. Scoped: throws {@code 404}
	 * when the errand is missing in this namespace/municipality or has no calculation draft.
	 */
	@Transactional
	public DecisionProposal get(final String municipalityId, final String namespace, final String errandId) {
		final var basis = proposalBasisService.basis(municipalityId, namespace, errandId);
		final var draft = basis.draft();
		final var previousDecision = basis.household().applicantPersonalNumber()
			.flatMap(applicant -> basis.applicationMonth().flatMap(month -> previousDecision(applicant, month)));
		final var partiallyRejected = ProposalMapper.partiallyRejectedExpenses(draft);
		final var outcome = basis.estimatedAmount().map(amount -> ProposalMapper.outcome(amount, partiallyRejected));
		final var reason = previousDecision.map(DecisionView::reason).filter(text -> hasText(text));

		final var warnings = warningService.reconcileByTypes(errandId, DECISION_PROPOSAL_TYPES, warningInputs(previousDecision, partiallyRejected));

		return DecisionProposal.create()
			.withOutcome(outcome.orElse(null))
			.withOutcomeOptions(FinancialAssistanceSchema.decisionOptions())
			.withPeriodFrom(draft.getCalculationFromDate())
			.withPeriodTo(draft.getCalculationToDate())
			.withConcernedMonth(basis.applicationMonth().map(YearMonth::toString).orElse(null))
			.withEstimatedAmount(basis.estimatedAmount().orElse(null))
			.withNormSum(basis.normSum().orElse(null))
			.withIncomeSum(draft.getIncomeSum())
			.withExpenseSum(draft.getExpenseSum())
			.withSpecialExpenseSum(draft.getSpecialExpenseSum())
			.withExplanation(explanation(basis))
			.withReason(reason.orElse(null))
			.withReasonOptions(reasonOptions(reason))
			.withPhraseText(outcome.map(value -> ProposalMapper.phraseText(value, ProposalMapper.childrenInCalculation(draft))).orElse(null))
			.withPreviousDecision(previousDecision.map(ProposalMapper::toPreviousDecision).orElse(null))
			.withWarnings(warnings);
	}

	private static String explanation(final ProposalBasisService.ProposalBasis basis) {
		if (basis.estimatedAmount().isPresent()) {
			return null;
		}
		return EXPLANATION_NO_NORM;
	}

	private static List<String> reasonOptions(final Optional<String> previousReason) {
		final var options = new LinkedHashSet<>(DEFAULT_REASON_OPTIONS);
		previousReason.ifPresent(options::add);
		return List.copyOf(options);
	}

	private static List<WarningService.WarningInput> warningInputs(final Optional<DecisionView> previousDecision, final List<NormExpenseRow> partiallyRejected) {
		final var inputs = new ArrayList<WarningService.WarningInput>();
		previousDecision.filter(ProposalMapper::isAdvanceOnBenefit)
			.ifPresent(_ -> inputs.add(new WarningService.WarningInput(TYPE_PREVIOUS_DECISION_ADVANCE_ON_BENEFIT, "previous-decision", WARNING_PREVIOUS_DECISION_ADVANCE_ON_BENEFIT)));
		partiallyRejected.forEach(row -> inputs.add(new WarningService.WarningInput(TYPE_EXPENSE_PARTIALLY_REJECTED, ProposalMapper.expenseSourceKey(row),
			WARNING_EXPENSE_PARTIALLY_REJECTED.formatted(ProposalMapper.expenseLabel(row), ProposalMapper.plain(row.getAppliedAmount()),
				ProposalMapper.plain(row.getAppliedAmount().subtract(orZero(row)))))));
		return inputs;
	}

	private static java.math.BigDecimal orZero(final NormExpenseRow row) {
		return Optional.ofNullable(row.getEffectiveAmount()).orElse(java.math.BigDecimal.ZERO);
	}

	/**
	 * The applicant's most recent Lifecare decision within the lookback window ending at the application month —
	 * Lifecare lists newest-first, so the first one. Best-effort: a failed read degrades to "no previous decision".
	 */
	private Optional<DecisionView> previousDecision(final String applicant, final YearMonth applicationMonth) {
		try {
			return lifecareCaseHistoryService.listDecisions(applicant, applicationMonth.minusMonths(PREVIOUS_DECISION_LOOKBACK_MONTHS).atDay(1), applicationMonth.atEndOfMonth())
				.stream()
				.findFirst();
		} catch (final RuntimeException e) {
			LOG.warn("Could not read the previous Lifecare decision — the decision proposal is computed without it", e);
			return Optional.empty();
		}
	}
}
