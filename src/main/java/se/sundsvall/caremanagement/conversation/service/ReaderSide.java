package se.sundsvall.caremanagement.conversation.service;

/**
 * The two sides of a conversation, as seen from the read-state perspective. A side reads the messages addressed to it,
 * which are the messages with the opposite {@code direction}: the {@link #CASEWORKER} (Draken) reads INBOUND
 * (applicant)
 * messages, the {@link #CLIENT} (Mina sidor) reads OUTBOUND (caseworker) messages. The name is what the read receipt
 * stores in {@code reader_side}. This is an internal concept derived from the {@code X-Sent-By} identifier — it is
 * never
 * part of an API model.
 */
public enum ReaderSide {

	CASEWORKER("INBOUND"),
	CLIENT("OUTBOUND");

	private final String addressedDirection;

	ReaderSide(final String addressedDirection) {
		this.addressedDirection = addressedDirection;
	}

	/** The direction of the messages addressed to this side — the ones that count towards its unread total. */
	public String addressedDirection() {
		return addressedDirection;
	}
}
