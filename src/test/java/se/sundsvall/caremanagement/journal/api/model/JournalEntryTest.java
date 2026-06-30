package se.sundsvall.caremanagement.journal.api.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JournalEntryTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final LocalDate ENTRY_DATE = LocalDate.parse("2025-05-30");
	private static final LocalTime ENTRY_TIME = LocalTime.of(14, 30);

	@Test
	void builderMethods() {
		final var entry = JournalEntry.create()
			.withId("je1")
			.withErrandId("e1")
			.withType("Journalfört meddelande")
			.withHeading("Rubrik")
			.withText("body")
			.withEntryDate(ENTRY_DATE)
			.withEntryTime(ENTRY_TIME)
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
		assertThat(entry.getEntryDate()).isEqualTo(ENTRY_DATE);
		assertThat(entry.getEntryTime()).isEqualTo(ENTRY_TIME);
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
		entry.setEntryDate(ENTRY_DATE);
		entry.setEntryTime(ENTRY_TIME);
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
		assertThat(entry.getEntryDate()).isEqualTo(ENTRY_DATE);
		assertThat(entry.getEntryTime()).isEqualTo(ENTRY_TIME);
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
			.withEntryDate(ENTRY_DATE).withEntryTime(ENTRY_TIME).withStatus("WORKING").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);
		final var b = JournalEntry.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withEntryDate(ENTRY_DATE).withEntryTime(ENTRY_TIME).withStatus("WORKING").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);
		final var c = JournalEntry.create().withId("2");
		final var d = JournalEntry.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withEntryDate(ENTRY_DATE).withEntryTime(ENTRY_TIME).withStatus("LOCKED").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(d);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}
}
