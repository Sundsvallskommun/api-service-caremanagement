package se.sundsvall.caremanagement.document.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTypeTest {

	@Test
	void builderMethods() {
		final var type = DocumentType.create().withCode("LETTER").withDisplayName("Brev");

		assertThat(type.getCode()).isEqualTo("LETTER");
		assertThat(type.getDisplayName()).isEqualTo("Brev");
	}

	@Test
	void setters() {
		final var type = DocumentType.create();
		type.setCode("FORM");
		type.setDisplayName("Blankett");

		assertThat(type.getCode()).isEqualTo("FORM");
		assertThat(type.getDisplayName()).isEqualTo("Blankett");
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(DocumentType.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsHashCodeAndToString() {
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
