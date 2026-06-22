package se.sundsvall.caremanagement.journal.service;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.journal.api.model.JournalEntryType;

import static org.assertj.core.api.Assertions.assertThat;

class JournalEntryTypesTest {

	@Test
	void catalogueIsNonEmptyWithUniqueCodes() {
		assertThat(JournalEntryTypes.TYPES).isNotEmpty();
		assertThat(JournalEntryTypes.TYPES).extracting(JournalEntryType::getCode).doesNotContainNull().doesNotHaveDuplicates();
		assertThat(JournalEntryTypes.TYPES).extracting(JournalEntryType::getDisplayName).doesNotContainNull().doesNotHaveDuplicates();
	}

	@Test
	void catalogueContainsTheConfirmedLifecareType() {
		assertThat(JournalEntryTypes.TYPES)
			.extracting(JournalEntryType::getDisplayName)
			.contains("Journalfört meddelande");
	}

	@Test
	void metadataWrapsTheCatalogue() {
		assertThat(JournalEntryTypes.metadata().getTypes()).isEqualTo(JournalEntryTypes.TYPES);
	}
}
