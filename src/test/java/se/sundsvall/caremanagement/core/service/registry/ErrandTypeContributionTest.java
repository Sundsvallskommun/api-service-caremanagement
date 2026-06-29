package se.sundsvall.caremanagement.core.service.registry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class ErrandTypeContributionTest {

	@Test
	void statusKeepsDisplayNameAndDeclarationOrder() {
		final var contribution = ErrandTypeContribution.builder("eb")
			.status("RECEIVED", "Inkommen")
			.status("UNDER_REVIEW", "Under utredning")
			.allowedStatuses("WITHDRAWN")
			.allowedTransition("UNDER_REVIEW", "GRANTED")
			.build();

		assertThat(contribution.statusDisplayNames())
			.containsExactly(
				entry("RECEIVED", "Inkommen"),
				entry("UNDER_REVIEW", "Under utredning"),
				entry("WITHDRAWN", null),
				entry("GRANTED", null));
		assertThat(contribution.allowedStatuses())
			.containsExactlyInAnyOrder("RECEIVED", "UNDER_REVIEW", "WITHDRAWN", "GRANTED");
	}

	@Test
	void buildsWithAllowedStatusesAndTransitions() {
		final var contribution = ErrandTypeContribution.builder("fostercare")
			.displayName("Foster Care")
			.allowedStatuses("DRAFT", "OPEN")
			.allowedTransition("OPEN", "CLOSED", "ARCHIVED")
			.build();

		assertThat(contribution.typeSlug()).isEqualTo("fostercare");
		assertThat(contribution.displayName()).isEqualTo("Foster Care");
		assertThat(contribution.allowedStatuses()).containsExactlyInAnyOrder("DRAFT", "OPEN", "CLOSED", "ARCHIVED");
	}

	@Test
	void isValidStatus() {
		final var contribution = ErrandTypeContribution.builder("t")
			.allowedStatuses("DRAFT", "OPEN")
			.build();

		assertThat(contribution.isValidStatus("DRAFT")).isTrue();
		assertThat(contribution.isValidStatus("OPEN")).isTrue();
		assertThat(contribution.isValidStatus("UNKNOWN")).isFalse();
	}

	@Test
	void isValidTransition() {
		final var contribution = ErrandTypeContribution.builder("t")
			.allowedTransition("DRAFT", "OPEN")
			.allowedTransition("OPEN", "CLOSED", "ARCHIVED")
			.build();

		assertThat(contribution.isValidTransition("DRAFT", "OPEN")).isTrue();
		assertThat(contribution.isValidTransition("OPEN", "CLOSED")).isTrue();
		assertThat(contribution.isValidTransition("OPEN", "ARCHIVED")).isTrue();
		assertThat(contribution.isValidTransition("DRAFT", "CLOSED")).isFalse();
		assertThat(contribution.isValidTransition("UNKNOWN", "ANY")).isFalse();
	}

	@Test
	void multipleTransitionsFromSameStatusAccumulate() {
		final var contribution = ErrandTypeContribution.builder("t")
			.allowedTransition("OPEN", "CLOSED")
			.allowedTransition("OPEN", "ARCHIVED")
			.build();

		assertThat(contribution.isValidTransition("OPEN", "CLOSED")).isTrue();
		assertThat(contribution.isValidTransition("OPEN", "ARCHIVED")).isTrue();
	}

	@Test
	void allowedTransitionAlsoRegistersFromStatusAsValid() {
		final var contribution = ErrandTypeContribution.builder("t")
			.allowedTransition("OPEN", "CLOSED")
			.build();

		assertThat(contribution.isValidStatus("OPEN")).isTrue();
		assertThat(contribution.isValidStatus("CLOSED")).isTrue();
	}
}
