package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * A single Lifecare FC actualisation (case intake) as listed for a person — the read-side counterpart of the
 * write-back the financial assistance intake performs. A privacy-safe projection of the generated
 * {@code PersonBasedAktualiseringDTO}:
 * the {@code personId} (personnummer) is deliberately dropped so it never leaves the integration boundary. The
 * {@code date} is passed through as the raw Lifecare string (the listing is for display/selection only).
 */
public record ActualisationSummary(
	Integer id,
	String type,
	String name,
	String date,
	String reason,
	String regards,
	String fromWho,
	String caseworker,
	String organization,
	String status,
	Integer investigationId,
	Integer serviceId,
	Integer decisionId) {
}
