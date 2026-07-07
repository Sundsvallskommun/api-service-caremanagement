package se.sundsvall.caremanagement.document.api.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final LocalDate DOCUMENT_DATE = LocalDate.parse("2025-05-30");
	private static final LocalTime DOCUMENT_TIME = LocalTime.of(14, 30);

	@Test
	void builderMethods() {
		final var document = Document.create()
			.withId("d1")
			.withErrandId("e1")
			.withType("Brev")
			.withHeading("Rubrik")
			.withText("body")
			.withDocumentDate(DOCUMENT_DATE)
			.withDocumentTime(DOCUMENT_TIME)
			.withStatus("WORKING")
			.withCreatedBy("carola")
			.withCreated(FIXED_TIMESTAMP)
			.withModifiedBy("editor")
			.withModified(FIXED_TIMESTAMP.plusHours(1))
			.withLockedBy("locker")
			.withLocked(FIXED_TIMESTAMP.plusHours(2));

		assertThat(document.getId()).isEqualTo("d1");
		assertThat(document.getErrandId()).isEqualTo("e1");
		assertThat(document.getType()).isEqualTo("Brev");
		assertThat(document.getHeading()).isEqualTo("Rubrik");
		assertThat(document.getText()).isEqualTo("body");
		assertThat(document.getDocumentDate()).isEqualTo(DOCUMENT_DATE);
		assertThat(document.getDocumentTime()).isEqualTo(DOCUMENT_TIME);
		assertThat(document.getStatus()).isEqualTo("WORKING");
		assertThat(document.getCreatedBy()).isEqualTo("carola");
		assertThat(document.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(document.getModifiedBy()).isEqualTo("editor");
		assertThat(document.getModified()).isEqualTo(FIXED_TIMESTAMP.plusHours(1));
		assertThat(document.getLockedBy()).isEqualTo("locker");
		assertThat(document.getLocked()).isEqualTo(FIXED_TIMESTAMP.plusHours(2));
	}

	@Test
	void setters() {
		final var document = Document.create();
		document.setId("id");
		document.setErrandId("eid");
		document.setType("T");
		document.setHeading("H");
		document.setText("b");
		document.setDocumentDate(DOCUMENT_DATE);
		document.setDocumentTime(DOCUMENT_TIME);
		document.setStatus("LOCKED");
		document.setCreatedBy("a");
		document.setCreated(FIXED_TIMESTAMP);
		document.setModifiedBy("editor");
		document.setModified(FIXED_TIMESTAMP.plusHours(1));
		document.setLockedBy("locker");
		document.setLocked(FIXED_TIMESTAMP.plusHours(2));

		assertThat(document.getId()).isEqualTo("id");
		assertThat(document.getErrandId()).isEqualTo("eid");
		assertThat(document.getType()).isEqualTo("T");
		assertThat(document.getHeading()).isEqualTo("H");
		assertThat(document.getText()).isEqualTo("b");
		assertThat(document.getDocumentDate()).isEqualTo(DOCUMENT_DATE);
		assertThat(document.getDocumentTime()).isEqualTo(DOCUMENT_TIME);
		assertThat(document.getStatus()).isEqualTo("LOCKED");
		assertThat(document.getCreatedBy()).isEqualTo("a");
		assertThat(document.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(document.getModifiedBy()).isEqualTo("editor");
		assertThat(document.getModified()).isEqualTo(FIXED_TIMESTAMP.plusHours(1));
		assertThat(document.getLockedBy()).isEqualTo("locker");
		assertThat(document.getLocked()).isEqualTo(FIXED_TIMESTAMP.plusHours(2));
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(Document.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var a = Document.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withDocumentDate(DOCUMENT_DATE).withDocumentTime(DOCUMENT_TIME).withStatus("WORKING").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);
		final var b = Document.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withDocumentDate(DOCUMENT_DATE).withDocumentTime(DOCUMENT_TIME).withStatus("WORKING").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);
		final var c = Document.create().withId("2");
		final var d = Document.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withDocumentDate(DOCUMENT_DATE).withDocumentTime(DOCUMENT_TIME).withStatus("LOCKED").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(d)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}
}
