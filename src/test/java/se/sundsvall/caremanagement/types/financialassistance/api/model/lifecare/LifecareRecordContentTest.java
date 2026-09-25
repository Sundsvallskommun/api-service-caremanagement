package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class LifecareRecordContentTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareRecordContent.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = LifecareRecordContent.create()
			.withId("135")
			.withCategory("JOURNAL_NOTE")
			.withTitle("Journalanteckning")
			.withContent("<p>Hej</p>")
			.withOccurenceDate("2026-09-23")
			.withTime("09:02")
			.withEditable(true);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo("135");
		assertThat(result.getCategory()).isEqualTo("JOURNAL_NOTE");
		assertThat(result.getTitle()).isEqualTo("Journalanteckning");
		assertThat(result.getContent()).isEqualTo("<p>Hej</p>");
		assertThat(result.getOccurenceDate()).isEqualTo("2026-09-23");
		assertThat(result.getTime()).isEqualTo("09:02");
		assertThat(result.getEditable()).isEqualTo(true);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareRecordContent.create()).hasAllNullFieldsOrProperties();
	}
}
