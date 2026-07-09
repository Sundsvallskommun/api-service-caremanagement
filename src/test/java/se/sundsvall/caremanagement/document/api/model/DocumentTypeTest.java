package se.sundsvall.caremanagement.document.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class DocumentTypeTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(DocumentType.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var type = DocumentType.create().withCode("LETTER").withDisplayName("Brev");

		assertThat(type.getCode()).isEqualTo("LETTER");
		assertThat(type.getDisplayName()).isEqualTo("Brev");
	}

	@Test
	void testSetters() {
		final var type = DocumentType.create();
		type.setCode("FORM");
		type.setDisplayName("Blankett");

		assertThat(type.getCode()).isEqualTo("FORM");
		assertThat(type.getDisplayName()).isEqualTo("Blankett");
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentType.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentType()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testEqualsHashCodeAndToString() {
		final var a = DocumentType.create().withCode("C").withDisplayName("D");
		final var b = DocumentType.create().withCode("C").withDisplayName("D");
		final var c = DocumentType.create().withCode("X").withDisplayName("D");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string")
			.hasToString("DocumentType{code='C', displayName='D'}");
	}
}
