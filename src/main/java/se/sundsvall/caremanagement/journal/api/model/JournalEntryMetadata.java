package se.sundsvall.caremanagement.journal.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * The journal metadata response — the provisional catalogue of journal entry types the frontend offers in the "Typ"
 * dropdown. Journal types are municipality-configured in Lifecare; this set is a starting point and is not enforced on
 * {@code JournalEntry.type}.
 */
@Schema(description = "Journal metadata — the provisional catalogue of selectable journal entry types.")
public class JournalEntryMetadata {

	@ArraySchema(arraySchema = @Schema(description = "Selectable journal entry types"), schema = @Schema(implementation = JournalEntryType.class))
	private List<JournalEntryType> types;

	public static JournalEntryMetadata create() {
		return new JournalEntryMetadata();
	}

	public List<JournalEntryType> getTypes() {
		return types;
	}

	public void setTypes(final List<JournalEntryType> types) {
		this.types = types;
	}

	public JournalEntryMetadata withTypes(final List<JournalEntryType> types) {
		this.types = types;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final JournalEntryMetadata that = (JournalEntryMetadata) o;
		return Objects.equals(types, that.types);
	}

	@Override
	public int hashCode() {
		return Objects.hash(types);
	}
}
