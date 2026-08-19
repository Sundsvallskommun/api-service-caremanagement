package se.sundsvall.caremanagement.journal.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * One selectable journal entry type — a machine {@code code} paired with the Swedish Lifecare label the frontend shows
 * and that an RPA flow selects in the Lifecare "Typ" dropdown. Journal types are municipality-configured in Lifecare,
 * so
 * the served catalogue is provisional and {@code JournalEntry.type} is not validated against it.
 */
@Schema(description = "A selectable journal entry type — the code and the Swedish Lifecare label.")
public class JournalEntryType {

	@Schema(description = "The type code", examples = "JOURNALED_MESSAGE")
	private String code;

	@Schema(description = "Human-readable Swedish label (the Lifecare 'Typ' value)", examples = "Journalfört meddelande")
	private String displayName;

	public static JournalEntryType create() {
		return new JournalEntryType();
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public JournalEntryType withCode(final String code) {
		this.code = code;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public JournalEntryType withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final JournalEntryType that = (JournalEntryType) o;
		return Objects.equals(code, that.code) && Objects.equals(displayName, that.displayName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, displayName);
	}

	@Override
	public String toString() {
		return "JournalEntryType{code='" + code + "', displayName='" + displayName + "'}";
	}
}
