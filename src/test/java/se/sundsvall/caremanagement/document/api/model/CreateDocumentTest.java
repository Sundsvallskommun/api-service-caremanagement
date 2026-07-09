package se.sundsvall.caremanagement.document.api.model;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateDocumentTest {
	private static final LocalDate DOCUMENT_DATE = LocalDate.parse("2025-05-30");
	private static final LocalTime DOCUMENT_TIME = LocalTime.of(14, 30);

	@Test
	void testAccessors() {
		final var request = new CreateDocument("Brev", "Rubrik", "body", DOCUMENT_DATE, DOCUMENT_TIME, "carola");

		assertThat(request.type()).isEqualTo("Brev");
		assertThat(request.heading()).isEqualTo("Rubrik");
		assertThat(request.text()).isEqualTo("body");
		assertThat(request.documentDate()).isEqualTo(DOCUMENT_DATE);
		assertThat(request.documentTime()).isEqualTo(DOCUMENT_TIME);
		assertThat(request.createdBy()).isEqualTo("carola");
	}

	@Test
	void testOptionalFieldsMayBeNull() {
		final var request = new CreateDocument("T", "H", null, DOCUMENT_DATE, null, null);

		assertThat(request.text()).isNull();
		assertThat(request.documentTime()).isNull();
		assertThat(request.createdBy()).isNull();
	}
}
