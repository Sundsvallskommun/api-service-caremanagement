package se.sundsvall.caremanagement.document.api.model;

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

class DocumentMetadataTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(DocumentMetadata.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var types = List.of(DocumentType.create().withCode("C").withDisplayName("D"));
		final var metadata = DocumentMetadata.create().withTypes(types);

		assertThat(metadata.getTypes()).isEqualTo(types);
	}

	@Test
	void testSetters() {
		final var types = List.of(DocumentType.create().withCode("C").withDisplayName("D"));
		final var metadata = DocumentMetadata.create();
		metadata.setTypes(types);

		assertThat(metadata.getTypes()).isEqualTo(types);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentMetadata.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentMetadata()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testEqualsAndHashCode() {
		final var types = List.of(DocumentType.create().withCode("C").withDisplayName("D"));
		final var a = DocumentMetadata.create().withTypes(types);
		final var b = DocumentMetadata.create().withTypes(types);
		final var c = DocumentMetadata.create();

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}

	@Test
	void testToStringContainsFields() {
		final var types = List.of(DocumentType.create().withCode("C").withDisplayName("D"));
		final var metadata = DocumentMetadata.create().withTypes(types);

		assertThat(metadata.toString()).contains("DocumentMetadata", "types");
	}
}
