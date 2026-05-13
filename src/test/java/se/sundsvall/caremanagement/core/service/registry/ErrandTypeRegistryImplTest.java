package se.sundsvall.caremanagement.core.service.registry;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrandTypeRegistryImplTest {

	private static ErrandTypeContribution fostercare() {
		return ErrandTypeContribution.builder("fostercare")
			.displayName("Foster Care")
			.allowedStatuses("DRAFT")
			.allowedTransition("DRAFT", "OPEN", "CLOSED")
			.build();
	}

	private static ErrandTypeContribution adoption() {
		return ErrandTypeContribution.builder("adoption")
			.displayName("Adoption")
			.allowedStatuses("DRAFT", "ONGOING")
			.allowedTransition("ONGOING", "CLOSED")
			.build();
	}

	@Test
	void knownSlugsExposesAllRegisteredTypes() {
		final var registry = new ErrandTypeRegistryImpl(List.of(fostercare(), adoption()));
		assertThat(registry.knownSlugs()).containsExactlyInAnyOrder("fostercare", "adoption");
	}

	@Test
	void emptyRegistryReturnsNoSlugs() {
		final var registry = new ErrandTypeRegistryImpl(List.of());
		assertThat(registry.knownSlugs()).isEmpty();
	}

	@Test
	void getReturnsContribution() {
		final var registry = new ErrandTypeRegistryImpl(List.of(fostercare()));
		assertThat(registry.get("fostercare").typeSlug()).isEqualTo("fostercare");
	}

	@Test
	void getUnknownSlugThrows() {
		final var registry = new ErrandTypeRegistryImpl(List.of(fostercare()));
		assertThatThrownBy(() -> registry.get("unknown"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("unknown");
	}

	@Test
	void existsTrueForRegisteredFalseForUnknown() {
		final var registry = new ErrandTypeRegistryImpl(List.of(fostercare()));
		assertThat(registry.exists("fostercare")).isTrue();
		assertThat(registry.exists("missing")).isFalse();
	}

	@Test
	void isValidStatusDelegatesToContribution() {
		final var registry = new ErrandTypeRegistryImpl(List.of(fostercare()));
		assertThat(registry.isValidStatus("fostercare", "DRAFT")).isTrue();
		assertThat(registry.isValidStatus("fostercare", "BOGUS")).isFalse();
		assertThat(registry.isValidStatus("unknown-type", "DRAFT")).isFalse();
	}

	@Test
	void isValidTransitionDelegatesToContribution() {
		final var registry = new ErrandTypeRegistryImpl(List.of(fostercare()));
		assertThat(registry.isValidTransition("fostercare", "DRAFT", "OPEN")).isTrue();
		assertThat(registry.isValidTransition("fostercare", "DRAFT", "ARCHIVED")).isFalse();
		assertThat(registry.isValidTransition("unknown", "X", "Y")).isFalse();
	}
}
