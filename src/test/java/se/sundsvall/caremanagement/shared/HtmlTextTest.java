package se.sundsvall.caremanagement.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextTest {

	@Test
	void nullStaysNull() {
		assertThat(HtmlText.toPlainText(null)).isNull();
	}

	@Test
	void blankBecomesEmpty() {
		assertThat(HtmlText.toPlainText("   ")).isEmpty();
	}

	@Test
	void plainTextPassesThrough() {
		assertThat(HtmlText.toPlainText("Hej hopp")).isEqualTo("Hej hopp");
	}

	@Test
	void paragraphsBecomeNewlinesAndEntitiesAreDecoded() {
		assertThat(HtmlText.toPlainText("<p>Hej! Vill bara informera att jag f&aring;tt jobb p&aring; Mejeriet.</p><p>H&auml;lsningar</p>"))
			.isEqualTo("Hej! Vill bara informera att jag fått jobb på Mejeriet.\nHälsningar");
	}

	@Test
	void lineBreaksBecomeNewlines() {
		assertThat(HtmlText.toPlainText("rad ett<br>rad tv&aring;<br/>rad tre")).isEqualTo("rad ett\nrad två\nrad tre");
	}

	@Test
	void otherMarkupIsStripped() {
		assertThat(HtmlText.toPlainText("<div><b>fet</b> och <i>kursiv</i></div>")).isEqualTo("fet och kursiv");
	}

	@Test
	void nonBreakingSpacesBecomeRegularSpaces() {
		assertThat(HtmlText.toPlainText("a&nbsp;b")).isEqualTo("a b");
	}

	@Test
	void excessiveBlankLinesAreCollapsed() {
		assertThat(HtmlText.toPlainText("<p>a</p><p></p><p></p><p>b</p>")).isEqualTo("a\n\nb");
	}
}
