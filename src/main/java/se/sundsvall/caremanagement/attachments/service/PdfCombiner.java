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
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

/**
 * Merges a heterogeneous list of uploaded files into a single PDF.
 *
 * <p>
 * Each source is parsed into a {@link PDDocument} first — existing PDFs are loaded, images are rasterised onto a page
 * sized to the image, {@code .docx} is rendered via POI/XDocReport, and legacy {@code .doc} (HWPF) is laid out as its
 * extracted text. Anything we cannot parse (an unknown or corrupt file) becomes a one-page placeholder naming the file,
 * so combining a list <em>never</em> fails because of a single odd attachment. The per-source documents are then
 * concatenated with PDFBox.
 * </p>
 */
final class PdfCombiner {

	private static final Logger LOG = LoggerFactory.getLogger(PdfCombiner.class);

	private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private static final String DOC_MIME = "application/msword";
	private static final byte[] PDF_MAGIC = {
		'%', 'P', 'D', 'F'
	};
	private static final float MARGIN = 50f;
	private static final float FONT_SIZE = 12f;
	private static final float LEADING = 16f;
	private static final int WRAP = 90;

	/**
	 * Upper bound on the combined input size. Every source is held in memory as bytes and again as a live
	 * {@link PDDocument} during the merge, so the peak heap is a multiple of this; cap the total to fail fast with a 413
	 * rather than OOM the request/listener thread on a pathological attachment set.
	 */
	private static final long MAX_TOTAL_INPUT_BYTES = 50L * 1024 * 1024;

	private PdfCombiner() {}

	/**
	 * Concatenate the given sources into one PDF.
	 *
	 * @param  sources the files to combine, in order (must contain at least one element)
	 * @return         the combined PDF as bytes
	 */
	static byte[] combine(final List<SourceFile> sources) {
		final var totalBytes = sources.stream()
			.map(SourceFile::content)
			.filter(content -> content != null)
			.mapToLong(content -> content.length)
			.sum();
		if (totalBytes > MAX_TOTAL_INPUT_BYTES) {
			throw Problem.valueOf(PAYLOAD_TOO_LARGE, "Attachments are too large to combine into a single PDF (%d bytes, max %d).".formatted(totalBytes, MAX_TOTAL_INPUT_BYTES));
		}

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
			if (isDoc(source)) {
				return docToDocument(source.content());
			}
			LOG.info("Attachment '{}' (type '{}') has no inline renderer — using a placeholder page in the combined PDF", forLog(name), forLog(source.contentType()));
			return textDocument("Bilaga: %s (filtypen kunde inte infogas i sammanställningen)".formatted(name));
		} catch (final Exception e) {
			// Log without content/PII so a silently-dropped source is auditable; the content itself is never logged.
			LOG.warn("Attachment '{}' could not be rendered into the combined PDF ({}) — using a placeholder page", forLog(name), e.getClass().getSimpleName());
			return textDocument("Bilaga: %s (kunde inte läsas: %s)".formatted(name, e.getMessage()));
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

	private static boolean isDoc(final SourceFile source) {
		return DOC_MIME.equalsIgnoreCase(source.contentType())
			|| hasExtension(source.fileName(), ".doc");
	}

	private static PDDocument imageDocument(final byte[] content, final String name) throws IOException {
		final var document = new PDDocument();
		final var image = PDImageXObject.createFromByteArray(document, content, name);

		// Normalise every image onto a standard A4 page — landscape for images wider than tall, portrait otherwise — so a
		// wide wallpaper and a portrait scan end up the same page size in the combined PDF rather than each keeping its own
		// pixel dimensions. The image is scaled to fit within the page margins, preserving aspect ratio, and centred;
		// images already smaller than that are left at their natural size (never upscaled).
		final var pageSize = image.getWidth() > image.getHeight()
			? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
			: PDRectangle.A4;
		final var page = new PDPage(pageSize);
		document.addPage(page);

		final var scale = Math.min(Math.min((pageSize.getWidth() - 2 * MARGIN) / image.getWidth(), (pageSize.getHeight() - 2 * MARGIN) / image.getHeight()), 1f);
		final var width = image.getWidth() * scale;
		final var height = image.getHeight() * scale;
		final var x = (pageSize.getWidth() - width) / 2;
		final var y = (pageSize.getHeight() - height) / 2;

		try (final var contentStream = new PDPageContentStream(document, page)) {
			contentStream.drawImage(image, x, y, width, height);
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

	/** Legacy binary {@code .doc}: there is no in-process renderer, so lay out the document's extracted text. */
	private static PDDocument docToDocument(final byte[] content) throws IOException {
		try (final var input = new ByteArrayInputStream(content);
			final var document = new HWPFDocument(input);
			final var extractor = new WordExtractor(document)) {
			return textDocument(extractor.getText());
		}
	}

	/** Render the given text across as many A4 pages as it takes; also serves as the placeholder page renderer. */
	static PDDocument textDocument(final String text) {
		try {
			final var lines = layout(text);
			final var document = new PDDocument();
			var index = 0;
			do {
				final var page = new PDPage(PDRectangle.A4);
				document.addPage(page);
				try (final var contentStream = new PDPageContentStream(document, page)) {
					contentStream.beginText();
					contentStream.setFont(new PDType1Font(FontName.HELVETICA), FONT_SIZE);
					contentStream.setLeading(LEADING);
					contentStream.newLineAtOffset(MARGIN, PDRectangle.A4.getHeight() - MARGIN);
					var y = PDRectangle.A4.getHeight() - MARGIN;
					while (index < lines.size() && y > MARGIN) {
						contentStream.showText(lines.get(index));
						contentStream.newLine();
						y -= LEADING;
						index++;
					}
					contentStream.endText();
				}
			} while (index < lines.size());
			return document;
		} catch (final IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Could not render text page: %s".formatted(e.getMessage()));
		}
	}

	/**
	 * Neutralise control characters (notably CR/LF) in an untrusted value — the filename and content-type come from the
	 * uploaded file — before it goes into a log line, so a crafted name can't forge log entries (CWE-117).
	 */
	private static String forLog(final String value) {
		return (value == null) ? null : value.replace("\n", "_").replace("\r", "_").replace("\t", "_");
	}

	/** Split on line breaks, sanitise each line, and wrap it to the page width. */
	private static List<String> layout(final String text) {
		final var lines = new ArrayList<String>();
		for (final var raw : text.split("\r\n|\r|\n", -1)) {
			final var clean = sanitize(raw);
			if (clean.isEmpty()) {
				lines.add("");
				continue;
			}
			for (var start = 0; start < clean.length(); start += WRAP) {
				lines.add(clean.substring(start, Math.min(start + WRAP, clean.length())));
			}
		}
		return lines.isEmpty() ? List.of("") : lines;
	}

	/** Keep a single line to glyphs the standard Helvetica font can render (tabs and control chars become spaces). */
	private static String sanitize(final String text) {
		final var builder = new StringBuilder(text.length());
		for (final var character : text.toCharArray()) {
			if (character == '\t') {
				builder.append("    ");
			} else if (character < 32) {
				builder.append(' ');
			} else if (character <= 255) {
				builder.append(character);
			} else {
				builder.append('?');
			}
		}
		return builder.toString();
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
