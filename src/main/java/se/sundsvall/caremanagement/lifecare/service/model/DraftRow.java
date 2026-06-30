package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * One FC income row of a draft calculation, in module-neutral terms (amounts as {@code Double}, dates as ISO strings)
 * so it crosses the lifecare → financial-assistance boundary without leaking the generated Lifecare DTOs. The
 * financial-assistance module maps this to its own API/persistence row.
 *
 * @param typeId                the FC income-type id
 * @param typeName              the FC income-type name (resolved from the calculation proposal)
 * @param applicantAmount       the applicant's amount, may be {@code null}
 * @param applicantAmountDate   the date the applicant's amount is attributed to (ISO), may be {@code null}
 * @param coApplicantAmount     the co-applicant's amount, may be {@code null}
 * @param coApplicantAmountDate the date the co-applicant's amount is attributed to (ISO), may be {@code null}
 * @param note                  free-text note (e.g. the SSBTEK source)
 */
public record DraftRow(
	Integer typeId,
	String typeName,
	Double applicantAmount,
	String applicantAmountDate,
	Double coApplicantAmount,
	String coApplicantAmountDate,
	String note) {
}
