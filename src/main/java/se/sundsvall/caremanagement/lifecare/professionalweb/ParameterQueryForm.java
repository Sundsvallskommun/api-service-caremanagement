package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;

/**
 * The parameter questions Lifecare's print asks before it renders some templates: the template "Beslut" asks "Visa
 * medsökande" for a beslut with a medsökande (capture 2026-09-30, architecture/lifecare-beslutsutskrift-parametrar.md).
 *
 * <p>
 * The page is an ordinary HTML form, {@code form#myForm}, that the browser posts back to the same address; Lifecare
 * then
 * answers with the PDF. Its field names differ per template (51_0_2_1, 18_0_2_1 and so on), so they are read off the
 * page every time. The fields are taken as a browser submits them, in document order: a checkbox only when checked,
 * beside the hidden false Lifecare gives it under the same name, and the session's X-LEGACY-TOKEN. careM answers with
 * Lifecare's own defaults; "Visa medsökande" is checked by default.
 * </p>
 */
final class ParameterQueryForm {

	static final String FORM_ID = "myForm";
	static final String TOKEN_FIELD = "X-LEGACY-TOKEN";

	private static final List<String> IGNORED_TYPES = List.of("submit", "button", "reset", "image", "file");
	private static final List<String> CHECKABLE_TYPES = List.of("checkbox", "radio");

	private ParameterQueryForm() {}

	/**
	 * Whether the answer is Lifecare's parameter questions rather than what was asked for.
	 *
	 * @param  response the answer
	 * @return          true for the parameter page
	 */
	static boolean isParameterQuery(final ProfessionalWebResponse response) {
		if (!response.contentType().contains("text/html")) {
			return false;
		}
		final var page = response.bodyAsString();
		return page.contains("id=\"" + FORM_ID + "\"") && page.contains("name=\"" + TOKEN_FIELD + "\"");
	}

	/**
	 * The fields to post back, as a browser would with every question left at Lifecare's default.
	 *
	 * @param  response the answer carrying the parameter page
	 * @return          the fields in document order, empty when the answer is not the parameter page
	 */
	static Optional<List<ProfessionalWebFormField>> fields(final ProfessionalWebResponse response) {
		if (!isParameterQuery(response)) {
			return Optional.empty();
		}
		return Optional.ofNullable(Jsoup.parse(new String(response.body(), StandardCharsets.UTF_8)).getElementById(FORM_ID))
			.map(ParameterQueryForm::read);
	}

	private static List<ProfessionalWebFormField> read(final Element form) {
		final var fields = new ArrayList<ProfessionalWebFormField>();
		for (final var element : form.select("input[name], select[name], textarea[name]")) {
			valueOf(element).ifPresent(value -> fields.add(new ProfessionalWebFormField(element.attr("name"), value)));
		}
		return fields;
	}

	/** What a browser submits for the element; empty for one it leaves out. */
	private static Optional<String> valueOf(final Element element) {
		if (!StringUtils.hasText(element.attr("name")) || element.hasAttr("disabled")) {
			return Optional.empty();
		}
		if ("select".equals(element.tagName())) {
			return Optional.ofNullable(element.selectFirst("option[selected]"))
				.or(() -> Optional.ofNullable(element.selectFirst("option")))
				.map(ParameterQueryForm::optionValue);
		}
		if ("textarea".equals(element.tagName())) {
			return Optional.of(element.wholeText());
		}
		final var type = element.attr("type").toLowerCase(Locale.ROOT);
		if (IGNORED_TYPES.contains(type)) {
			return Optional.empty();
		}
		if (CHECKABLE_TYPES.contains(type)) {
			return Optional.of(element).filter(checkable -> checkable.hasAttr("checked")).map(ParameterQueryForm::checkedValue);
		}
		return Optional.of(element.attr("value"));
	}

	/** An option without a value is submitted with its text. */
	private static String optionValue(final Element option) {
		if (option.hasAttr("value")) {
			return option.attr("value");
		}
		return option.text();
	}

	/** A checked box without a value is submitted as on. */
	private static String checkedValue(final Element checkable) {
		if (checkable.hasAttr("value")) {
			return checkable.attr("value");
		}
		return "on";
	}
}
