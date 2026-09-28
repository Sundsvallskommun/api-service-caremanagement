package se.sundsvall.caremanagement.lifecare.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttachmentUploadTest {

	@Test
	void equalsAndHashCodeCompareContentByValue() {
		final var first = new AttachmentUpload("1", "1", "Title", "Draken", "file.pdf", new byte[] {
			1, 2, 3
		});
		final var second = new AttachmentUpload("1", "1", "Title", "Draken", "file.pdf", new byte[] {
			1, 2, 3
		});
		final var differentContent = new AttachmentUpload("1", "1", "Title", "Draken", "file.pdf", new byte[] {
			9
		});
		final var differentTitle = new AttachmentUpload("1", "1", "Other", "Draken", "file.pdf", new byte[] {
			1, 2, 3
		});

		assertThat(first)
			.isEqualTo(second).hasSameHashCodeAs(second)
			.isNotEqualTo(differentContent)
			.isNotEqualTo(differentTitle)
			.isEqualTo(first)
			.isNotEqualTo("not an attachment")
			.isNotEqualTo(null);
	}

	@Test
	void toStringSummarizesContentByLengthRatherThanBytes() {
		final var attachment = new AttachmentUpload("1", "1", "Title", "Draken", "file.pdf", new byte[] {
			1, 2, 3, 4
		});

		assertThat(attachment.toString())
			.contains("documentType=1", "documentSenderType=1", "title=Title", "senderName=Draken", "fileName=file.pdf", "content=4 bytes")
			.doesNotContain("1, 2, 3, 4");
	}

	@Test
	void toStringHandlesNullContent() {
		final var attachment = new AttachmentUpload("1", "1", "Title", "Draken", "file.pdf", null);

		assertThat(attachment.toString()).contains("content=0 bytes");
	}
}
