package se.sundsvall.caremanagement.journal.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LockJournalEntryTest {

	@Test
	void testAccessors() {
		assertThat(new LockJournalEntry("carola").lockedBy()).isEqualTo("carola");
	}

	@Test
	void testLockedByMayBeNull() {
		assertThat(new LockJournalEntry(null).lockedBy()).isNull();
	}
}
