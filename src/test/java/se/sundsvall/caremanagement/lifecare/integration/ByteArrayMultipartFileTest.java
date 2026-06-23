package se.sundsvall.caremanagement.lifecare.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ByteArrayMultipartFileTest {

	private static final byte[] CONTENT = "hello".getBytes(StandardCharsets.UTF_8);

	@Test
	void exposesMetadataAndContent() throws IOException {
		final var file = new ByteArrayMultipartFile("Content", "report.pdf", "application/pdf", CONTENT);

		assertThat(file.getName()).isEqualTo("Content");
		assertThat(file.getOriginalFilename()).isEqualTo("report.pdf");
		assertThat(file.getContentType()).isEqualTo("application/pdf");
		assertThat(file.isEmpty()).isFalse();
		assertThat(file.getSize()).isEqualTo(CONTENT.length);
		assertThat(file.getBytes()).isEqualTo(CONTENT);
		assertThat(file.getInputStream().readAllBytes()).isEqualTo(CONTENT);
	}

	@Test
	void getBytesReturnsACopy() {
		final var file = new ByteArrayMultipartFile("Content", "report.pdf", "application/pdf", CONTENT);

		file.getBytes()[0] = 0;

		assertThat(file.getBytes()).isEqualTo(CONTENT);
	}

	@Test
	void nullContentIsEmpty() {
		final var file = new ByteArrayMultipartFile("Content", "empty.pdf", "application/pdf", null);

		assertThat(file.isEmpty()).isTrue();
		assertThat(file.getSize()).isZero();
		assertThat(file.getBytes()).isEmpty();
	}

	@Test
	void transfersToPath(@TempDir final Path dir) throws IOException {
		final var file = new ByteArrayMultipartFile("Content", "report.pdf", "application/pdf", CONTENT);
		final var target = dir.resolve("path-out.pdf");

		file.transferTo(target);

		assertThat(Files.readAllBytes(target)).isEqualTo(CONTENT);
	}

	@Test
	void transfersToFile(@TempDir final Path dir) throws IOException {
		final var file = new ByteArrayMultipartFile("Content", "report.pdf", "application/pdf", CONTENT);
		final var target = dir.resolve("file-out.pdf").toFile();

		file.transferTo(target);

		assertThat(Files.readAllBytes(target.toPath())).isEqualTo(CONTENT);
	}
}
