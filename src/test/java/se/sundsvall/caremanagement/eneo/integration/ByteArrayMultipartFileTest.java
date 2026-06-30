package se.sundsvall.caremanagement.eneo.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ByteArrayMultipartFileTest {

	@Test
	void exposesContentAndMetadata() throws IOException {
		final var content = "file content".getBytes();
		final var file = new ByteArrayMultipartFile("upload_file", "doc.pdf", "application/pdf", content);

		assertThat(file.getName()).isEqualTo("upload_file");
		assertThat(file.getOriginalFilename()).isEqualTo("doc.pdf");
		assertThat(file.getContentType()).isEqualTo("application/pdf");
		assertThat(file.isEmpty()).isFalse();
		assertThat(file.getSize()).isEqualTo(content.length);
		assertThat(file.getBytes()).isEqualTo(content);
		assertThat(file.getInputStream()).hasBinaryContent(content);
	}

	@Test
	void nullContentIsEmpty() {
		final var file = new ByteArrayMultipartFile("upload_file", "empty.pdf", "application/pdf", null);

		assertThat(file.isEmpty()).isTrue();
		assertThat(file.getSize()).isZero();
		assertThat(file.getBytes()).isEmpty();
	}

	@Test
	void transferToWritesContent(@TempDir final Path dir) throws IOException {
		final var content = "transferred".getBytes();
		final var file = new ByteArrayMultipartFile("upload_file", "doc.pdf", "application/pdf", content);
		final var target = dir.resolve("out.pdf");

		file.transferTo(target);

		assertThat(Files.readAllBytes(target)).isEqualTo(content);
	}
}
