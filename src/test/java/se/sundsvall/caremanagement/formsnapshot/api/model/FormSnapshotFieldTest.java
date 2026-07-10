package se.sundsvall.caremanagement.formsnapshot.api.model;

import com.google.code.beanmatchers.BeanMatchers;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FormSnapshotFieldTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		BeanMatchers.registerValueGenerator(() -> FormSnapshotAnswer.create().withDisplay("D" + random.nextInt()), FormSnapshotAnswer.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotOption.create().withCode("C" + random.nextInt()), FormSnapshotOption.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotNotice.create().withText("T" + random.nextInt()), FormSnapshotNotice.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotGroup.create().withFields(List.of(FormSnapshotField.create().withName("g" + random.nextInt()))), FormSnapshotGroup.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FormSnapshotField.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
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
