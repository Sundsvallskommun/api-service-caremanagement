package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterQueryFormTest {

	/** RenderPdf/PrintDecision, template Beslut, beslut 134 with a medsökande (capture 2026-09-30), token masked. */
	static final String BESLUT_PAGE = """
		<!DOCTYPE html><html><head><title>Parameterfrågor</title></head><body>
		<div id="app-config"></div>
		<form id="myForm" method="post" novalidate autocomplete="off">
		  <input type="checkbox" name="51_0_2_1" id="51_0_2_1" value="true" checked><label for="51_0_2_1">Visa medsökande</label>
		  <input type="hidden" name="51_0_2_1" value="false">
		  <input type="hidden" name="X-LEGACY-TOKEN" value="tok">
		  <button type="submit" id="submitButton">Klar</button>
		</form>
		<script src="../dist2/parameter_query_main.js"></script>
		</body></html>""";

	static ProfessionalWebResponse page(final String contentType, final String body) {
		return new ProfessionalWebResponse(200, HttpHeaders.of(Map.of("Content-Type", List.of(contentType)), (name, value) -> true), body.getBytes(StandardCharsets.UTF_8),
			URI.create("https://lifecare.test/WESE.FC.ProfessionalWeb/RenderPdf/PrintDecision"));
	}

	@Test
	void postsTheBeslutQuestionsBackAsTheBrowserDoes() {
		assertThat(ParameterQueryForm.fields(page("text/html; charset=utf-8", BESLUT_PAGE))).contains(List.of(
			new ProfessionalWebFormField("51_0_2_1", "true"),
			new ProfessionalWebFormField("51_0_2_1", "false"),
			new ProfessionalWebFormField("X-LEGACY-TOKEN", "tok")));
	}

	@Test
	void takesEachKindOfFieldAsABrowserSubmitsIt() {
		// The address template asks five prefilled text questions; an unchecked box, a disabled field and a button are left
		// out.
		final var form = """
			<form id="myForm" method="post">
			  <input type="text" name="16_0_2_1" value="Testsson, Test">
			  <input type="text" name="16_0_2_2">
			  <input type="checkbox" name="unchecked" value="true">
			  <input type="checkbox" name="noValue" checked>
			  <input type="radio" name="choice" value="a"><input type="radio" name="choice" value="b" checked>
			  <input type="text" name="off" value="x" disabled>
			  <input type="text" value="nameless">
			  <select name="list"><option value="1">Ett</option><option value="2" selected>Två</option></select>
			  <select name="bare"><option>Utan värde</option></select>
			  <textarea name="note">rad</textarea>
			  <input type="submit" name="go" value="Klar">
			  <input type="hidden" name="X-LEGACY-TOKEN" value="tok">
			</form>""";

		assertThat(ParameterQueryForm.fields(page("text/html", form))).contains(List.of(
			new ProfessionalWebFormField("16_0_2_1", "Testsson, Test"),
			new ProfessionalWebFormField("16_0_2_2", ""),
			new ProfessionalWebFormField("noValue", "on"),
			new ProfessionalWebFormField("choice", "b"),
			new ProfessionalWebFormField("list", "2"),
			new ProfessionalWebFormField("bare", "Utan värde"),
			new ProfessionalWebFormField("note", "rad"),
			new ProfessionalWebFormField("X-LEGACY-TOKEN", "tok")));
	}

	@Test
	void knowsTheParameterPageFromOtherAnswers() {
		assertThat(ParameterQueryForm.isParameterQuery(page("text/html; charset=utf-8", BESLUT_PAGE))).isTrue();
		assertThat(ParameterQueryForm.isParameterQuery(page("application/pdf", BESLUT_PAGE))).isFalse();
		// The sign-in page is a form too, but carries no LEGACY-TOKEN.
		assertThat(ParameterQueryForm.isParameterQuery(page("text/html", "<form id=\"myForm\" action=\"/idp/login\"><input name=\"uid\"></form>"))).isFalse();
		assertThat(ParameterQueryForm.isParameterQuery(page("text/html", "<form><input type=\"hidden\" name=\"X-LEGACY-TOKEN\" value=\"t\"></form>"))).isFalse();
		assertThat(ParameterQueryForm.fields(page("text/html", "<html>Lifecare</html>"))).isEmpty();
	}
}
