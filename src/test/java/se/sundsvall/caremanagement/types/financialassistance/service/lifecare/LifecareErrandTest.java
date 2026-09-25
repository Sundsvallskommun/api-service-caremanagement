package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LifecareErrandTest {

	@Test
	void accessors() {
		final var ids = new ArrayList<>(List.of("p1"));
		final var errand = new LifecareErrand("2281", "NS", "e", 24, 3, 4, ids, 2026, 9);
		ids.add("p2");

		assertThat(errand.requireServiceId()).isEqualTo(24);
		assertThat(errand.calculation()).contains(3);
		assertThat(errand.decision()).contains(4);
		assertThat(errand.paymentIds()).containsExactly("p1");
	}

	@Test
	void noServiceId() {
		final var errand = new LifecareErrand("2281", "NS", "e", null, null, null, null, null, null);

		assertThat(errand.paymentIds()).isEmpty();
		assertThat(errand.calculation()).isEmpty();
		assertThatThrownBy(errand::requireServiceId).isInstanceOfSatisfying(ThrowableProblem.class,
			problem -> assertThat(problem.getStatus().value()).isEqualTo(409));
	}
}
