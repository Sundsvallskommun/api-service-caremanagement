package se.sundsvall.caremanagement.document.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LockDocumentTest {

	@Test
	void testAccessors() {
		assertThat(new LockDocument("carola").lockedBy()).isEqualTo("carola");
	}

	@Test
	void testLockedByMayBeNull() {
		assertThat(new LockDocument(null).lockedBy()).isNull();
	}
}
