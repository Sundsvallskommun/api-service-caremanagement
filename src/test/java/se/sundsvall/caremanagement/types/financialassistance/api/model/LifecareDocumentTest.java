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

class LifecareDocumentTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareDocument.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var document = LifecareDocument.create()
			.withId("a3f1c2...")
			.withTitle("Beslut försörjningsstöd juni")
			.withDate("2026-06-01")
			.withDocumentType("Beslut")
			.withOwnerId("9900")
			.withOwnerType("Decision");

		assertThat(document.getId()).isEqualTo("a3f1c2...");
		assertThat(document.getTitle()).isEqualTo("Beslut försörjningsstöd juni");
		assertThat(document.getDate()).isEqualTo("2026-06-01");
		assertThat(document.getDocumentType()).isEqualTo("Beslut");
		assertThat(document.getOwnerId()).isEqualTo("9900");
		assertThat(document.getOwnerType()).isEqualTo("Decision");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareDocument.create()).hasAllNullFieldsOrProperties();
	}
}
