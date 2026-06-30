package se.sundsvall.caremanagement.errandtypes.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionOptionTest {

	@Test
	void builderMethods() {
		final var option = DecisionOption.create()
			.withCode("BIFALL")
			.withDisplayName("Bifall")
			.withCarriesAmount(true);

		assertThat(option.getCode()).isEqualTo("BIFALL");
		assertThat(option.getDisplayName()).isEqualTo("Bifall");
		assertThat(option.isCarriesAmount()).isTrue();
		assertThat(option).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersWork() {
		final var option = DecisionOption.create();
		option.setCode("AVSLAG");
		option.setDisplayName("Avslag");
		option.setCarriesAmount(false);

		assertThat(option.getCode()).isEqualTo("AVSLAG");
		assertThat(option.getDisplayName()).isEqualTo("Avslag");
		assertThat(option.isCarriesAmount()).isFalse();
	}

	@Test
	void equalsHashCodeAndToString() {
		final var a = DecisionOption.create().withCode("BIFALL").withDisplayName("Bifall").withCarriesAmount(true);
		final var b = DecisionOption.create().withCode("BIFALL").withDisplayName("Bifall").withCarriesAmount(true);
		final var c = DecisionOption.create().withCode("AVSLAG").withDisplayName("Avslag").withCarriesAmount(false);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
		assertThat(a).isEqualTo(a);
		assertThat(a).hasToString(b.toString());
		assertThat(a.toString()).contains("BIFALL", "Bifall");
	}
}
