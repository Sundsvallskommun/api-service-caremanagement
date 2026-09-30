package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalWebHttpTest {

	@Test
	void encodeFormReturnsAnEmptyArrayRatherThanNullWhenThereAreNoFields() {
		assertThat(ProfessionalWebHttp.encodeForm(null)).isEmpty();
	}

	@Test
	void encodeFormEncodesEachFieldAsUrlEncodedPairs() {
		final var body = ProfessionalWebHttp.encodeForm(Map.of("a", "1"));

		assertThat(new String(body, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("a=1");
	}

	@Test
	void encodeFieldsKeepsTheOrderAndARepeatedName() {
		final var body = ProfessionalWebHttp.encodeFields(List.of(new ProfessionalWebFormField("51_0_2_1", "true"), new ProfessionalWebFormField("51_0_2_1", "false"),
			new ProfessionalWebFormField("X-LEGACY-TOKEN", "a b/c")));

		assertThat(new String(body, StandardCharsets.UTF_8)).isEqualTo("51_0_2_1=true&51_0_2_1=false&X-LEGACY-TOKEN=a+b%2Fc");
	}

	@Test
	void theParameterPageIsAnAnswerNotALostSession() {
		assertThat(ProfessionalWebHttp.needsSession(ParameterQueryFormTest.page("text/html; charset=utf-8", ParameterQueryFormTest.BESLUT_PAGE))).isFalse();
		assertThat(ProfessionalWebHttp.needsSession(ParameterQueryFormTest.page("text/html", "<html>Logga in</html>"))).isTrue();
	}
}
