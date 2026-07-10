package se.sundsvall.caremanagement.formsnapshot.api.model;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormSnapshotTest {

	@BeforeAll
	static void setup() {
		FormSnapshotModelTestSupport.registerValueGenerators();
	}

	@Test
	void testBean() {
		FormSnapshotModelTestSupport.assertValidBean(FormSnapshot.class);
	}

	@Test
	void testBuilderMethods() {
		final var capturedAt = OffsetDateTime.parse("2026-06-24T10:15:30+02:00");
		final var section = FormSnapshotSection.create().withId("household");
		final var attestation = FormSnapshotAttestation.create().withLabel("Jag intygar");

		final var snapshot = FormSnapshot.create()
			.withSchemaVersion("form-snapshot/1")
			.withFormDefinitionVersion("eb-2026.06-r3")
			.withTypeSlug("financial-assistance-new")
			.withLocale("sv-SE")
			.withCapturedAt(capturedAt)
			.withTitle("Ansökan")
			.withSections(List.of(section))
			.withAttestation(attestation);

		assertThat(snapshot.getSchemaVersion()).isEqualTo("form-snapshot/1");
		assertThat(snapshot.getFormDefinitionVersion()).isEqualTo("eb-2026.06-r3");
		assertThat(snapshot.getTypeSlug()).isEqualTo("financial-assistance-new");
		assertThat(snapshot.getLocale()).isEqualTo("sv-SE");
		assertThat(snapshot.getCapturedAt()).isEqualTo(capturedAt);
		assertThat(snapshot.getTitle()).isEqualTo("Ansökan");
		assertThat(snapshot.getSections()).containsExactly(section);
		assertThat(snapshot.getAttestation()).isEqualTo(attestation);
	}
}
