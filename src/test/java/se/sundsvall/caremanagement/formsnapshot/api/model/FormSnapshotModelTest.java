package se.sundsvall.caremanagement.formsnapshot.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class FormSnapshotModelTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		BeanMatchers.registerValueGenerator(() -> now().plusDays(random.nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotAnswer.create().withDisplay("D" + random.nextInt()), FormSnapshotAnswer.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotOption.create().withCode("C" + random.nextInt()), FormSnapshotOption.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotNotice.create().withText("T" + random.nextInt()), FormSnapshotNotice.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotField.create().withName("n" + random.nextInt()), FormSnapshotField.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotGroup.create().withFields(List.of(FormSnapshotField.create().withName("g" + random.nextInt()))), FormSnapshotGroup.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotSection.create().withId("s" + random.nextInt()), FormSnapshotSection.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotAttestation.create().withLabel("A" + random.nextInt()), FormSnapshotAttestation.class);
	}

	@Test
	void beansAreWellFormed() {
		for (final var type : List.of(FormSnapshotAnswer.class, FormSnapshotOption.class, FormSnapshotNotice.class,
			FormSnapshotField.class, FormSnapshotGroup.class, FormSnapshotSection.class, FormSnapshotAttestation.class, FormSnapshot.class)) {
			assertThat(type, allOf(
				hasValidBeanConstructor(),
				hasValidGettersAndSetters(),
				hasValidBeanHashCode(),
				hasValidBeanEquals(),
				hasValidBeanToString()));
		}
	}

	@Test
	void builderMethods() {
		final var capturedAt = OffsetDateTime.parse("2026-06-24T10:15:30+02:00");
		final var answer = FormSnapshotAnswer.create().withCode("SINGLE").withDisplay("Ensamstående");
		final var option = FormSnapshotOption.create().withCode("SINGLE").withLabel("Ensamstående").withSelected(true);
		final var notice = FormSnapshotNotice.create().withLevel("WARNING").withText("Bidragsbrott");
		final var field = FormSnapshotField.create()
			.withName("maritalStatus").withLabel("Civilstånd").withInputType("RADIO")
			.withHelpText("help").withInfoTexts(List.of("info")).withNotices(List.of(notice))
			.withOptions(List.of(option)).withAnswer(answer).withRequired(true).withVisible(true).withCondition("always");
		final var groupField = FormSnapshotField.create().withName("incomes").withInputType("REPEATING_GROUP")
			.withItems(List.of(FormSnapshotGroup.create().withFields(List.of(FormSnapshotField.create().withName("amount").withAnswer(answer)))));
		final var section = FormSnapshotSection.create().withId("household").withTitle("Hushåll")
			.withDescription("desc").withVisible(true).withFields(List.of(field, groupField));
		final var attestation = FormSnapshotAttestation.create().withLabel("Jag intygar").withAnswer(answer);
		final var snapshot = FormSnapshot.create()
			.withSchemaVersion("form-snapshot/1").withFormDefinitionVersion("eb-2026.06-r3")
			.withTypeSlug("financial-assistance-new").withLocale("sv-SE").withCapturedAt(capturedAt)
			.withTitle("Ansökan").withSections(List.of(section)).withAttestation(attestation);

		assertThat(snapshot.getSchemaVersion()).isEqualTo("form-snapshot/1");
		assertThat(snapshot.getSections()).containsExactly(section);
		assertThat(snapshot.getAttestation().getAnswer()).isEqualTo(answer);
		assertThat(field.getOptions()).containsExactly(option);
		assertThat(field.getNotices()).containsExactly(notice);
		assertThat(groupField.getItems().getFirst().getFields().getFirst().getName()).isEqualTo("amount");
		assertThat(option.isSelected()).isTrue();
	}
}
