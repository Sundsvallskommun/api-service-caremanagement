package se.sundsvall.caremanagement.formsnapshot.api.model;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormSnapshotSectionTest {

	@BeforeAll
	static void setup() {
		FormSnapshotModelTestSupport.registerValueGenerators();
	}

	@Test
	void testBean() {
		FormSnapshotModelTestSupport.assertValidBean(FormSnapshotSection.class);
	}

	@Test
	void testBuilderMethods() {
		final var field = FormSnapshotField.create().withName("maritalStatus");
		final var section = FormSnapshotSection.create()
			.withId("household")
			.withTitle("Hushåll")
			.withDescription("desc")
			.withVisible(true)
			.withFields(List.of(field));

		assertThat(section.getId()).isEqualTo("household");
		assertThat(section.getTitle()).isEqualTo("Hushåll");
		assertThat(section.getDescription()).isEqualTo("desc");
		assertThat(section.isVisible()).isTrue();
		assertThat(section.getFields()).containsExactly(field);
	}
}
