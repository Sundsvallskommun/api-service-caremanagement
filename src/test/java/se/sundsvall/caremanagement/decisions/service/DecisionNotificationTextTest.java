package se.sundsvall.caremanagement.decisions.service;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionNotificationTextTest {

	private static Stream<Arguments> describeArguments() {
		return Stream.of(
			Arguments.of("PAYMENT", "BIFALL", "Utbetalningsbeslut: bifall"),
			Arguments.of("PAYMENT", "DELAVSLAG", "Utbetalningsbeslut: delvis bifall"),
			Arguments.of("PAYMENT", "AVSLAG", "Utbetalningsbeslut: avslag"),
			Arguments.of("PAYMENT", "APPROVED", "Utbetalningsbeslut: beviljat"),
			Arguments.of("PAYMENT", "REJECTED", "Utbetalningsbeslut: avslag"),
			Arguments.of("RECOMMENDATION", "OK", "Rekommendation: inga anmärkningar"),
			Arguments.of("RECOMMENDATION", "REVIEW_REQUIRED", "Rekommendation: kräver granskning"),
			Arguments.of("ACTUALISATION", "122", "Aktualisering: 122"),
			Arguments.of("UNKNOWN_TYPE", "SOME_VALUE", "UNKNOWN_TYPE: SOME_VALUE"),
			Arguments.of("PAYMENT", null, "Utbetalningsbeslut"),
			Arguments.of("PAYMENT", " ", "Utbetalningsbeslut"));
	}

	@ParameterizedTest
	@MethodSource("describeArguments")
	void describe(final String decisionType, final String value, final String expected) {
		assertThat(DecisionNotificationText.describe(decisionType, value)).isEqualTo(expected);
	}
}
