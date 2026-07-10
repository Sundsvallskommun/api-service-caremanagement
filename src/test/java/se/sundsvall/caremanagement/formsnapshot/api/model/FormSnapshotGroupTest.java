package se.sundsvall.caremanagement.formsnapshot.api.model;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormSnapshotGroupTest {

	@BeforeAll
	static void setup() {
		FormSnapshotModelTestSupport.registerValueGenerators();
	}

	@Test
	void testBean() {
		FormSnapshotModelTestSupport.assertValidBean(FormSnapshotGroup.class);
	}

	@Test
	void testBuilderMethods() {
		final var field = FormSnapshotField.create().withName("amount");
		final var group = FormSnapshotGroup.create()
			.withFields(List.of(field));

		assertThat(group.getFields()).containsExactly(field);
	}
}
