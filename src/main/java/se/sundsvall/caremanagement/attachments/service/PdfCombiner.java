package se.sundsvall.caremanagement.attachments.service;

import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Merges a heterogeneous list of uploaded files into a single PDF.
 *
 * <p>
 * Each source is parsed into a {@link PDDocument} first — existing PDFs are loaded, images are rasterised onto a page
 * sized to the image, and {@code .docx} documents are rendered via POI/XDocReport. Anything we cannot parse (an unknown
 * or corrupt file) becomes a one-page placeholder naming the file, so combining a list <em>never</em> fails because of
 * a
 * single odd attachment. The per-source documents are then concatenated with PDFBox.
 * </p>
 */
final class PdfCombiner {

	private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private static final byte[] PDF_MAGIC = {
		'%', 'P', 'D', 'F'
	};
	private static final float MARGIN = 50f;
	private static final float FONT_SIZE = 12f;
	private static final float LEADING = 16f;
	private static final int WRAP = 90;

	private PdfCombiner() {}

	/**
	 * Concatenate the given sources into one PDF.
	 *
	 * @param  sources the files to combine, in order (must contain at least one element)
	 * @return         the combined PDF as bytes
	 */
	static byte[] combine(final List<SourceFile> sources) {
		// The source documents must stay open until the merged result is saved — PDFBox's appendDocument keeps lazy
		// references into them — so they are closed only afterwards, in the finally block.
		final var documents = new ArrayList<PDDocument>();
		try (final var result = new PDDocument()) {
			final var merger = new PDFMergerUtility();
			for (final var source : sources) {
				final var document = toDocument(source);
				documents.add(document);
				merger.appendDocument(result, document);
			}

			final var output = new ByteArrayOutputStream();
			result.save(output);
			return output.toByteArray();
		} catch (final IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Could not combine attachments into a PDF: %s".formatted(e.getMessage()));
		} finally {
			documents.forEach(IOUtils::closeQuietly);
		}
	}

	/**
	 * Parse a single source into a PDF document. Falls back to a placeholder page rather than ever propagating a failure.
	 */
	private static PDDocument toDocument(final SourceFile source) {
		final var name = ofNullable(source.fileName()).orElse("bilaga");
		try {
			if (isPdf(source)) {
				return Loader.loadPDF(source.content());
			}
			if (isImage(source)) {
				return imageDocument(source.content(), name);
			}
			if (isDocx(source)) {
				return Loader.loadPDF(docxToPdf(source.content()));
			}
			return placeholderDocument("Bilaga: %s (filtypen kunde inte infogas i sammanställningen)".formatted(name));
		} catch (final Exception e) {
			return placeholderDocument("Bilaga: %s (kunde inte läsas: %s)".formatted(name, e.getMessage()));
		}
	}

	private static boolean isPdf(final SourceFile source) {
		return startsWith(source.content(), PDF_MAGIC)
			|| "application/pdf".equalsIgnoreCase(source.contentType())
			|| hasExtension(source.fileName(), ".pdf");
	}

	private static boolean isImage(final SourceFile source) {
		return ofNullable(source.contentType()).filter(type -> type.toLowerCase().startsWith("image/")).isPresent()
			|| hasExtension(source.fileName(), ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".tif", ".tiff");
	}

	private static boolean isDocx(final SourceFile source) {
		return DOCX_MIME.equalsIgnoreCase(source.contentType())
			|| hasExtension(source.fileName(), ".docx");
	}

	private static PDDocument imageDocument(final byte[] content, final String name) throws IOException {
		final var document = new PDDocument();
		final var image = PDImageXObject.createFromByteArray(document, content, name);
		final var page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
		document.addPage(page);
		try (final var contentStream = new PDPageContentStream(document, page)) {
			contentStream.drawImage(image, 0, 0, image.getWidth(), image.getHeight());
		}
		return document;
	}

	private static byte[] docxToPdf(final byte[] content) throws IOException {
		try (final var input = new ByteArrayInputStream(content);
			final var document = new XWPFDocument(input);
			final var output = new ByteArrayOutputStream()) {
			PdfConverter.getInstance().convert(document, output, PdfOptions.getDefault());
			return output.toByteArray();
		}
	}

	private static PDDocument placeholderDocument(final String text) {
		try {
			final var document = new PDDocument();
			final var page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			try (final var contentStream = new PDPageContentStream(document, page)) {
				contentStream.beginText();
				contentStream.setFont(new PDType1Font(FontName.HELVETICA), FONT_SIZE);
				contentStream.setLeading(LEADING);
				contentStream.newLineAtOffset(MARGIN, PDRectangle.A4.getHeight() - MARGIN);
				for (final var line : wrap(sanitize(text))) {
					contentStream.showText(line);
					contentStream.newLine();
				}
				contentStream.endText();
			}
			return document;
		} catch (final IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Could not render placeholder page: %s".formatted(e.getMessage()));
		}
	}

	/** Keep the placeholder text to glyphs the standard Helvetica font can render. */
	private static String sanitize(final String text) {
		final var builder = new StringBuilder(text.length());
		for (final var character : text.toCharArray()) {
			builder.append((character >= 32 && character <= 255) ? character : '?');
		}
		return builder.toString();
	}

	private static List<String> wrap(final String text) {
		final var lines = new ArrayList<String>();
		for (var start = 0; start < text.length(); start += WRAP) {
			lines.add(text.substring(start, Math.min(start + WRAP, text.length())));
		}
		return lines.isEmpty() ? List.of("") : lines;
	}

	private static boolean startsWith(final byte[] content, final byte[] prefix) {
		if (content == null || content.length < prefix.length) {
			return false;
		}
		for (var i = 0; i < prefix.length; i++) {
			if (content[i] != prefix[i]) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasExtension(final String fileName, final String... extensions) {
		final var lower = ofNullable(fileName).map(String::toLowerCase).orElse("");
		return Arrays.stream(extensions).anyMatch(lower::endsWith);
	}
}
