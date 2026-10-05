package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One income the applicant declared in the application — Swish, lön, tjänstepension and the rest SSBTEK never
 * reports — as the calculation draft needs it.
 *
 * @param incomeType the application income code, e.g. {@code SWISH_DEPOSITS}
 * @param recipient  {@code APPLICANT} or {@code CO_APPLICANT}; anything else counts as the applicant's
 * @param amount     the declared amount
 * @param date       the date the income was received
 * @param label      how the handläggare reads the income type, used in the row's note
 */
public record ApplicationIncome(
	String incomeType,
	String recipient,
	BigDecimal amount,
	LocalDate date,
	String label) {
}
