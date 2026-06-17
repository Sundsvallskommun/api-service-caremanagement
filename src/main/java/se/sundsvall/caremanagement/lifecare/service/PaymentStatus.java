package se.sundsvall.caremanagement.lifecare.service;

/**
 * The Lifecare utbetalning status for an application month: whether it has been effectuated and, when it has, the
 * Lifecare PayDate.
 */
public record PaymentStatus(boolean effectuated, String paymentDate) {
}
