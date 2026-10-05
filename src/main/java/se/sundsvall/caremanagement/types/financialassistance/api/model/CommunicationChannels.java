package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * The channels the caseworker chose for sending the calculation and decision to the applicant. Recorded on the errand
 * for the audit trail and echoed back to Draken, whose BFF performs the actual send (PDF via templating, then Mina
 * sidor message / digital brevlåda / brev) — caremanagement sends nothing itself.
 */
@Schema(description = "The channels chosen for communicating the calculation and decision to the applicant.")
public class CommunicationChannels {

	@Schema(description = "Send as a message in Mina sidor", examples = "true", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Boolean minaSidor;

	@Schema(description = "Send to the applicant's digital mailbox (digital brevlåda)", examples = "false", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Boolean digitalMailbox;

	@Schema(description = "Send as a physical letter", examples = "false", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Boolean letter;

	public static CommunicationChannels create() {
		return new CommunicationChannels();
	}

	public Boolean getMinaSidor() {
		return minaSidor;
	}

	public void setMinaSidor(final Boolean minaSidor) {
		this.minaSidor = minaSidor;
	}

	public CommunicationChannels withMinaSidor(final Boolean minaSidor) {
		this.minaSidor = minaSidor;
		return this;
	}

	public Boolean getDigitalMailbox() {
		return digitalMailbox;
	}

	public void setDigitalMailbox(final Boolean digitalMailbox) {
		this.digitalMailbox = digitalMailbox;
	}

	public CommunicationChannels withDigitalMailbox(final Boolean digitalMailbox) {
		this.digitalMailbox = digitalMailbox;
		return this;
	}

	public Boolean getLetter() {
		return letter;
	}

	public void setLetter(final Boolean letter) {
		this.letter = letter;
	}

	public CommunicationChannels withLetter(final Boolean letter) {
		this.letter = letter;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final CommunicationChannels that = (CommunicationChannels) o;
		return Objects.equals(minaSidor, that.minaSidor) && Objects.equals(digitalMailbox, that.digitalMailbox) && Objects.equals(letter, that.letter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(minaSidor, digitalMailbox, letter);
	}

	@Override
	public String toString() {
		return "CommunicationChannels{" +
			"minaSidor=" + minaSidor +
			", digitalMailbox=" + digitalMailbox +
			", letter=" + letter +
			'}';
	}
}
