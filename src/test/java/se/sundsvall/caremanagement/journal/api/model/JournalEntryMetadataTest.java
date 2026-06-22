package se.sundsvall.caremanagement.journal.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JournalEntryMetadataTest {

	@Test
	void builderMethods() {
		final var types = List.of(JournalEntryType.create().withCode("C").withDisplayName("D"));
		final var metadata = JournalEntryMetadata.create().withTypes(types);

		assertThat(metadata.getTypes()).isEqualTo(types);
	}

	@Test
	void setters() {
		final var types = List.of(JournalEntryType.create().withCode("C").withDisplayName("D"));
		final var metadata = JournalEntryMetadata.create();
		metadata.setTypes(types);

		assertThat(metadata.getTypes()).isEqualTo(types);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(JournalEntryMetadata.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var types = List.of(JournalEntryType.create().withCode("C").withDisplayName("D"));
		final var a = JournalEntryMetadata.create().withTypes(types);
		final var b = JournalEntryMetadata.create().withTypes(types);
		final var c = JournalEntryMetadata.create();

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}
}
