package se.sundsvall.caremanagement.stakeholders.service.event;

/**
 * Published whenever an errand's stakeholder set changes (a stakeholder is created, updated or deleted). Lets type
 * modules keep their own denormalized stakeholder-derived read models in sync — the financial-assistance module
 * recomputes the errand's applicant name from this. Carries only the errand coordinates; subscribers re-read the
 * current stakeholders themselves.
 */
public record StakeholderMutated(
	String municipalityId,
	String namespace,
	String errandId) {}
