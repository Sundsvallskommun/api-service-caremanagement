package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import tools.jackson.databind.JsonNode;

/**
 * Lenient reads of Lifecare's JSON: a field of the wrong type reads as absent rather than failing, the way Lifecare's
 * own client treats it.
 */
final class LifecareJson {

	private LifecareJson() {}

	/**
	 * A non-empty string.
	 *
	 * @param  node the node
	 * @return      the string, empty when the node is not a string or is an empty one
	 */
	static Optional<String> text(final JsonNode node) {
		return Optional.ofNullable(node)
			.filter(JsonNode::isString)
			.map(JsonNode::stringValue)
			.filter(value -> !value.isEmpty());
	}

	/**
	 * A whole number.
	 *
	 * @param  node the node
	 * @return      the number, empty when the node is not a number
	 */
	static Optional<Integer> integer(final JsonNode node) {
		return Optional.ofNullable(node)
			.filter(JsonNode::isNumber)
			.map(JsonNode::asInt);
	}

	/**
	 * A number.
	 *
	 * @param  node the node
	 * @return      the number, empty when the node is not a number
	 */
	static Optional<BigDecimal> decimal(final JsonNode node) {
		return Optional.ofNullable(node)
			.filter(JsonNode::isNumber)
			.map(JsonNode::asDecimal);
	}

	/**
	 * Whether a flag is set.
	 *
	 * @param  node the node
	 * @return      true only for a boolean true
	 */
	static boolean isTrue(final JsonNode node) {
		return node != null && node.isBoolean() && node.booleanValue();
	}

	/**
	 * The elements of an array.
	 *
	 * @param  node the node
	 * @return      the elements, none when the node is not an array
	 */
	static List<JsonNode> elements(final JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		return StreamSupport.stream(node.spliterator(), false).toList();
	}
}
