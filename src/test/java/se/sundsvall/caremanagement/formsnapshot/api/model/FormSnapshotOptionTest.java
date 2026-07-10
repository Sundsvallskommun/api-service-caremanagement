package se.sundsvall.caremanagement.formsnapshot.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormSnapshotOptionTest {

	@Test
	void testBean() {
		FormSnapshotModelTestSupport.assertValidBean(FormSnapshotOption.class);
	}

	@Test
	void testBuilderMethods() {
		final var option = FormSnapshotOption.create()
			.withCode("SINGLE")
			.withLabel("Ensamstående")
			.withSelected(true);

		assertThat(option.getCode()).isEqualTo("SINGLE");
		assertThat(option.getLabel()).isEqualTo("Ensamstående");
		assertThat(option.isSelected()).isTrue();
	}
}
