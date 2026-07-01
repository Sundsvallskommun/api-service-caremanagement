package se.sundsvall.caremanagement.types.financialassistance.archive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import se.sundsvall.caremanagement.conversation.spi.ConversationAttachmentView;
import se.sundsvall.caremanagement.conversation.spi.ConversationMessageView;
import se.sundsvall.caremanagement.types.financialassistance.archive.ThreadAttachments.NumberedAttachment;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Renders the text pages of the message history. {@link #renderMessages} produces the conversation section — a
 * title,
 * a short summary and every message oldest-first as a bold header ({@code timestamp · role · author}), the body and,
 * when present, a {@code Bilagor:} line listing the message's attachments by their global {@code Bilaga {n}} number.
 * {@link #renderSeparator} produces the one-page divider that precedes each appended attachment, naming it and its
 * origin so the reader can tell two identically-named files apart. The archiving job stitches these PDFs together with
 * the actual attachment files.
 *
 * <p>
 * Text is word-wrapped to the page width by measuring it in the Standard-14 Helvetica font and sanitised to the glyphs
 * that font can render (Swedish letters, en-dashes and smart quotes survive; a stray emoji becomes {@code ?}), so an
 * odd
 * character never breaks the render and content flows across as many A4 pages as it takes.
 * </p>
 */
final class MessageHistoryPdfRenderer {

	private static final float MARGIN = 50f;
	private static final float TITLE_SIZE = 18f;
	private static final float HEADING_SIZE = 11f;
	private static final float BODY_SIZE = 11f;
	private static final float LINE_GAP = 5f;
	private static final float PAGE_TOP = PDRectangle.A4.getHeight() - MARGIN;
	private static final float MAX_WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;

	private static final PDFont REGULAR = new PDType1Font(FontName.HELVETICA);
	private static final PDFont BOLD = new PDType1Font(FontName.HELVETICA_BOLD);

	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private MessageHistoryPdfRenderer() {}

	/** A single laid-out line and how to render it. */
	private record Line(String text, PDFont font, float size) {}

	/**
	 * Render the conversation (messages) section.
	 *
	 * @param  errandNumber the errand number, shown in the title block
	 * @param  thread       the conversation messages, oldest first
	 * @param  attachments  the thread's globally-numbered attachments (for the per-message {@code Bilagor:} listing)
	 * @return              the section as PDF bytes
	 */
	static byte[] renderMessages(final String errandNumber, final List<ConversationMessageView> thread, final List<NumberedAttachment> attachments) {
		final Map<Integer, List<NumberedAttachment>> byMessage = attachments.stream().collect(groupingBy(NumberedAttachment::messageIndex));

		final var lines = new ArrayList<Line>();
		add(lines, "Meddelandehistorik", BOLD, TITLE_SIZE);
		add(lines, "Ärende: " + errandNumber, REGULAR, BODY_SIZE);
		if (!thread.isEmpty()) {
			add(lines, "Period: %s – %s".formatted(date(thread.getFirst().created()), date(thread.getLast().created())), REGULAR, BODY_SIZE);
		}
		add(lines, "Antal meddelanden: " + thread.size(), REGULAR, BODY_SIZE);
		add(lines, "Antal bilagor: " + attachments.size(), REGULAR, BODY_SIZE);
		lines.add(blank());

		for (var messageIndex = 0; messageIndex < thread.size(); messageIndex++) {
			final var message = thread.get(messageIndex);
			add(lines, header(message), BOLD, HEADING_SIZE);
			add(lines, ofNullable(message.body()).orElse(""), REGULAR, BODY_SIZE);
			attachmentLine(message, byMessage.getOrDefault(messageIndex, List.of())).ifPresent(line -> add(lines, line, REGULAR, BODY_SIZE));
			lines.add(blank());
		}
		return renderLines(lines);
	}

	/**
	 * Render the one-page divider shown before an appended attachment.
	 *
	 * @param  attachment the attachment to introduce
	 * @return            the divider as PDF bytes
	 */
	static byte[] renderSeparator(final NumberedAttachment attachment) {
		final var date = ofNullable(attachment.created()).map(created -> created.atZoneSameInstant(ZoneId.systemDefault()).format(DATE)).orElse("");
		final var lines = new ArrayList<Line>();
		add(lines, "Bilaga " + attachment.number(), BOLD, TITLE_SIZE);
		add(lines, attachment.fileName(), REGULAR, BODY_SIZE);
		add(lines, "Bifogad av %s · %s".formatted(attachment.role(), date), REGULAR, BODY_SIZE);
		return renderLines(lines);
	}

	private static byte[] renderLines(final List<Line> lines) {
		try (final var document = new PDDocument(); final var output = new ByteArrayOutputStream()) {
			var index = 0;
			do {
				final var page = new PDPage(PDRectangle.A4);
				document.addPage(page);
				try (final var content = new PDPageContentStream(document, page)) {
					var y = PAGE_TOP;
					while (index < lines.size() && y > MARGIN) {
						final var line = lines.get(index);
						if (!line.text().isEmpty()) {
							content.beginText();
							content.setFont(line.font(), line.size());
							content.newLineAtOffset(MARGIN, y);
							content.showText(line.text());
							content.endText();
						}
						y -= line.size() + LINE_GAP;
						index++;
					}
				}
			} while (index < lines.size());

			document.save(output);
			return output.toByteArray();
		} catch (final IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Could not render message history PDF: %s".formatted(e.getMessage()));
		}
	}

	/**
	 * Sanitise the text, split it on its own line breaks, word-wrap each paragraph to the page width, and add the lines.
	 */
	private static void add(final List<Line> lines, final String text, final PDFont font, final float size) {
		for (final var paragraph : text.split("\r\n|\r|\n", -1)) {
			final var clean = sanitize(paragraph, font);
			if (clean.isEmpty()) {
				lines.add(blank());
				continue;
			}
			wordWrap(clean, font, size).forEach(line -> lines.add(new Line(line, font, size)));
		}
	}

	/**
	 * The {@code Bilagor:} line for a message, if any. Applicant attachments are listed by their {@code Bilaga {n}}
	 * number (they are appended); handläggare attachments are listed by file name and flagged as already in Lifecare (they
	 * are not appended).
	 */
	private static Optional<String> attachmentLine(final ConversationMessageView message, final List<NumberedAttachment> numbered) {
		if (ThreadAttachments.INBOUND.equals(message.direction())) {
			return numbered.isEmpty() ? Optional.empty()
				: Optional.of("Bilagor: " + numbered.stream().map(a -> "[%d] %s".formatted(a.number(), a.fileName())).collect(joining(", ")));
		}
		final var files = message.attachments();
		return files.isEmpty() ? Optional.empty()
			: Optional.of("Bilagor (finns i Lifecare): " + files.stream().map(ConversationAttachmentView::fileName).collect(joining(", ")));
	}

	private static String date(final OffsetDateTime timestamp) {
		return ofNullable(timestamp).map(value -> value.atZoneSameInstant(ZoneId.systemDefault()).format(DATE)).orElse("");
	}

	private static String header(final ConversationMessageView message) {
		final var timestamp = ofNullable(message.created())
			.map(created -> created.atZoneSameInstant(ZoneId.systemDefault()).format(TIMESTAMP))
			.orElse("");
		final var author = ofNullable(message.author()).filter(value -> !value.isBlank()).map(" · "::concat).orElse("");
		return "%s · %s%s".formatted(timestamp, ThreadAttachments.role(message.direction()), author);
	}

	private static Line blank() {
		return new Line("", REGULAR, BODY_SIZE);
	}

	/** Greedily pack words onto lines no wider than the page; a single word wider than the page is hard-broken. */
	private static List<String> wordWrap(final String line, final PDFont font, final float size) {
		final var out = new ArrayList<String>();
		final var current = new StringBuilder();
		for (final var word : line.split("\\s+")) {
			if (word.isEmpty()) {
				continue;
			}
			for (final var piece : fit(word, font, size)) {
				if (current.isEmpty()) {
					current.append(piece);
				} else if (width(current + " " + piece, font, size) <= MAX_WIDTH) {
					current.append(' ').append(piece);
				} else {
					out.add(current.toString());
					current.setLength(0);
					current.append(piece);
				}
			}
		}
		if (!current.isEmpty()) {
			out.add(current.toString());
		}
		if (out.isEmpty()) {
			return List.of("");
		}
		return out;
	}

	/** Break a single token so every piece fits the page width (only triggers for words longer than a full line). */
	private static List<String> fit(final String word, final PDFont font, final float size) {
		if (width(word, font, size) <= MAX_WIDTH) {
			return List.of(word);
		}
		final var pieces = new ArrayList<String>();
		final var piece = new StringBuilder();
		for (var i = 0; i < word.length(); i++) {
			piece.append(word.charAt(i));
			if (width(piece.toString(), font, size) > MAX_WIDTH && piece.length() > 1) {
				piece.deleteCharAt(piece.length() - 1);
				pieces.add(piece.toString());
				piece.setLength(0);
				piece.append(word.charAt(i));
			}
		}
		if (!piece.isEmpty()) {
			pieces.add(piece.toString());
		}
		return pieces;
	}

	/**
	 * Keep a string to the glyphs the given Standard-14 font can render: tabs become spaces, other control characters are
	 * dropped, and any character the font's WinAnsi encoding cannot represent (emoji, exotic punctuation) becomes
	 * {@code ?}. Swedish letters, en-dashes and smart quotes are all representable and survive untouched.
	 */
	private static String sanitize(final String text, final PDFont font) {
		final var builder = new StringBuilder(text.length());
		text.codePoints().forEach(codePoint -> {
			if (codePoint == '\t') {
				builder.append(' ');
			} else if (codePoint >= 0x20) {
				final var character = new String(Character.toChars(codePoint));
				final String appended;
				if (encodable(character, font)) {
					appended = character;
				} else {
					appended = "?";
				}
				builder.append(appended);
			}
		});
		return builder.toString().strip();
	}

	private static boolean encodable(final String character, final PDFont font) {
		try {
			font.getStringWidth(character);
			return true;
		} catch (final IllegalArgumentException | IOException e) {
			return false;
		}
	}

	private static float width(final String text, final PDFont font, final float size) {
		try {
			return font.getStringWidth(text) / 1000f * size;
		} catch (final IOException e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Could not measure text for the message history PDF: %s".formatted(e.getMessage()));
		}
	}
}
