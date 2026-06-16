package se.sundsvall.caremanagement.attachments.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfCombinerTest {

	private static final String PDF_MIME = "application/pdf";
	private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

	@Test
	void combinesPdfImageDocxAndUnknownIntoOnePdf() throws IOException {
		final var sources = List.of(
			new SourceFile("hyreskontrakt.pdf", PDF_MIME, pdf(2)),
			new SourceFile("hyresavi.png", "image/png", png()),
			new SourceFile("intyg.docx", DOCX_MIME, docx()),
			new SourceFile("anteckning.txt", "text/plain", "saknar pdf".getBytes()));

		final var combined = PdfCombiner.combine(sources);

		// 2 (pdf) + 1 (image) + >=1 (docx) + 1 (placeholder for the unknown text file).
		assertThat(pageCount(combined)).isGreaterThanOrEqualTo(5);
	}

	@Test
	void detectsTypesByExtensionWhenContentTypeMissing() throws IOException {
		final var sources = List.of(
			new SourceFile("scan.PDF", null, pdf(1)),  // PDF by (upper-case) extension
			new SourceFile("bild.JPG", null, png()),   // image by extension; bytes are sniffed, not the name
			new SourceFile(null, null, "x".getBytes())); // no name, no type -> placeholder named "bilaga"

		assertThat(pageCount(PdfCombiner.combine(sources))).isGreaterThanOrEqualTo(3);
	}

	@Test
	void corruptPdfDegradesToPlaceholderInsteadOfFailingTheBatch() throws IOException {
		final var sources = List.of(new SourceFile("broken.pdf", PDF_MIME, "this is not a pdf".getBytes()));

		assertThat(pageCount(PdfCombiner.combine(sources))).isEqualTo(1);
	}

	@Test
	void invalidImageDegradesToPlaceholder() throws IOException {
		final var sources = List.of(new SourceFile("photo.png", "image/png", new byte[] {
			1, 2, 3, 4
		}));

		assertThat(pageCount(PdfCombiner.combine(sources))).isEqualTo(1);
	}

	@Test
	void placeholderSanitisesNonLatinTextAndWrapsLongFilenames() throws IOException {
		final var oddName = "ärende_" + "x".repeat(120) + "_文書.bin"; // å-range kept, CJK replaced, length forces wrapping
		final var sources = List.of(new SourceFile(oddName, "application/octet-stream", new byte[] {
			0, 1, 2
		}));

		assertThat(pageCount(PdfCombiner.combine(sources))).isEqualTo(1);
	}

	private static int pageCount(final byte[] pdf) throws IOException {
		try (final var document = Loader.loadPDF(pdf)) {
			return document.getNumberOfPages();
		}
	}

	private static byte[] pdf(final int pages) throws IOException {
		try (final var document = new PDDocument();
			final var output = new ByteArrayOutputStream()) {
			for (var i = 0; i < pages; i++) {
				document.addPage(new PDPage(PDRectangle.A4));
			}
			document.save(output);
			return output.toByteArray();
		}
	}

	private static byte[] png() throws IOException {
		final var image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB);
		final var output = new ByteArrayOutputStream();
		ImageIO.write(image, "png", output);
		return output.toByteArray();
	}

	private static byte[] docx() throws IOException {
		try (final var document = new XWPFDocument();
			final var output = new ByteArrayOutputStream()) {
			document.createParagraph().createRun().setText("Underlag för hyra");
			document.write(output);
			return output.toByteArray();
		}
	}
}
