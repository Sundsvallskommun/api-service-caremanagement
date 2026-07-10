package se.sundsvall.caremanagement.formsnapshot.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormSnapshotAnswerTest {

	@Test
	void testBean() {
		FormSnapshotModelTestSupport.assertValidBean(FormSnapshotAnswer.class);
	}

	@Test
	void testBuilderMethods() {
		final var answer = FormSnapshotAnswer.create()
			.withCode("SINGLE")
			.withValue("single")
			.withDisplay("Ensamstående");

		assertThat(answer.getCode()).isEqualTo("SINGLE");
		assertThat(answer.getValue()).isEqualTo("single");
		assertThat(answer.getDisplay()).isEqualTo("Ensamstående");
	}
}
