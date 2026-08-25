package se.sundsvall.caremanagement.formsnapshot.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FormSnapshotTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		BeanMatchers.registerValueGenerator(() -> now().plusDays(random.nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotSection.create().withId("s" + random.nextInt()), FormSnapshotSection.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotAttestation.create().withLabel("A" + random.nextInt()), FormSnapshotAttestation.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FormSnapshot.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
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
