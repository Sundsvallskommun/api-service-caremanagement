package se.sundsvall.caremanagement.document.api.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateDocumentTest {
	private static final OffsetDateTime DOCUMENT_DATE_TIME = OffsetDateTime.parse("2025-05-30T14:30:00+02:00");

	@Test
	void testAccessors() {
		final var request = new CreateDocument("Brev", "Rubrik", "body", DOCUMENT_DATE_TIME, "carola");

		assertThat(request.type()).isEqualTo("Brev");
		assertThat(request.heading()).isEqualTo("Rubrik");
		assertThat(request.text()).isEqualTo("body");
		assertThat(request.documentDateTime()).isEqualTo(DOCUMENT_DATE_TIME);
		assertThat(request.createdBy()).isEqualTo("carola");
	}

	@Test
	void testOptionalFieldsMayBeNull() {
		final var request = new CreateDocument("T", "H", null, DOCUMENT_DATE_TIME, null);

		assertThat(request.text()).isNull();
		assertThat(request.createdBy()).isNull();
	}
}
