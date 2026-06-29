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
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

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
	void rejectsInputAboveTheTotalSizeCap() {
		// One source just over the 50 MB total cap — combine must fail fast with 413 rather than risk OOM.
		final var oversized = new byte[(int) (50L * 1024 * 1024) + 1];
		assertThatThrownBy(() -> PdfCombiner.combine(List.of(new SourceFile("huge.bin", "application/octet-stream", oversized))))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", PAYLOAD_TOO_LARGE);
	}

	@Test
	void normalisesWideImageToA4LandscapePage() throws IOException {
		// An ultrawide image must not become a giant page sized to its pixels — it lands on a normal A4 landscape page.
		final var combined = PdfCombiner.combine(List.of(new SourceFile("wallpaper.png", "image/png", png(2400, 600))));

		try (final var document = Loader.loadPDF(combined)) {
			final var box = document.getPage(0).getMediaBox();
			assertThat(box.getWidth()).isCloseTo(PDRectangle.A4.getHeight(), within(1f));
			assertThat(box.getHeight()).isCloseTo(PDRectangle.A4.getWidth(), within(1f));
		}
	}

	@Test
	void normalisesTallImageToA4PortraitPage() throws IOException {
		final var combined = PdfCombiner.combine(List.of(new SourceFile("scan.png", "image/png", png(600, 2400))));

		try (final var document = Loader.loadPDF(combined)) {
			final var box = document.getPage(0).getMediaBox();
			assertThat(box.getWidth()).isCloseTo(PDRectangle.A4.getWidth(), within(1f));
			assertThat(box.getHeight()).isCloseTo(PDRectangle.A4.getHeight(), within(1f));
		}
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
	void rendersLegacyDocByExtractingItsText() throws IOException {
		final var sources = List.of(new SourceFile("kontrakt.doc", "application/msword", fixture("/pdfcombiner/sample.doc")));

		final var combined = PdfCombiner.combine(sources);

		assertThat(pageCount(combined)).isGreaterThanOrEqualTo(1);
		// Proves the HWPF render path ran rather than falling back to a "kunde inte" placeholder.
		assertThat(text(combined)).doesNotContain("kunde inte");
	}

	@Test
	void corruptDocDegradesToPlaceholder() throws IOException {
		final var sources = List.of(new SourceFile("kontrakt.doc", "application/msword", "not a real doc".getBytes()));

		final var combined = PdfCombiner.combine(sources);

		assertThat(pageCount(combined)).isEqualTo(1);
		assertThat(text(combined)).contains("kunde inte");
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
	void textDocumentPaginatesLongTextAndSanitisesControlAndNonLatinChars() throws IOException {
		// 80 lines forces a second page; each line exercises tab, a non-tab control char, Latin-1 and a >255 (CJK) glyph.
		final var text = "Kolumn1\tKolumn2 åäö  文書\n".repeat(80);

		try (final var document = PdfCombiner.textDocument(text)) {
			assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(2);
		}
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

	private static String text(final byte[] pdf) throws IOException {
		try (final var document = Loader.loadPDF(pdf)) {
			return new PDFTextStripper().getText(document);
		}
	}

	private static byte[] fixture(final String path) throws IOException {
		try (final var in = PdfCombinerTest.class.getResourceAsStream(path)) {
			return in.readAllBytes();
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
		return png(12, 8);
	}

	private static byte[] png(final int width, final int height) throws IOException {
		final var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
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
