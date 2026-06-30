package se.sundsvall.caremanagement.document.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LockDocumentTest {

	@Test
	void accessors() {
		assertThat(new LockDocument("carola").lockedBy()).isEqualTo("carola");
	}

	@Test
	void lockedByMayBeNull() {
		assertThat(new LockDocument(null).lockedBy()).isNull();
	}
}
