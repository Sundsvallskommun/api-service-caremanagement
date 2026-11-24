package se.sundsvall.caremanagement.lifecare.professionalweb;

import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

class ProfessionalWebErrorsTest {

	@Test
	void isRefusal() {
		assertThat(ProfessionalWebErrors.isRefusal(Problem.valueOf(BAD_REQUEST, "x"))).isTrue();
		assertThat(ProfessionalWebErrors.isRefusal(Problem.valueOf(UNPROCESSABLE_CONTENT, "x"))).isTrue();
		assertThat(ProfessionalWebErrors.isRefusal(Problem.valueOf(BAD_GATEWAY, "x"))).isFalse();
	}
}
