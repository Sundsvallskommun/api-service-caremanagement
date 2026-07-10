package se.sundsvall.caremanagement.core.integration.db.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class ErrandNumberSequenceEntityTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(ErrandNumberSequenceEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = 1L;
		final var municipalityId = "2281";
		final var namespace = "FINANCIAL_ASSISTANCE";
		final var sequenceYear = 2026;
		final var sequenceMonth = 6;
		final var currentValue = 42L;

		final var entity = ErrandNumberSequenceEntity.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withSequenceYear(sequenceYear)
			.withSequenceMonth(sequenceMonth)
			.withCurrentValue(currentValue);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getNamespace()).isEqualTo(namespace);
		assertThat(entity.getSequenceYear()).isEqualTo(sequenceYear);
		assertThat(entity.getSequenceMonth()).isEqualTo(sequenceMonth);
		assertThat(entity.getCurrentValue()).isEqualTo(currentValue);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandNumberSequenceEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandNumberSequenceEntity()).hasAllNullFieldsOrProperties();
	}
}
