package se.sundsvall.caremanagement.core.integration.db.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ErrandNumberSequenceEntityTest {

	@Test
	void testBean() {
		assertThat(ErrandNumberSequenceEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = 1L;
		final var municipalityId = "2281";
		final var namespace = "FINANCIAL_ASSISTANCE";
		final var sequenceYear = 2026;
		final var currentValue = 42L;

		final var entity = ErrandNumberSequenceEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withSequenceYear(sequenceYear)
			.withCurrentValue(currentValue);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getSequenceYear()).isEqualTo(sequenceYear);
		assertThat(entity.getCurrentValue()).isEqualTo(currentValue);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ErrandNumberSequenceEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandNumberSequenceEntity()).hasAllNullFieldsOrProperties();
	}
}
