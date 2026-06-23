package se.sundsvall.caremanagement.types.financialassistance.archive;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.conversation.spi.ConversationAttachmentView;
import se.sundsvall.caremanagement.conversation.spi.ConversationMessageView;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class MeddelandehistorikPdfRendererTest {

	private static final String ERRAND_NUMBER = "EB-2026-000123";

	private static ConversationAttachmentView attachment(final String name) {
		return new ConversationAttachmentView(name, "application/pdf", "x".getBytes());
	}

	private static String textOf(final byte[] pdf) throws IOException {
		try (final var document = Loader.loadPDF(pdf)) {
			return new PDFTextStripper().getText(document);
		}
	}

	@Test
	void rendersTitleSummaryRolesAndNumberedAttachments() throws IOException {
		final var thread = List.of(
			new ConversationMessageView("INBOUND", "Hej, jag bifogar mitt intyg.", "joe01doe", OffsetDateTime.parse("2026-06-01T09:15:00+02:00"),
				List.of(attachment("intyg.pdf"), attachment("kvitto.pdf"))),
			new ConversationMessageView("OUTBOUND", "Tack, vi har tagit emot ditt intyg.", null, OffsetDateTime.parse("2026-06-02T13:00:00+02:00"), emptyList()));

		final var pdf = MeddelandehistorikPdfRenderer.renderMessages(ERRAND_NUMBER, thread, ThreadAttachments.flatten(thread));

		assertThat(pdf).isNotEmpty();
		assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

		final var text = textOf(pdf);
		assertThat(text).contains("Meddelandehistorik");
		assertThat(text).contains(ERRAND_NUMBER);
		assertThat(text).contains("Antal meddelanden: 2");
		assertThat(text).contains("Antal bilagor: 2");
		assertThat(text).contains("Sökande");
		assertThat(text).contains("Handläggare");
		assertThat(text).contains("Bilagor: [1] intyg.pdf, [2] kvitto.pdf");
	}

	@Test
	void rendersSeparatorPageForAnAttachment() throws IOException {
		final var thread = List.of(
			new ConversationMessageView("INBOUND", "Här kommer underlaget.", "k.andersson", OffsetDateTime.parse("2026-06-01T09:15:00+02:00"),
				List.of(attachment("kontoutdrag.pdf"))));
		final var attachment = ThreadAttachments.flatten(thread).getFirst();

		final var text = textOf(MeddelandehistorikPdfRenderer.renderSeparator(attachment));

		assertThat(text).contains("Bilaga 1");
		assertThat(text).contains("kontoutdrag.pdf");
		assertThat(text).contains("Bifogad av Sökande");
	}

	@Test
	void sanitisesUnencodableCharactersAndWrapsLongLines() throws IOException {
		final var longLine = "x".repeat(400);
		final var thread = List.of(
			new ConversationMessageView("OUTBOUND", "Emoji 😀 och tab\tslut.\n" + longLine, "agent", OffsetDateTime.now(), emptyList()),
			new ConversationMessageView("INBOUND", "", "", null, emptyList()));

		final var text = textOf(MeddelandehistorikPdfRenderer.renderMessages(ERRAND_NUMBER, thread, ThreadAttachments.flatten(thread)));

		assertThat(text).contains("Meddelandehistorik");
		assertThat(text.replace("\n", "").replace("\r", "")).contains("x".repeat(200));
	}

	@Test
	void flowsAcrossMultiplePagesForLongThreads() throws IOException {
		final var thread = new ArrayList<ConversationMessageView>();
		for (var i = 0; i < 120; i++) {
			thread.add(new ConversationMessageView(i % 2 == 0 ? "INBOUND" : "OUTBOUND", "Meddelande nummer " + i, "user" + i, OffsetDateTime.now(), emptyList()));
		}

		final var pdf = MeddelandehistorikPdfRenderer.renderMessages(ERRAND_NUMBER, thread, ThreadAttachments.flatten(thread));

		try (final var document = Loader.loadPDF(pdf)) {
			assertThat(document.getNumberOfPages()).isGreaterThan(1);
		}
	}

	@Test
	void rendersEmptyThread() throws IOException {
		final var pdf = MeddelandehistorikPdfRenderer.renderMessages(ERRAND_NUMBER, emptyList(), emptyList());

		assertThat(pdf).isNotEmpty();
		assertThat(textOf(pdf)).contains("Antal meddelanden: 0");
	}
}
