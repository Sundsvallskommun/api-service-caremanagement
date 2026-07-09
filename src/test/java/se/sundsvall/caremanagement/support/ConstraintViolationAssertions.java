package se.sundsvall.caremanagement.support;

import org.assertj.core.groups.Tuple;
import org.springframework.http.HttpStatus;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;

public final class ConstraintViolationAssertions {

	private ConstraintViolationAssertions() {}

	public static void assertConstraintViolation(final ConstraintViolationProblem response) {
		assertThat(response).isNotNull();
		assertThat(response.getTitle()).isEqualTo("Constraint Violation");
		assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getViolations())
			.isNotEmpty()
			.allSatisfy(violation -> assertThat(violation.field()).isNotBlank());
		assertThat(response.getViolations())
			.allSatisfy(violation -> assertThat(violation.message()).isNotBlank());
	}

	public static void assertConstraintViolation(final ConstraintViolationProblem response, final Tuple... violations) {
		assertConstraintViolation(response);
		assertThat(response.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(violations);
	}
}
