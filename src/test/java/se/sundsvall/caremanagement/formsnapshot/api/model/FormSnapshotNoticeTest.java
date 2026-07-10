package se.sundsvall.caremanagement.formsnapshot.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormSnapshotNoticeTest {

	@Test
	void testBean() {
		FormSnapshotModelTestSupport.assertValidBean(FormSnapshotNotice.class);
	}

	@Test
	void testBuilderMethods() {
		final var notice = FormSnapshotNotice.create()
			.withLevel("WARNING")
			.withText("Bidragsbrott");

		assertThat(notice.getLevel()).isEqualTo("WARNING");
		assertThat(notice.getText()).isEqualTo("Bidragsbrott");
	}
}
