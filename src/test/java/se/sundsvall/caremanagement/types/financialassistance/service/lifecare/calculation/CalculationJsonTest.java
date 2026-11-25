package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationJsonTest {

	@Test
	void numberNodeWritesIntegralValueWithoutDecimals() {
		assertThat(CalculationJson.numberNode(42.0).asText()).isEqualTo("42");
	}

	@Test
	void numberNodeKeepsDecimalsForFractionalValue() {
		assertThat(CalculationJson.numberNode(42.5).asText()).isEqualTo("42.5");
	}

	@Test
	void numberNodeDoesNotNarrowAnOutOfRangeIntegralValue() {
		// Just outside the int range: guarded away from the narrowing cast, written as a double instead of overflowing.
		final var outOfRange = Integer.MAX_VALUE + 1_000.0;

		assertThat(CalculationJson.numberNode(outOfRange).doubleValue()).isEqualTo(outOfRange);
	}
}
