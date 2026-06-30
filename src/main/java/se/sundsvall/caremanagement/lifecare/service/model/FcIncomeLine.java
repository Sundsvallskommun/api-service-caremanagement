package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * One process-derived income line for the calculation draft — a classified SSBTEK income resolved to its FC income
 * type, for a single recipient ({@code APPLICANT}/{@code CO_APPLICANT}). The financialassistance module maps these to
 * the draft's income rows (process column). Distinct from the FC post DTO, which folds both recipients into one row.
 */
public record FcIncomeLine(
	Integer typeId,
	String typeName,
	String recipient,
	BigDecimal amount,
	OffsetDateTime date,
	String note) {
}
