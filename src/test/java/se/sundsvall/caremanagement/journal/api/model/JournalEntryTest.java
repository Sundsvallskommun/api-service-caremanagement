package se.sundsvall.caremanagement.journal.api.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JournalEntryTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final OffsetDateTime ENTRY_DATE_TIME = OffsetDateTime.parse("2025-05-30T14:30:00+02:00");

	@Test
	void builderMethods() {
		final var entry = JournalEntry.create()
			.withId("je1")
			.withErrandId("e1")
			.withType("Journalfört meddelande")
			.withHeading("Rubrik")
			.withText("body")
			.withEntryDateTime(ENTRY_DATE_TIME)
			.withStatus("WORKING")
			.withCreatedBy("carola")
			.withCreated(FIXED_TIMESTAMP)
			.withModifiedBy("editor")
			.withModified(FIXED_TIMESTAMP.plusHours(1))
			.withLockedBy("locker")
			.withLocked(FIXED_TIMESTAMP.plusHours(2));

		assertThat(entry.getId()).isEqualTo("je1");
		assertThat(entry.getErrandId()).isEqualTo("e1");
		assertThat(entry.getType()).isEqualTo("Journalfört meddelande");
		assertThat(entry.getHeading()).isEqualTo("Rubrik");
		assertThat(entry.getText()).isEqualTo("body");
		assertThat(entry.getEntryDateTime()).isEqualTo(ENTRY_DATE_TIME);
		assertThat(entry.getStatus()).isEqualTo("WORKING");
		assertThat(entry.getCreatedBy()).isEqualTo("carola");
		assertThat(entry.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(entry.getModifiedBy()).isEqualTo("editor");
		assertThat(entry.getModified()).isEqualTo(FIXED_TIMESTAMP.plusHours(1));
		assertThat(entry.getLockedBy()).isEqualTo("locker");
		assertThat(entry.getLocked()).isEqualTo(FIXED_TIMESTAMP.plusHours(2));
	}

	@Test
	void setters() {
		final var entry = JournalEntry.create();
		entry.setId("id");
		entry.setErrandId("eid");
		entry.setType("T");
		entry.setHeading("H");
		entry.setText("b");
		entry.setEntryDateTime(ENTRY_DATE_TIME);
		entry.setStatus("LOCKED");
		entry.setCreatedBy("a");
		entry.setCreated(FIXED_TIMESTAMP);
		entry.setModifiedBy("editor");
		entry.setModified(FIXED_TIMESTAMP.plusHours(1));
		entry.setLockedBy("locker");
		entry.setLocked(FIXED_TIMESTAMP.plusHours(2));

		assertThat(entry.getId()).isEqualTo("id");
		assertThat(entry.getErrandId()).isEqualTo("eid");
		assertThat(entry.getType()).isEqualTo("T");
		assertThat(entry.getHeading()).isEqualTo("H");
		assertThat(entry.getText()).isEqualTo("b");
		assertThat(entry.getEntryDateTime()).isEqualTo(ENTRY_DATE_TIME);
		assertThat(entry.getStatus()).isEqualTo("LOCKED");
		assertThat(entry.getCreatedBy()).isEqualTo("a");
		assertThat(entry.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(entry.getModifiedBy()).isEqualTo("editor");
		assertThat(entry.getModified()).isEqualTo(FIXED_TIMESTAMP.plusHours(1));
		assertThat(entry.getLockedBy()).isEqualTo("locker");
		assertThat(entry.getLocked()).isEqualTo(FIXED_TIMESTAMP.plusHours(2));
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(JournalEntry.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var a = JournalEntry.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withEntryDateTime(ENTRY_DATE_TIME).withStatus("WORKING").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);
		final var b = JournalEntry.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withEntryDateTime(ENTRY_DATE_TIME).withStatus("WORKING").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);
		final var c = JournalEntry.create().withId("2");
		final var d = JournalEntry.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withEntryDateTime(ENTRY_DATE_TIME).withStatus("LOCKED").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(d)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}

	@Test
	void toStringContainsFields() {
		final var entry = JournalEntry.create()
			.withId("je1")
			.withErrandId("e1")
			.withType("Journalfört meddelande")
			.withHeading("Rubrik")
			.withText("body")
			.withStatus("WORKING")
			.withCreatedBy("carola");

		assertThat(entry.toString()).contains("je1", "e1", "Journalfört meddelande", "Rubrik", "body", "WORKING", "carola");
	}
}
