package se.sundsvall.caremanagement.conversation.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationAttachmentContentTest {

	@Test
	void equalsCoversAllBranches() {
		final var base = new ConversationAttachmentContent("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});
		final var same = new ConversationAttachmentContent("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		assertThat(base.equals(base)).isTrue();
		assertThat(base.equals(same)).isTrue();
		assertThat(base.equals(new ConversationAttachmentContent("annan.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new ConversationAttachmentContent("intyg.pdf", "image/png", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new ConversationAttachmentContent("intyg.pdf", "application/pdf", new byte[] {
			9
		}))).isFalse();
		assertThat(base.equals(null)).isFalse();
		assertThat(base.equals("not a ConversationAttachmentContent")).isFalse();
	}

	@Test
	void hashCodeAndToString() {
		final var base = new ConversationAttachmentContent("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		assertThat(base).hasSameHashCodeAs(new ConversationAttachmentContent("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}));
		assertThat(base).hasToString("ConversationAttachmentContent[fileName=intyg.pdf, mimeType=application/pdf, content=3 bytes]");
		assertThat(new ConversationAttachmentContent(null, null, null)).hasToString("ConversationAttachmentContent[fileName=null, mimeType=null, content=0 bytes]");
	}
}
