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

class StakeholderParameterEntityTest {

	@Test
	void testBean() {
		assertThat(StakeholderParameterEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("stakeholderEntity"),
			hasValidBeanEqualsExcluding("stakeholderEntity"),
			hasValidBeanToStringExcluding("stakeholderEntity")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = 1L;
		final var stakeholder = StakeholderEntity.create().withId(UUID.randomUUID().toString());
		final var displayName = "displayName";
		final var key = "key";
		final var values = List.of("value1", "value2");

		final var entity = StakeholderParameterEntity.create()
			.withId(id)
			.withStakeholderEntity(stakeholder)
			.withDisplayName(displayName)
			.withKey(key)
			.withValues(values);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getStakeholderEntity()).isEqualTo(stakeholder);
		assertThat(entity.getDisplayName()).isEqualTo(displayName);
		assertThat(entity.getKey()).isEqualTo(key);
		assertThat(entity.getValues()).isEqualTo(values);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(StakeholderParameterEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		assertThat(new StakeholderParameterEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}

	@Test
	void toStringHandlesNullStakeholder() {
		assertThat(StakeholderParameterEntity.create().toString()).contains("stakeholderEntity=null");
	}
}
