package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * The effective income for one FamilyCare income type at commit — the applicant's and co-applicant's effective
 * amounts (the caseworker value when set, otherwise the process value) already folded into one row, ready to post to
 * Lifecare FamilyCare.
 */
public record EffectiveIncome(
	Integer typeId,
	BigDecimal applicantAmount,
	OffsetDateTime applicantAmountDate,
	BigDecimal coApplicantAmount,
	OffsetDateTime coApplicantAmountDate,
	String note) {
}
