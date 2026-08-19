package se.sundsvall.caremanagement.types.financialassistance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApproval;
import se.sundsvall.caremanagement.types.financialassistance.api.model.SectionApprovals;

/**
 * The errand-scoped facade for the financial assistance view-section approvals (calculation / payment / decision):
 * every
 * call first scope-checks the errand (throws {@code 404} when it is missing in this namespace/municipality) before
 * delegating to {@link SectionApprovalService}. Split out of the former FinancialAssistanceService god-service.
 */
@Service
@Transactional
public class FinancialAssistanceApprovalService {

	private final ErrandService errandService;
	private final SectionApprovalService sectionApprovalService;

	FinancialAssistanceApprovalService(final ErrandService errandService, final SectionApprovalService sectionApprovalService) {
		this.errandService = errandService;
		this.sectionApprovalService = sectionApprovalService;
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
		return sectionApprovalService.setApproval(errandId, section, approved, approvedBy);
	}
}
