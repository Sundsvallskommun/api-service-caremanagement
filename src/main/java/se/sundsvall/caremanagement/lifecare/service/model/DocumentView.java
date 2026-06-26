package se.sundsvall.caremanagement.lifecare.service.model;

/**
 * A single Lifecare document as listed for a person — metadata only. The content (a generated PDF) is fetched
 * separately by {@code id}. The {@code date} is passed through as the raw Lifecare string.
 */
public record DocumentView(
	String id,
	String title,
	String date,
	String documentType,
	String ownerId,
	String ownerType) {
}
