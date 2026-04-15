package se.sundsvall.caremanagement.integration.db.model;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ParameterEntityTest {

	@Test
	void testBean() {
		assertThat(ParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("errandEntity"),
			hasValidBeanEqualsExcluding("errandEntity"),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = UUID.randomUUID().toString();
		final var errand = ErrandEntity.create().withId("errand-id");
		final var displayName = "displayName";
		final var parameterGroup = "group";
		final var key = "key";
		final var values = List.of("v1", "v2");

		final var entity = ParameterEntity.create()
			.withId(id)
			.withErrandEntity(errand)
			.withDisplayName(displayName)
			.withParameterGroup(parameterGroup)
			.withKey(key)
			.withValues(values);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandEntity()).isEqualTo(errand);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getParameterGroup()).isEqualTo(parameterGroup);
		assertThat(entity.getKey()).isEqualTo(key);
		assertThat(entity.getValues()).isEqualTo(values);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ParameterEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ParameterEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void toStringHandlesNullErrand() {
		assertThat(ParameterEntity.create().toString()).contains("errandEntity=null");
	}
}
