package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * A single household member on a Lifecare normberäkning, as read for display. The {@code personId} (personnummer) is
 * passed through for the handläggare-facing case view (the same audience that sees it in Lifecare).
 */
public record CalculationPersonView(
	String personId,
	String name,
	Double amount,
	String deviationFromDate,
	String deviationToDate) {
}
