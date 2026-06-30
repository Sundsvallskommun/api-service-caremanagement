package se.sundsvall.caremanagement.document.integration.db.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.document.integration.db.model.DocumentStatus.WORKING;

class DocumentEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void builderMethods() {
		final var documentDate = LocalDate.parse("2025-05-30");
		final var documentTime = LocalTime.of(14, 30);
		final var modified = FIXED_TIMESTAMP.plusHours(1);
		final var locked = FIXED_TIMESTAMP.plusHours(2);

		final var entity = DocumentEntity.create()
			.withId("d1")
			.withErrandId("e1")
			.withType("Brev")
			.withHeading("Rubrik")
			.withText("body")
			.withDocumentDate(documentDate)
			.withDocumentTime(documentTime)
			.withStatus(WORKING)
			.withCreatedBy("carola")
			.withCreated(FIXED_TIMESTAMP)
			.withModifiedBy("editor")
			.withModified(modified)
			.withLockedBy("locker")
			.withLocked(locked);

		assertThat(entity.getId()).isEqualTo("d1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getType()).isEqualTo("Brev");
		assertThat(entity.getHeading()).isEqualTo("Rubrik");
		assertThat(entity.getText()).isEqualTo("body");
		assertThat(entity.getDocumentDate()).isEqualTo(documentDate);
		assertThat(entity.getDocumentTime()).isEqualTo(documentTime);
		assertThat(entity.getStatus()).isEqualTo(WORKING);
		assertThat(entity.getCreatedBy()).isEqualTo("carola");
		assertThat(entity.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(entity.getModifiedBy()).isEqualTo("editor");
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getLockedBy()).isEqualTo("locker");
		assertThat(entity.getLocked()).isEqualTo(locked);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(DocumentEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentEntity()).hasAllNullFieldsOrProperties();
	}
}
