package se.sundsvall.caremanagement.conversation.spi;

/**
 * Direction of a conversation message: {@code OUTBOUND} = caseworker → applicant, {@code INBOUND} = applicant →
 * caseworker. An internal/service value type that backs the conversation logic and validation; the API field and the
 * {@code direction} column carry its {@link #name()} as a {@code String}, so the enum itself is never exposed in an API
 * model nor stored as a database enum.
 */
public enum Direction {
	INBOUND,
	OUTBOUND
}
