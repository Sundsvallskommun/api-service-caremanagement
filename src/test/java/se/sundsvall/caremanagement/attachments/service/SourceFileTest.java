package se.sundsvall.caremanagement.attachments.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceFileTest {

	@Test
	void equalsCoversAllBranches() {
		final var base = new SourceFile("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});
		final var same = new SourceFile("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		assertThat(base.equals(base)).isTrue();
		assertThat(base.equals(same)).isTrue();
		assertThat(base.equals(new SourceFile("annan.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new SourceFile("intyg.pdf", "image/png", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new SourceFile("intyg.pdf", "application/pdf", new byte[] {
			9
		}))).isFalse();
		assertThat(base.equals(null)).isFalse();
		assertThat(base.equals("not a SourceFile")).isFalse();
	}

	@Test
	void hashCodeAndToString() {
		final var base = new SourceFile("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		assertThat(base).hasSameHashCodeAs(new SourceFile("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		})).hasToString("SourceFile[fileName=intyg.pdf, contentType=application/pdf, content=3 bytes]");
		assertThat(new SourceFile(null, null, null)).hasToString("SourceFile[fileName=null, contentType=null, content=0 bytes]");
	}
}
