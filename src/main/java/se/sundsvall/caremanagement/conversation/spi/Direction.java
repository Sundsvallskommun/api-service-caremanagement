package se.sundsvall.caremanagement.conversation.spi;

/**
 * Direction of a conversation message: {@code OUTBOUND} = caseworker → applicant, {@code INBOUND} = applicant →
 * caseworker. An internal/service value type that backs the conversation logic and validation; the API field and the
 * {@code direction} column carry its {@link #name()} as a {@code String}, so the enum itself is never exposed in an API
 * model nor stored as a database enum.
 *
 * <p>
 * Each direction also carries the {@link #role()} of the party that authors it — {@code INBOUND} → {@code CLIENT}
 * (applicant), {@code OUTBOUND} → {@code CASEWORKER} — which is the value denormalised into {@code sender_role} and
 * into
 * {@code reader_side} (the reader is identified by the side they author). This lets a single type back both the sender
 * and reader concepts, so no separate {@code SenderRole}/{@code ReaderSide} enums are needed.
 * </p>
 */
public enum Direction {

	INBOUND("CLIENT"),
	OUTBOUND("CASEWORKER");

	private final String role;

	Direction(final String role) {
		this.role = role;
	}

	/**
	 * The role of the party that authors messages of this direction: {@code CLIENT} for INBOUND, {@code CASEWORKER} for
	 * OUTBOUND.
	 */
	public String role() {
		return role;
	}

	/** The other direction — for a reader who authors this direction, the direction of the messages addressed to them. */
	public Direction opposite() {
		return switch (this) {
			case INBOUND -> OUTBOUND;
			case OUTBOUND -> INBOUND;
		};
	}
}
