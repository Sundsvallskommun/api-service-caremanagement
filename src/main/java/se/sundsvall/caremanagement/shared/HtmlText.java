package se.sundsvall.caremanagement.shared;

import java.util.Arrays;
import org.springframework.web.util.HtmlUtils;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

/**
 * Converts the HTML fragments Lifecare stores in free-text fields ({@code 
 * 
<p>
 * ...
 * 
</p>
 * } paragraphs with HTML entities
 * such as {@code f&ouml;r}) into the plain text the errand modules persist. Block-level breaks become newlines, all
 * other markup is dropped and entities are decoded — the Lifecare original remains the formatted source of record.
 */
public final class HtmlText {

	private HtmlText() {}

	/**
	 * The plain-text rendering of an HTML fragment: {@code null} stays {@code null}, blank input becomes the empty
	 * string, paragraph and line-break tags become newlines, remaining tags are stripped and entities are decoded.
	 *
	 * @param  html the HTML fragment, may be {@code null}
	 * @return      the plain text, or {@code null} when the input was {@code null}
	 */
	public static String toPlainText(final String html) {
		return ofNullable(html)
			.map(value -> value
				.replaceAll("(?i)<br\\s*/?>", "\n")
				.replaceAll("(?i)</p\\s*>", "\n")
				.replaceAll("(?i)</div\\s*>", "\n")
				.replaceAll("<[^>]*>", ""))
			.map(HtmlUtils::htmlUnescape)
			.map(value -> value.replace('\u00A0', ' '))
			.map(value -> withoutTrailingBlanks(value.strip()).replaceAll("\\n{3,}", "\n\n"))
			.orElse(null);
	}

	/**
	 * Each line without the spaces and tabs it ends with; done by hand, as a regex for it backtracks on long blank runs.
	 */
	private static String withoutTrailingBlanks(final String text) {
		return Arrays.stream(text.split("\n", -1))
			.map(HtmlText::stripTrailingBlanks)
			.collect(joining("\n"));
	}

	private static String stripTrailingBlanks(final String line) {
		var end = line.length();
		while (end > 0 && (line.charAt(end - 1) == ' ' || line.charAt(end - 1) == '\t')) {
			end--;
		}
		return line.substring(0, end);
	}
}
