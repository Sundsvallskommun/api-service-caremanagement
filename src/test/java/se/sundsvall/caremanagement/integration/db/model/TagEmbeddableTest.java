package se.sundsvall.caremanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class TagEmbeddableTest {

	@Test
	void testBean() {
		assertThat(TagEmbeddable.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var key = "key";
		final var value = "value";

		final var tag = TagEmbeddable.create()
			.withKey(key)
			.withValue(value);

		assertThat(tag).hasNoNullFieldsOrProperties();
		assertThat(tag.getKey()).isEqualTo(key);
		assertThat(tag.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(TagEmbeddable.create()).hasAllNullFieldsOrProperties();
		assertThat(new TagEmbeddable()).hasAllNullFieldsOrProperties();
	}
}
