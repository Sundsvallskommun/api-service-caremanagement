package se.sundsvall.caremanagement.journal.api.model;

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

class JournalEntryMetadataTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(JournalEntryMetadata.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var types = List.of(JournalEntryType.create().withCode("C").withDisplayName("D"));
		final var metadata = JournalEntryMetadata.create().withTypes(types);

		assertThat(metadata.getTypes()).isEqualTo(types);
	}

	@Test
	void testSetters() {
		final var types = List.of(JournalEntryType.create().withCode("C").withDisplayName("D"));
		final var metadata = JournalEntryMetadata.create();
		metadata.setTypes(types);

		assertThat(metadata.getTypes()).isEqualTo(types);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(JournalEntryMetadata.create()).hasAllNullFieldsOrProperties();
		assertThat(new JournalEntryMetadata()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testEqualsAndHashCode() {
		final var types = List.of(JournalEntryType.create().withCode("C").withDisplayName("D"));
		final var a = JournalEntryMetadata.create().withTypes(types);
		final var b = JournalEntryMetadata.create().withTypes(types);
		final var c = JournalEntryMetadata.create();

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}

	@Test
	void testToStringContainsFields() {
		final var types = List.of(JournalEntryType.create().withCode("C").withDisplayName("D"));
		final var metadata = JournalEntryMetadata.create().withTypes(types);

		assertThat(metadata.toString()).contains("JournalEntryMetadata", "types");
	}
}
