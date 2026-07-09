package se.sundsvall.caremanagement.document.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMetadataTest {

	@Test
	void builderMethods() {
		final var types = List.of(DocumentType.create().withCode("C").withDisplayName("D"));
		final var metadata = DocumentMetadata.create().withTypes(types);

		assertThat(metadata.getTypes()).isEqualTo(types);
	}

	@Test
	void setters() {
		final var types = List.of(DocumentType.create().withCode("C").withDisplayName("D"));
		final var metadata = DocumentMetadata.create();
		metadata.setTypes(types);

		assertThat(metadata.getTypes()).isEqualTo(types);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(DocumentMetadata.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
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
	void toStringContainsFields() {
		final var types = List.of(DocumentType.create().withCode("C").withDisplayName("D"));
		final var metadata = DocumentMetadata.create().withTypes(types);

		assertThat(metadata.toString()).contains("DocumentMetadata", "types");
	}
}
