package se.sundsvall.caremanagement.shared;

import org.springframework.web.util.HtmlUtils;

import static java.util.Optional.ofNullable;

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
			.map(value -> value.strip().replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n"))
			.orElse(null);
	}
}
