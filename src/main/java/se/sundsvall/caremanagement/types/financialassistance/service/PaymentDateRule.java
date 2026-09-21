package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Verksamheten's payment-date rule for ekonomiskt bistånd: the bistånd is paid on the 27th of the concerned month, and
 * when the 27th falls on a weekend it moves to the weekday before — Saturday → Friday the 26th, Sunday → Friday the
 * 25th. Only Saturday/Sunday count as helg here; Swedish public holidays (röda dagar) are not considered — that is an
 * open question with verksamheten and would need a holiday calendar.
 */
final class PaymentDateRule {

	static final int PAYMENT_DAY_OF_MONTH = 27;

	private PaymentDateRule() {}

	/** The payment date for the concerned month per the rule above. */
	static LocalDate paymentDate(final YearMonth concernedMonth) {
		final var nominal = concernedMonth.atDay(PAYMENT_DAY_OF_MONTH);
		if (nominal.getDayOfWeek() == DayOfWeek.SATURDAY) {
			return nominal.minusDays(1);
		}
		if (nominal.getDayOfWeek() == DayOfWeek.SUNDAY) {
			return nominal.minusDays(2);
		}
		return nominal;
	}
}
