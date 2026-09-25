package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reading and writing Lifecare's calculation trees the way Lifecare's web app (JavaScript) does: a missing number
 * counts as nothing, a number is compared by value whatever its JSON spelling, and an integral amount is written
 * without decimals.
 */
final class CalculationJson {

	static final JsonNodeFactory NODES = JsonNodeFactory.instance;

	private CalculationJson() {}

	/** The number in the field, 0 when there is none. */
	static double number(final JsonNode node, final String field) {
		final var value = node.path(field);
		if (value.isNumber()) {
			return value.doubleValue();
		}
		return 0;
	}

	/** The number in the field, as an int, 0 when there is none. */
	static int integer(final JsonNode node, final String field) {
		return (int) number(node, field);
	}

	/** The number in the field when it is one. */
	static Optional<BigDecimal> decimal(final JsonNode node, final String field) {
		final var value = node.path(field);
		if (value.isNumber()) {
			return Optional.of(value.decimalValue());
		}
		return Optional.empty();
	}

	/** The number in the field as an Integer, null when it is not a number. */
	static Integer integerOrNull(final JsonNode node, final String field) {
		final var value = node.path(field);
		if (value.isNumber()) {
			return value.intValue();
		}
		return null;
	}

	/** The string in the field, null when it is not a string. */
	static String text(final JsonNode node, final String field) {
		final var value = node.path(field);
		if (value.isString()) {
			return value.stringValue();
		}
		return null;
	}

	/** The string in the field, empty when there is none. */
	static String textOrEmpty(final JsonNode node, final String field) {
		return Optional.ofNullable(text(node, field)).orElse("");
	}

	/** Whether the field is JSON true (the JavaScript {@code === true}). */
	static boolean isTrue(final JsonNode node, final String field) {
		final var value = node.path(field);
		return value.isBoolean() && value.booleanValue();
	}

	/** Whether the field is truthy as JavaScript reads it. */
	static boolean truthy(final JsonNode node, final String field) {
		final var value = node.path(field);
		if (value.isBoolean()) {
			return value.booleanValue();
		}
		if (value.isNumber()) {
			return value.doubleValue() != 0;
		}
		if (value.isString()) {
			return !value.stringValue().isEmpty();
		}
		return value.isContainer();
	}

	/** Whether the field holds a number other than 0 or no number at all (the JavaScript {@code !== 0}). */
	static boolean notZero(final JsonNode node, final String field) {
		final var value = node.path(field);
		return !value.isNumber() || value.doubleValue() != 0;
	}

	/** The objects of the array in the field, empty when there is none. */
	static List<ObjectNode> objects(final JsonNode node, final String field) {
		return elements(node.path(field));
	}

	/** The objects of the array, empty when it is not one. */
	static List<ObjectNode> elements(final JsonNode value) {
		final var result = new ArrayList<ObjectNode>();
		if (value.isArray()) {
			value.values().forEach(element -> {
				if (element instanceof final ObjectNode object) {
					result.add(object);
				}
			});
		}
		return result;
	}

	/** The first object of the array in the field that matches. */
	static Optional<ObjectNode> find(final JsonNode node, final String field, final Predicate<ObjectNode> match) {
		return objects(node, field).stream().filter(match).findFirst();
	}

	/** The objects as a JSON array. */
	static ArrayNode array(final List<? extends JsonNode> elements) {
		final var array = NODES.arrayNode();
		elements.forEach(array::add);
		return array;
	}

	/** Whether two values are the same as JavaScript's {@code ===} sees them: numbers by value, the rest by content. */
	static boolean same(final JsonNode first, final JsonNode second) {
		if (first.isNumber() && second.isNumber()) {
			return first.doubleValue() == second.doubleValue();
		}
		return first.equals(second);
	}

	/** Whether the field holds the number. */
	static boolean hasNumber(final JsonNode node, final String field, final double number) {
		final var value = node.path(field);
		return value.isNumber() && value.doubleValue() == number;
	}

	/** A number the way JavaScript writes it: without decimals when it is integral. */
	static JsonNode numberNode(final double value) {
		if (value == Math.rint(value) && Math.abs(value) < Integer.MAX_VALUE) {
			return NODES.numberNode((int) value);
		}
		return NODES.numberNode(value);
	}

	/** A number from an API amount, the way JavaScript writes it. */
	static JsonNode numberNode(final BigDecimal value) {
		return numberNode(value.doubleValue());
	}

	/** An id the way JavaScript's {@code String(number)} writes it. */
	static String idOf(final JsonNode value) {
		if (value.isNumber()) {
			final var number = value.doubleValue();
			if (number == Math.rint(number)) {
				return String.valueOf((long) number);
			}
			return String.valueOf(number);
		}
		if (value.isString()) {
			return value.stringValue();
		}
		return "undefined";
	}

	/** The value, or JSON null when it is missing (the JavaScript {@code ?? null}). */
	static JsonNode orNull(final JsonNode value) {
		if (value.isMissingNode()) {
			return NullNode.getInstance();
		}
		return value;
	}

	/** Sets the field to the value, or removes it when the value is missing (JSON leaves an undefined out). */
	static void setOrRemove(final ObjectNode node, final String field, final JsonNode value) {
		if (value.isMissingNode()) {
			node.remove(field);
			return;
		}
		node.set(field, value.deepCopy());
	}

	/** A string field that JSON's empty string or null leaves unset. */
	static String dateOrNull(final JsonNode node, final String field) {
		final var value = text(node, field);
		if (value == null || value.isEmpty()) {
			return null;
		}
		return value;
	}
}
