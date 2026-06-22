package se.sundsvall.caremanagement.journal.integration.db.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.journal.integration.db.model.JournalEntryStatus.WORKING;

class JournalEntryEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void builderMethods() {
		final var entryDate = LocalDate.parse("2025-05-30");
		final var entryTime = LocalTime.of(14, 30);
		final var modified = FIXED_TIMESTAMP.plusHours(1);
		final var locked = FIXED_TIMESTAMP.plusHours(2);

		final var entity = JournalEntryEntity.create()
			.withId("je1")
			.withErrandId("e1")
			.withType("Journalfört meddelande")
			.withHeading("Rubrik")
			.withText("body")
			.withEntryDate(entryDate)
			.withEntryTime(entryTime)
			.withStatus(WORKING)
			.withCreatedBy("carola")
			.withCreated(FIXED_TIMESTAMP)
			.withModifiedBy("editor")
			.withModified(modified)
			.withLockedBy("locker")
			.withLocked(locked);

		assertThat(entity.getId()).isEqualTo("je1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getType()).isEqualTo("Journalfört meddelande");
		assertThat(entity.getHeading()).isEqualTo("Rubrik");
		assertThat(entity.getText()).isEqualTo("body");
		assertThat(entity.getEntryDate()).isEqualTo(entryDate);
		assertThat(entity.getEntryTime()).isEqualTo(entryTime);
		assertThat(entity.getStatus()).isEqualTo(WORKING);
		assertThat(entity.getCreatedBy()).isEqualTo("carola");
		assertThat(entity.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(entity.getModifiedBy()).isEqualTo("editor");
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getLockedBy()).isEqualTo("locker");
		assertThat(entity.getLocked()).isEqualTo(locked);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(JournalEntryEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new JournalEntryEntity()).hasAllNullFieldsOrProperties();
	}
}
