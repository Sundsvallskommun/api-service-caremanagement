package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class RenewalPrefillTest {

	private static final List<PrefilledChild> CHILDREN = List.of(
		PrefilledChild.create().withPartyId("201801012380").withName("Kid Andersson"));

	@Test
	void testBean() {
		MatcherAssert.assertThat(RenewalPrefill.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var prefill = RenewalPrefill.create()
			.withChildren(CHILDREN)
			.withLifecareChecked(true);

		assertThat(prefill.getChildren()).isEqualTo(CHILDREN);
		assertThat(prefill.isLifecareChecked()).isTrue();
		assertThat(prefill).hasNoNullFieldsOrProperties();
	}

	@Test
	void testSettersWork() {
		final var prefill = RenewalPrefill.create();
		prefill.setChildren(CHILDREN);
		prefill.setLifecareChecked(false);

		assertThat(prefill.getChildren()).isEqualTo(CHILDREN);
		assertThat(prefill.isLifecareChecked()).isFalse();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(RenewalPrefill.create()).hasAllNullFieldsOrPropertiesExcept("lifecareChecked");
		assertThat(new RenewalPrefill()).hasAllNullFieldsOrPropertiesExcept("lifecareChecked");
	}

	@Test
	void testEqualsAndHashCode() {
		final var a = RenewalPrefill.create().withChildren(CHILDREN).withLifecareChecked(true);
		final var b = RenewalPrefill.create().withChildren(CHILDREN).withLifecareChecked(true);
		final var c = RenewalPrefill.create().withLifecareChecked(false);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}
}
