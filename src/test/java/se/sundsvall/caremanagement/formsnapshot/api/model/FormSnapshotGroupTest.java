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

class FormSnapshotGroupTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		BeanMatchers.registerValueGenerator(() -> FormSnapshotField.create().withName("n" + random.nextInt()), FormSnapshotField.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FormSnapshotGroup.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var field = FormSnapshotField.create().withName("amount");
		final var group = FormSnapshotGroup.create()
			.withFields(List.of(field));

		assertThat(group.getFields()).containsExactly(field);
	}
}
