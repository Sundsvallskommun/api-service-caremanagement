package se.sundsvall.caremanagement.types.financialassistance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;

import static se.sundsvall.caremanagement.types.financialassistance.service.SectionApprovalService.SECTION_CALCULATION;
import static se.sundsvall.caremanagement.types.financialassistance.service.SectionApprovalService.SECTION_DECISION;

/**
 * The errand-scoped facade for the financial assistance view-section approvals (calculation / payment / decision):
 * every
 * call first scope-checks the errand (throws {@code 404} when it is missing in this namespace/municipality) before
 * delegating to {@link SectionApprovalService}. Split out of the former FinancialAssistanceService god-service.
 *
 * <p>
 * Approving a section also refreshes the next section's warnings — CALCULATION approved → the decision proposal,
 * DECISION approved → the payment warnings — so they are on the errand the moment the caseworker moves on.
 * The recompute is a direct, best-effort call (not a Modulith event: those run asynchronously after commit, which
 * would leave the tab briefly stale) and never fails the approval — the decision proposal is also recomputed on every
 * read.
 * </p>
 */
@Service
@Transactional
public class FinancialAssistanceApprovalService {

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceApprovalService.class);

	private final ErrandService errandService;
	private final SectionApprovalService sectionApprovalService;
	private final DecisionProposalService decisionProposalService;
	private final PaymentWarningService paymentWarningService;

	FinancialAssistanceApprovalService(final ErrandService errandService, final SectionApprovalService sectionApprovalService,
		final DecisionProposalService decisionProposalService, final PaymentWarningService paymentWarningService) {
		this.errandService = errandService;
		this.sectionApprovalService = sectionApprovalService;
		this.decisionProposalService = decisionProposalService;
		this.paymentWarningService = paymentWarningService;
	}

	/**
	 * The caseworker approval state of the three financial assistance view sections (calculation / payment / decision).
	 * Scoped: throws {@code 404} when the errand is missing in this namespace/municipality.
	 */
	@Transactional(readOnly = true)
	public SectionApprovals getSectionApprovals(final String municipalityId, final String namespace, final String errandId) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		return sectionApprovalService.approvals(errandId);
	}

	/**
	 * Set a section's approval — a caseworker verifies it as approved (or withdraws the approval). Scoped: throws
	 * {@code 404} when the errand is missing, {@code 400} when the section is not CALCULATION/PAYMENT/DECISION.
	 */
	public SectionApproval setSectionApproval(final String municipalityId, final String namespace, final String errandId, final String section,
		final boolean approved, final String approvedBy) {
		errandService.readErrand(municipalityId, namespace, errandId); // scope check (404 when missing)
		final var approval = sectionApprovalService.setApproval(errandId, section, approved, approvedBy);
		if (approved) {
			recomputeNextProposal(municipalityId, namespace, errandId, section);
		}
		return approval;
	}

	/**
	 * CALCULATION approved → decision proposal; DECISION approved → payment warnings. Best-effort, never fails the
	 * approval.
	 */
	private void recomputeNextProposal(final String municipalityId, final String namespace, final String errandId, final String section) {
		try {
			if (SECTION_CALCULATION.equals(section)) {
				decisionProposalService.get(municipalityId, namespace, errandId);
			} else if (SECTION_DECISION.equals(section)) {
				paymentWarningService.reconcile(municipalityId, namespace, errandId);
			}
		} catch (final RuntimeException e) {
			LOG.warn("Could not refresh the next section's warnings after approving {}", section, e);
		}
	}
}
