package se.sundsvall.caremanagement.rpa.service;

import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.rpa.api.model.RpaTaskRequest;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards that the {@code @OneOf} allow-list on {@link RpaTaskRequest#getAction()} stays in sync with
 * {@link RpaAction#ACTIONS} — so the API validation and the action catalogue can't drift.
 */
class RpaActionTest {

	@Test
	void oneOfMatchesActionCatalogue() throws Exception {
		final var field = RpaTaskRequest.class.getDeclaredField("action");
		final var oneOf = field.getAnnotation(OneOf.class);

		assertThat(oneOf).isNotNull();
		assertThat(Set.of(oneOf.value())).isEqualTo(RpaAction.ACTIONS);
	}

	@Test
	void actionsAreDistinctNonBlank() {
		assertThat(RpaAction.ACTIONS).allSatisfy(a -> assertThat(a).isNotBlank());
		assertThat(RpaAction.ACTIONS).contains(RpaAction.FETCH_SUPPLEMENTS, RpaAction.WRITE_NORMBERAKNING);
	}

	@Test
	void writeActionsAreInTheCatalogue() {
		// every WRITE_*/REGISTER_* constant declared on RpaAction is part of ACTIONS (catches a forgotten registration)
		final var declared = Arrays.stream(RpaAction.class.getDeclaredFields())
			.filter(f -> f.getType() == String.class)
			.map(f -> {
				try {
					return (String) f.get(null);
				} catch (final IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			})
			.toList();

		assertThat(RpaAction.ACTIONS).containsAll(declared);
	}
}
