package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.mapper;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;

import static org.assertj.core.api.Assertions.assertThat;

class LifecareNodeValuesTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static JsonNode value(final String json) {
		return JSON.readTree("{\"v\": " + json + "}").path("v");
	}

	@ParameterizedTest
	@MethodSource("integerArguments")
	void integer(final JsonNode node, final Integer expected) {
		assertThat(LifecareNodeValues.integer(node)).isEqualTo(expected);
	}

	private static Stream<Arguments> integerArguments() {
		return Stream.of(
			Arguments.of(null, null),
			Arguments.of(value("7083"), 7083),
			Arguments.of(value("\"2\""), 2),
			Arguments.of(value("\"-3\""), -3),
			Arguments.of(value("\"abc\""), null),
			Arguments.of(value("null"), null),
			Arguments.of(value("99999999999"), null),
			Arguments.of(MissingNode.getInstance(), null));
	}

	@ParameterizedTest
	@MethodSource("truthyArguments")
	void truthy(final JsonNode node, final boolean expected) {
		assertThat(LifecareNodeValues.truthy(node)).isEqualTo(expected);
	}

	private static Stream<Arguments> truthyArguments() {
		return Stream.of(
			Arguments.of(null, false),
			Arguments.of(MissingNode.getInstance(), false),
			Arguments.of(value("null"), false),
			Arguments.of(value("false"), false),
			Arguments.of(value("true"), true),
			Arguments.of(value("\"\""), false),
			Arguments.of(value("\"x\""), true),
			Arguments.of(value("0"), false),
			Arguments.of(value("1"), true),
			Arguments.of(value("{}"), true));
	}

	@Test
	void text() {
		assertThat(LifecareNodeValues.text(value("\"a\""))).isEqualTo("a");
		assertThat(LifecareNodeValues.text(value("1"))).isNull();
		assertThat(LifecareNodeValues.text(null)).isNull();
		assertThat(LifecareNodeValues.textOrEmpty(value("null"))).isEmpty();
	}

	@Test
	void isTrue() {
		assertThat(LifecareNodeValues.isTrue(value("true"))).isTrue();
		assertThat(LifecareNodeValues.isTrue(value("\"true\""))).isFalse();
		assertThat(LifecareNodeValues.isTrue(null)).isFalse();
	}
}
