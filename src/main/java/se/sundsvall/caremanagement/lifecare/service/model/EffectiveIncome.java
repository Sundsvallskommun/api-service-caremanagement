package se.sundsvall.caremanagement.lifecare.service.model;

import java.time.OffsetDateTime;

/**
 * The effective income for one FC income type at commit — the applicant's and co-applicant's effective amounts (the
 * handläggare value when set, otherwise the process value) already folded into one row, ready to post to Lifecare FC.
 */
public record EffectiveIncome(
	Integer typeId,
	Double applicantAmount,
	OffsetDateTime applicantAmountDate,
	Double coApplicantAmount,
	OffsetDateTime coApplicantAmountDate,
	String note) {
}
