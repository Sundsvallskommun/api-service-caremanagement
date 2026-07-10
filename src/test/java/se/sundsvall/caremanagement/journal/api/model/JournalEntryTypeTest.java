package se.sundsvall.caremanagement.journal.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class JournalEntryTypeTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(JournalEntryType.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var type = JournalEntryType.create().withCode("JOURNALED_MESSAGE").withDisplayName("Journalfört meddelande");

		assertThat(type.getCode()).isEqualTo("JOURNALED_MESSAGE");
		assertThat(type.getDisplayName()).isEqualTo("Journalfört meddelande");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(JournalEntryType.create()).hasAllNullFieldsOrProperties();
		assertThat(new JournalEntryType()).hasAllNullFieldsOrProperties();
	}

}
