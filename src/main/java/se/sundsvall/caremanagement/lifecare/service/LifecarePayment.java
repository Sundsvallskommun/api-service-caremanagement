package se.sundsvall.caremanagement.lifecare.service;

/**
 * A payment as Lifecare holds it: its Lifecare id, the insats (service) it is registered on, the month or months it
 * concerns (Lifecare's own text, which carries yyyy-MM) and its PayDate, when it has one. Read, never stored.
 */
public record LifecarePayment(String id, Integer serviceId, String concernedMonth, String payDate) {
}
