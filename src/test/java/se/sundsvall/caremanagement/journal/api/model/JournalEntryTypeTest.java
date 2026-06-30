package se.sundsvall.caremanagement.journal.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JournalEntryTypeTest {

	@Test
	void builderMethods() {
		final var type = JournalEntryType.create().withCode("JOURNALED_MESSAGE").withDisplayName("Journalfört meddelande");

		assertThat(type.getCode()).isEqualTo("JOURNALED_MESSAGE");
		assertThat(type.getDisplayName()).isEqualTo("Journalfört meddelande");
	}

	@Test
	void setters() {
		final var type = JournalEntryType.create();
		type.setCode("OTHER");
		type.setDisplayName("Övrigt");

		assertThat(type.getCode()).isEqualTo("OTHER");
		assertThat(type.getDisplayName()).isEqualTo("Övrigt");
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(JournalEntryType.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsHashCodeAndToString() {
		final var a = JournalEntryType.create().withCode("C").withDisplayName("D");
		final var b = JournalEntryType.create().withCode("C").withDisplayName("D");
		final var c = JournalEntryType.create().withCode("X").withDisplayName("D");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
		assertThat(a).hasToString("JournalEntryType{code='C', displayName='D'}");
	}
}
