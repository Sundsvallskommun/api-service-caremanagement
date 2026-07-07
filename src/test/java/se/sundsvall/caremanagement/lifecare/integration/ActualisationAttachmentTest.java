package se.sundsvall.caremanagement.lifecare.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActualisationAttachmentTest {

	private static ActualisationAttachment base() {
		return new ActualisationAttachment("DOC", "SENDER", "Title", "Sender Name", "intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});
	}

	@Test
	void equalsCoversAllBranches() {
		final var base = base();

		assertThat(base.equals(base)).isTrue();
		assertThat(base.equals(base())).isTrue();
		assertThat(base.equals(null)).isFalse();
		assertThat(base.equals("not an ActualisationAttachment")).isFalse();

		assertThat(base.equals(new ActualisationAttachment("OTHER", "SENDER", "Title", "Sender Name", "intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new ActualisationAttachment("DOC", "OTHER", "Title", "Sender Name", "intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new ActualisationAttachment("DOC", "SENDER", "Other", "Sender Name", "intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new ActualisationAttachment("DOC", "SENDER", "Title", "Other Name", "intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new ActualisationAttachment("DOC", "SENDER", "Title", "Sender Name", "other.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new ActualisationAttachment("DOC", "SENDER", "Title", "Sender Name", "intyg.pdf", "image/png", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new ActualisationAttachment("DOC", "SENDER", "Title", "Sender Name", "intyg.pdf", "application/pdf", new byte[] {
			9
		}))).isFalse();
	}

	@Test
	void hashCodeAndToString() {
		final var base = base();

		assertThat(base).hasSameHashCodeAs(base())
			.hasToString("ActualisationAttachment[documentType=DOC, documentSenderType=SENDER, title=Title, senderName=Sender Name, fileName=intyg.pdf, mimeType=application/pdf, content=3 bytes]");
		assertThat(new ActualisationAttachment(null, null, null, null, null, null, null))
			.hasToString("ActualisationAttachment[documentType=null, documentSenderType=null, title=null, senderName=null, fileName=null, mimeType=null, content=0 bytes]");
	}
}
