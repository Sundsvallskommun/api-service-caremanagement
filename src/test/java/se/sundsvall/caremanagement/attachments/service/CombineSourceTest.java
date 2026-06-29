package se.sundsvall.caremanagement.attachments.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CombineSourceTest {

	@Test
	void equalsCoversAllBranches() {
		final var base = new CombineSource("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});
		final var same = new CombineSource("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		assertThat(base.equals(base)).isTrue();
		assertThat(base.equals(same)).isTrue();
		assertThat(base.equals(new CombineSource("annan.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new CombineSource("intyg.pdf", "image/png", new byte[] {
			1, 2, 3
		}))).isFalse();
		assertThat(base.equals(new CombineSource("intyg.pdf", "application/pdf", new byte[] {
			9
		}))).isFalse();
		assertThat(base.equals(null)).isFalse();
		assertThat(base.equals("not a CombineSource")).isFalse();
	}

	@Test
	void hashCodeAndToString() {
		final var base = new CombineSource("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		});

		assertThat(base).hasSameHashCodeAs(new CombineSource("intyg.pdf", "application/pdf", new byte[] {
			1, 2, 3
		}));
		assertThat(base).hasToString("CombineSource[fileName=intyg.pdf, mimeType=application/pdf, content=3 bytes]");
		assertThat(new CombineSource(null, null, null)).hasToString("CombineSource[fileName=null, mimeType=null, content=0 bytes]");
	}
}
