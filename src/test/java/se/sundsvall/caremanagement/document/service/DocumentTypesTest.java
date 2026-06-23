package se.sundsvall.caremanagement.document.service;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.document.api.model.DocumentType;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTypesTest {

	@Test
	void catalogueIsNonEmptyWithUniqueCodes() {
		assertThat(DocumentTypes.TYPES).isNotEmpty();
		assertThat(DocumentTypes.TYPES).extracting(DocumentType::getCode).doesNotContainNull().doesNotHaveDuplicates();
		assertThat(DocumentTypes.TYPES).extracting(DocumentType::getDisplayName).doesNotContainNull().doesNotHaveDuplicates();
	}

	@Test
	void catalogueContainsCommonDocumentTypes() {
		assertThat(DocumentTypes.TYPES)
			.extracting(DocumentType::getDisplayName)
			.contains("Brev", "Dokument");
	}

	@Test
	void metadataWrapsTheCatalogue() {
		assertThat(DocumentTypes.metadata().getTypes()).isEqualTo(DocumentTypes.TYPES);
	}
}
