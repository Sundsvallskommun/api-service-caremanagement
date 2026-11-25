package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * Which member of the household an SSBTEK income belongs to. FamilyCare's calculation income row carries the applicant
 * and co-applicant amounts in separate columns, so each {@link SsbtekIncome} must declare its role.
 */
public enum ApplicantRole {
	APPLICANT,
	CO_APPLICANT,
	/**
	 * A household child. The Lifecare normberäkning has income columns for the applicant and co-applicant only, so a
	 * child's income is transferred on the applicant's column; {@link SsbtekIncome#partyId()} names the child.
	 */
	CHILD
}
