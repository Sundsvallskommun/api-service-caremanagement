package se.sundsvall.caremanagement.formsnapshot.api.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormSnapshotAttestationTest {

	@BeforeAll
	static void setup() {
		FormSnapshotModelTestSupport.registerValueGenerators();
	}

	@Test
	void testBean() {
		FormSnapshotModelTestSupport.assertValidBean(FormSnapshotAttestation.class);
	}

	@Test
	void testBuilderMethods() {
		final var answer = FormSnapshotAnswer.create().withDisplay("Ja");
		final var attestation = FormSnapshotAttestation.create()
			.withLabel("Jag intygar")
			.withAnswer(answer);

		assertThat(attestation.getLabel()).isEqualTo("Jag intygar");
		assertThat(attestation.getAnswer()).isEqualTo(answer);
	}
}
