package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NormberakningResponseTest {

	@Test
	void testBean() {
		assertThat(NormberakningResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var response = NormberakningResponse.create()
			.withCalculationId(4711)
			.withUnhandledIncomes(List.of("Bostadstillägg (NOT_ON_WHITELIST)"))
			.withChangeWarnings(List.of("Bostadsbidrag: -23% (jämförelse 2400 → kontroll 1850)"));

		assertThat(response.getCalculationId()).isEqualTo(4711);
		assertThat(response.getUnhandledIncomes()).containsExactly("Bostadstillägg (NOT_ON_WHITELIST)");
		assertThat(response.getChangeWarnings()).containsExactly("Bostadsbidrag: -23% (jämförelse 2400 → kontroll 1850)");
	}

	@Test
	void createReturnsEmptyInstance() {
		final var response = NormberakningResponse.create();

		assertThat(response.getCalculationId()).isNull();
		assertThat(response.getUnhandledIncomes()).isEmpty();
		assertThat(response.getChangeWarnings()).isEmpty();
	}
}
