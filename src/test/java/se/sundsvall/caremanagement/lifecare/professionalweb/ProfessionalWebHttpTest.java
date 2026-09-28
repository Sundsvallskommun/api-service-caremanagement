package se.sundsvall.caremanagement.lifecare.professionalweb;

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
}
