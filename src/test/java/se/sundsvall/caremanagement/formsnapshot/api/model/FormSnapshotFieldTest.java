package se.sundsvall.caremanagement.formsnapshot.api.model;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormSnapshotFieldTest {

	@BeforeAll
	static void setup() {
		FormSnapshotModelTestSupport.registerValueGenerators();
	}

	@Test
	void testBean() {
		FormSnapshotModelTestSupport.assertValidBean(FormSnapshotField.class);
	}

	@Test
	void testBuilderMethods() {
		final var answer = FormSnapshotAnswer.create().withCode("SINGLE").withDisplay("Ensamstående");
		final var option = FormSnapshotOption.create().withCode("SINGLE").withLabel("Ensamstående").withSelected(true);
		final var notice = FormSnapshotNotice.create().withLevel("WARNING").withText("Bidragsbrott");
		final var item = FormSnapshotGroup.create().withFields(List.of(FormSnapshotField.create().withName("amount").withAnswer(answer)));

		final var field = FormSnapshotField.create()
			.withName("maritalStatus")
			.withLabel("Civilstånd")
			.withInputType("RADIO")
			.withHelpText("help")
			.withInfoTexts(List.of("info"))
			.withNotices(List.of(notice))
			.withOptions(List.of(option))
			.withAnswer(answer)
			.withItems(List.of(item))
			.withRequired(true)
			.withVisible(true)
			.withCondition("always");

		assertThat(field.getName()).isEqualTo("maritalStatus");
		assertThat(field.getLabel()).isEqualTo("Civilstånd");
		assertThat(field.getInputType()).isEqualTo("RADIO");
		assertThat(field.getHelpText()).isEqualTo("help");
		assertThat(field.getInfoTexts()).containsExactly("info");
		assertThat(field.getNotices()).containsExactly(notice);
		assertThat(field.getOptions()).containsExactly(option);
		assertThat(field.getAnswer()).isEqualTo(answer);
		assertThat(field.getItems()).containsExactly(item);
		assertThat(field.isRequired()).isTrue();
		assertThat(field.isVisible()).isTrue();
		assertThat(field.getCondition()).isEqualTo("always");
	}
}
