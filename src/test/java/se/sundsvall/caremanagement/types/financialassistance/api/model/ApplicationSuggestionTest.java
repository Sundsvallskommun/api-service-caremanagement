package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class ApplicationSuggestionTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(ApplicationSuggestion.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var typeSlug = "financial-assistance-renewal";
		final var applicationType = "RENEWAL";
		final var periodMonth = 7;
		final var periodYear = 2026;
		final var recommended = true;
		final var label = "Återansökan för juli 2026";
		final var description = "du har ansökt tidigare och inte haft ett längre uppehåll";

		final var result = ApplicationSuggestion.create()
			.withTypeSlug(typeSlug)
			.withApplicationType(applicationType)
			.withPeriodMonth(periodMonth)
			.withPeriodYear(periodYear)
			.withRecommended(recommended)
			.withLabel(label)
			.withDescription(description);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getTypeSlug()).isEqualTo(typeSlug);
		assertThat(result.getApplicationType()).isEqualTo(applicationType);
		assertThat(result.getPeriodMonth()).isEqualTo(periodMonth);
		assertThat(result.getPeriodYear()).isEqualTo(periodYear);
		assertThat(result.isRecommended()).isEqualTo(recommended);
		assertThat(result.getLabel()).isEqualTo(label);
		assertThat(result.getDescription()).isEqualTo(description);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ApplicationSuggestion.create()).hasAllNullFieldsOrPropertiesExcept("recommended");
	}

}
