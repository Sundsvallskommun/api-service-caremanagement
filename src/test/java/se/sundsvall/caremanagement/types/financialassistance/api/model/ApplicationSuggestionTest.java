package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ApplicationSuggestionTest {

	@Test
	void testBean() {
		assertThat(ApplicationSuggestion.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var suggestion = ApplicationSuggestion.create()
			.withTypeSlug("financial-assistance-renewal")
			.withApplicationType("RENEWAL")
			.withPeriodMonth(7)
			.withPeriodYear(2026)
			.withRecommended(true)
			.withLabel("Renewal for July 2026");

		assertThat(suggestion.getTypeSlug()).isEqualTo("financial-assistance-renewal");
		assertThat(suggestion.getApplicationType()).isEqualTo("RENEWAL");
		assertThat(suggestion.getPeriodMonth()).isEqualTo(7);
		assertThat(suggestion.getPeriodYear()).isEqualTo(2026);
		assertThat(suggestion.isRecommended()).isTrue();
		assertThat(suggestion.getLabel()).isEqualTo("Renewal for July 2026");
	}
}
