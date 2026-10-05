package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;

/**
 * A Lifecare utbetalning (payment) as read for the handläggare-facing payee options — the payment header and the
 * payee it went to. A display projection of the generated {@code PersonBasedPaymentDTO}; dates and the concerned month
 * are passed through as the raw Lifecare strings. The payment's person list (personal numbers) is deliberately not
 * projected — the reader only needs the payee (name + account), never the household's identities.
 */
public record PaymentView(
	Integer id,
	BigDecimal amount,
	String paymentMethod,
	String payDate,
	String clearing,
	String accountNumber,
	String name,
	String streetAddress,
	String careOfAddress,
	String postalCode,
	String postalAddress,
	String message,
	String concernedMonth) {
}
