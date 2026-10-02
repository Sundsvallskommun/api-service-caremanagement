package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class LifecareRecordsTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareRecords.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = LifecareRecords.create()
			.withJournalNotes(List.of(LifecareRecord.create()))
			.withDocuments(List.of(LifecareRecord.create()));

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getJournalNotes()).isEqualTo(List.of(LifecareRecord.create()));
		assertThat(result.getDocuments()).isEqualTo(List.of(LifecareRecord.create()));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareRecords.create()).hasAllNullFieldsOrProperties();
	}
}
