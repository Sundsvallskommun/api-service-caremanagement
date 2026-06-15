package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * Which member of the household an SSBTEK income belongs to. FC's calculation income row carries the applicant and
 * co-applicant amounts in separate columns, so each {@link SsbtekIncome} must declare its role.
 */
public enum ApplicantRole {
	APPLICANT,
	CO_APPLICANT
}
