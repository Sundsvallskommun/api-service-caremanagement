package se.sundsvall.caremanagement.conversation.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationAttachmentViewTest {

	@Test
	void equalsCoversAllBranches() {
		final var base = new ConversationAttachmentView("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});
		final var same = new ConversationAttachmentView("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		assertThat(base.equals(base)).isTrue();
		assertThat(base.equals(same)).isTrue();
		assertThat(base.equals(new ConversationAttachmentView("annan.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new ConversationAttachmentView("intyg.pdf", "image/png", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new ConversationAttachmentView("intyg.pdf", "application/pdf", new byte[] {
			9
		}))).isFalse();
		assertThat(base.equals(null)).isFalse();
		assertThat(base.equals("not a ConversationAttachmentView")).isFalse();
	}

	@Test
	void hashCodeAndToString() {
		final var base = new ConversationAttachmentView("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		assertThat(base).hasSameHashCodeAs(new ConversationAttachmentView("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		})).hasToString("ConversationAttachmentView[fileName=intyg.pdf, mimeType=application/pdf, content=3 bytes]");
		assertThat(new ConversationAttachmentView(null, null, null)).hasToString("ConversationAttachmentView[fileName=null, mimeType=null, content=0 bytes]");
	}
}
