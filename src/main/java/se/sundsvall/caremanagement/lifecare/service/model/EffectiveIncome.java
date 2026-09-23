package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * The effective income for one FamilyCare income type at commit — the applicant's and co-applicant's effective
 * amounts (the caseworker value when set, otherwise the process value) already folded into one row, ready to post to
 * Lifecare FamilyCare.
 *
 * <p>
 * {@code typeName} travels along because a caseworker-added row can carry only the name: the type options careM gives
 * the frontend are names, not FamilyCare ids. The id is then resolved against the proposal's income types at commit.
 * </p>
 */
public record EffectiveIncome(
	Integer typeId,
	String typeName,
	BigDecimal applicantAmount,
	OffsetDateTime applicantAmountDate,
	BigDecimal coApplicantAmount,
	OffsetDateTime coApplicantAmountDate,
	String note) {
}
