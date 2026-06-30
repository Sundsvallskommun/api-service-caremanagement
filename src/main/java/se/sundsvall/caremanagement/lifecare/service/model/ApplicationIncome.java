package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One income as declared by the citizen in a financial-assistance application — the self-reported equivalent of an
 * {@link SsbtekIncome}, used by the nyansökan path that builds a calculation straight from the application (no SSBTEK).
 * The {@code incomeType} is the application's own code (SALARY, SWISH_DEPOSITS, …); the
 * {@link se.sundsvall.caremanagement.lifecare.service.mapper.ApplicationIncomeToFcMapper} resolves it to an FC
 * income-type id.
 */
public record ApplicationIncome(
	String incomeType,
	BigDecimal amount,
	LocalDate date,
	ApplicantRole role) {
}
