package se.sundsvall.caremanagement.conversation.spi;

/**
 * Who sent a message or attachment, denormalised from the message {@link Direction}: an {@code INBOUND} message comes
 * from the applicant ({@code CLIENT}), {@code OUTBOUND} from the caseworker ({@code CASEWORKER}). An internal/service
 * value type that backs the conversation and attachment logic; the API field and the {@code sender_role} column carry
 * its {@link #name()} as a {@code String}, so the enum is never exposed in an API model nor stored as a database enum.
 */
public enum SenderRole {
	CLIENT,
	CASEWORKER;

	/** Applicant-sent (INBOUND) messages are from the {@link #CLIENT}; everything else is the {@link #CASEWORKER}. */
	public static SenderRole fromDirection(final Direction direction) {
		if (direction == Direction.INBOUND) {
			return CLIENT;
		}
		return CASEWORKER;
	}
}
